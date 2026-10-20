package gropius.sync.ipraktikum

import gropius.model.architecture.IMSProject
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Document("ipraktikum_inbox")
data class InboxSnapshot(@Id val id: String, val fetchedAt: String, val json: String)

/**
 * Reconciles one repository per cohort under a persisted worker lease. Domain changes commit with
 * an outbox record before remote writes; the second pass resolves relations whose endpoints were
 * created in the first pass. Each issue keeps its own error state.
 */
@Component
class Coordinator(
    private val configuration: Configuration,
    private val registry: Registry,
    private val client: GitLabClient,
    private val domain: DomainBridge,
    private val mongo: ReactiveMongoTemplate,
) {
    private val running = AtomicBoolean()
    private val logger = LoggerFactory.getLogger(javaClass)
    private val remoteJson = Json { ignoreUnknownKeys = true }

    @Scheduled(
        fixedDelayString = "\${gropius.ipraktikum.interval:60000}",
        initialDelayString = "\${gropius.ipraktikum.initial-delay:0}",
    )
    fun tick() = runBlocking {
        if (!running.compareAndSet(false, true)) return@runBlocking
        try {
            registry.initialize()
            check(registry.lease("bootstrap")) { "Another worker is bootstrapping" }
            val template =
                try {
                    configuration.bootstrap()
                } finally {
                    registry.release("bootstrap")
                }
            for (ims in template.usedIn()) for (binding in ims.projects()) {
                try {
                    cycle(binding)
                } catch (e: Exception) {
                    logger.warn("iPraktikum binding {}: {}", binding.rawId, e.message)
                    try {
                        configuration.publish(
                            binding,
                            "sync-status",
                            syncJson.encodeToString(
                                SyncStatus(
                                    Instant.now().toString(),
                                    messages = listOf(e.message ?: "Binding failed"),
                                )
                            ),
                        )
                    } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            logger.warn("iPraktikum discovery: {}", e.message)
        } finally {
            running.set(false)
        }
    }

    suspend fun cycle(binding: IMSProject) {
        val id = binding.rawId!!
        if (!registry.lease(id)) return
        val messages = mutableListOf<String>()
        var writes = 0
        try {
            val profile = binding.profile()
            val (catalog, components) = configuration.discover(binding, profile)
            if (!profile.enabled) {
                configuration.publish(
                    binding,
                    "sync-status",
                    syncJson.encodeToString(
                        SyncStatus(
                            Instant.now().toString(),
                            messages =
                                listOf(
                                    "Disabled. Configure creation defaults and enable synchronization."
                                ),
                        )
                    ),
                )
                return
            }
            val session = client.connect(binding, profile.dryRun)
            registry.claim("repository:${session.instance}:${session.project}", id)
            val configHash =
                fingerprint(
                    syncJson.encodeToString(
                        profile.copy(primary = emptyMap(), resolutions = emptyMap())
                    )
                )
            val remoteIssues = session.issues()
            for (remote in remoteIssues) mongo
                .save(
                    InboxSnapshot(
                        "$id:${remote.id}",
                        Instant.now().toString(),
                        Json.encodeToString(remote),
                    )
                )
                .awaitSingle()
            val descriptions = mutableMapOf<Long, ParsedDescription>()
            for (remote in remoteIssues) {
                try {
                    descriptions[remote.id] = MetadataCodec.parse(remote.description ?: "")
                } catch (e: Exception) {
                    messages += "#${remote.iid}: ${e.message}"
                }
            }
            val copies =
                descriptions.entries
                    .filter { it.value.identity != null }
                    .groupBy { it.value.identity!!.syncId }
                    .filterValues { it.size > 1 }
                    .values
                    .flatten()
                    .map { it.key }
                    .toSet()
            var records = registry.records(id)
            val localIssues = domain.discover(binding, components)
            if (!profile.dryRun) {
                for (issue in localIssues) if (records.none { it.gropiusId == issue.rawId }) {
                    val record =
                        IssueRecord(UUID.randomUUID().toString(), id, gropiusId = issue.rawId)
                    registry.claim("local:${issue.rawId}", "$id:${record.syncId}")
                    registry.save(record)
                    records = records + record
                }
                for (remote in
                    remoteIssues.filter { it.issue_type == "issue" && !it.confidential }) {
                    if (records.any { it.remoteId == remote.id }) continue
                    val parsed = descriptions[remote.id] ?: continue
                    val claim = parsed.identity
                    val adopt = remote.iid in profile.adoptCopies
                    if (remote.id in copies && !adopt) {
                        messages += "#${remote.iid}: copied identity; explicitly adopt the copy"
                        continue
                    }
                    if (claim != null && !adopt && claim.binding != id) {
                        messages += "#${remote.iid}: identity belongs to another binding"
                        continue
                    }
                    val existing =
                        if (claim != null && !adopt)
                            records.singleOrNull { it.syncId == claim.syncId }
                        else null
                    if (existing?.remoteId != null) {
                        messages += "#${remote.iid}: duplicate issue identity"
                        continue
                    }
                    val record =
                        (existing ?: IssueRecord(UUID.randomUUID().toString(), id)).copy(
                            remoteId = remote.id,
                            iid = remote.iid,
                        )
                    // Unknown identity claims never attach to arbitrary local IDs.
                    if (claim != null && existing == null && !adopt) {
                        messages +=
                            "#${remote.iid}: unknown identity; explicitly adopt or restore registry"
                        continue
                    }
                    registry.claim(
                        "remote:${session.instance}:${remote.id}",
                        "$id:${record.syncId}",
                    )
                    registry.save(record)
                    records = records.filterNot { it.syncId == record.syncId } + record
                }
            }
            // Dry-run discovers without persisting enrollment or changing either issue tracker.
            if (profile.dryRun) {
                val previews =
                    records +
                        localIssues
                            .filter { issue -> records.none { it.gropiusId == issue.rawId } }
                            .map {
                                IssueRecord(
                                    UUID.nameUUIDFromBytes("dry-run:${it.rawId}".toByteArray())
                                        .toString(),
                                    id,
                                    gropiusId = it.rawId,
                                )
                            }
                messages +=
                    "Dry-run: ${localIssues.size} Gropius issues and ${remoteIssues.size} GitLab issues discovered. No issue mutations."
                for (issue in localIssues) try {
                    val record = previews.single { it.gropiusId == issue.rawId }
                    val local = domain.snapshot(issue, record, catalog, binding, previews)
                    policy(profile, catalog, local.template)
                    if (record.remoteId == null)
                        messages += "Would export: ${issue.title} (${record.syncId})"
                    else {
                        val remote =
                            remoteIssues.singleOrNull { it.id == record.remoteId }
                                ?: error("Counterpart missing or inaccessible")
                        val parsed = descriptions[remote.id] ?: error("Invalid remote metadata")
                        val incoming =
                            incoming(
                                remote,
                                parsed,
                                record,
                                catalog,
                                profile,
                                previews,
                                session,
                                configHash,
                            )
                        val merged =
                            record.baseline?.let { Merge.reconcile(it, local, incoming) }
                                ?: if (local == incoming) local
                                else error("Missing common baseline")
                        if (merged != local || merged != incoming)
                            messages += "Would reconcile: #${remote.iid} / ${issue.title}"
                    }
                } catch (e: Exception) {
                    messages += "Gropius ${issue.rawId}: ${e.message}"
                }
                for (remote in
                    remoteIssues.filter {
                        it.issue_type == "issue" &&
                            !it.confidential &&
                            records.none { r -> r.remoteId == it.id }
                    }) try {
                    val parsed = descriptions[remote.id] ?: error("Invalid metadata")
                    val candidate =
                        IssueRecord(
                            UUID.nameUUIDFromBytes("dry-run:${remote.id}".toByteArray()).toString(),
                            id,
                        )
                    incoming(
                        remote,
                        parsed,
                        candidate,
                        catalog,
                        profile,
                        previews,
                        session,
                        configHash,
                    )
                    messages += "Would import: #${remote.iid} ${remote.title}"
                } catch (e: Exception) {
                    messages += "#${remote.iid} awaiting setup: ${e.message}"
                }
                for ((key, policy) in profile.templates) try {
                    Workflow.validate(policy, catalog, key)
                } catch (e: Exception) {
                    messages += "Template $key: ${e.message}"
                }
            } else {
                ensureLabels(session, catalog, profile, messages)
                // Two passes: create identities/endpoints first, then resolve newly available
                // relation endpoints.
                for (pass in 0..1) {
                    for (original in registry.records(id)) {
                        if (original.detached) continue
                        if (original.syncId in profile.detached) {
                            registry.save(original.copy(detached = true))
                            continue
                        }
                        if (original.remoteId in copies && original.iid !in profile.adoptCopies)
                            continue
                        try {
                            reconcile(
                                domain.binding(id),
                                original,
                                catalog,
                                profile,
                                configHash,
                                session,
                                pass == 0,
                            )
                        } catch (e: Exception) {
                            val current = registry.record(id, original.syncId) ?: original
                            registry.save(current.copy(error = e.message ?: "Issue sync failed"))
                            if (pass == 1)
                                messages +=
                                    "${original.iid?.let { "#$it" } ?: original.syncId}: ${e.message}"
                        }
                    }
                }
                writes = session.writes
            }
            val all = registry.records(id)
            val status =
                SyncStatus(
                    Instant.now().toString(),
                    messages.isEmpty() && all.none { it.error != null || it.warnings.isNotEmpty() },
                    profile.dryRun,
                    all.size,
                    writes,
                    (messages +
                            all.filter { it.error != null }.map { "${it.syncId}: ${it.error}" } +
                            all.flatMap { record ->
                                record.warnings.map { "${record.syncId}: $it" }
                            })
                        .distinct(),
                )
            configuration.publish(binding, "sync-status", syncJson.encodeToString(status))
        } finally {
            registry.release(id)
        }
    }

    private suspend fun ensureLabels(
        session: GitLabClient.Session,
        catalog: Catalog,
        profile: SyncProfile,
        messages: MutableList<String>,
    ) {
        val componentLabels = ComponentLabels(catalog, profile)
        val desired =
            catalog.entries
                .filter { it.kind == "component" }
                .map { componentLabels.label(it.key) }
                .toMutableSet()
        for ((template, policy) in profile.templates) {
            try {
                Workflow.validate(policy, catalog, template)
            } catch (e: Exception) {
                messages += "Template $template awaiting setup: ${e.message}"
                continue
            }
            desired +=
                catalog.entries
                    .filter { it.template == template && it.kind == "state" }
                    .map { "workflow/${Workflow.bucket(it.key, policy)}" }
            desired +=
                catalog.entries
                    .filter { it.template == template && it.kind == "type" }
                    .map { VocabularyLabels(catalog, "type", profile.typeLabels).label(it.key) }
            desired +=
                catalog.entries
                    .filter { it.template == template && it.kind == "priority" }
                    .map { VocabularyLabels(catalog, "priority", profile.priorityLabels).label(it.key) }
        }
        val actual =
            session.labels().associate {
                it.getValue("name").jsonPrimitive.content to it.getValue("id").jsonPrimitive.long
            }
        val stored =
            registry
                .get("labels:${session.binding}")
                ?.let { syncJson.decodeFromString<Map<String, Long>>(it) }
                .orEmpty()
        for (name in desired) {
            if (name in stored && name !in profile.repairLabels)
                require(actual[name] == stored[name]) {
                    "Configured label $name was renamed/deleted; explicitly add its name to repairLabels to recreate/rebind it"
                }
            if (name !in actual) session.label(name)
        }
        val refreshed =
            session.labels().associate {
                it.getValue("name").jsonPrimitive.content to it.getValue("id").jsonPrimitive.long
            }
        registry.put(
            "labels:${session.binding}",
            syncJson.encodeToString(refreshed.filterKeys { it in desired }),
        )
    }

    private fun policy(profile: SyncProfile, catalog: Catalog, template: String): TemplatePolicy {
        val policy =
            profile.templates[template] ?: error("Configure defaults for template $template")
        Workflow.validate(policy, catalog, template)
        return policy
    }

    private suspend fun reconcile(
        binding: IMSProject,
        initial: IssueRecord,
        catalog: Catalog,
        profile: SyncProfile,
        configHash: String,
        session: GitLabClient.Session,
        firstPass: Boolean,
    ) {
        var record = registry.record(initial.binding, initial.syncId)!!
        val records = registry.records(initial.binding)
        var remote = record.iid?.let { session.issue(it) }
        if (remote != null)
            require(!remote.confidential) {
                "Confidential issue excluded from shared cohort visibility"
            }
        if (record.pending != null) {
            try {
                record =
                    export(binding, record, remote, catalog, profile, configHash, session, records)
                remote = record.iid?.let { session.issue(it) }
            } catch (e: RemoteChanged) {
                record =
                    registry
                        .record(record.binding, record.syncId)!!
                        .copy(pending = null, operation = null)
                registry.save(record)
                remote = record.iid?.let { session.issue(it) }
            } catch (e: SyncConflict) {
                record =
                    registry
                        .record(record.binding, record.syncId)!!
                        .copy(pending = null, operation = null)
                registry.save(record)
                // Continue through normal merge so a fingerprint-bound user resolution can apply.
                remote = record.iid?.let { session.issue(it) }
            }
        }
        val local =
            record.gropiusId?.let {
                domain.snapshot(domain.issue(it), record, catalog, binding, records)
            }
        val parsed = remote?.let { MetadataCodec.parse(it.description ?: "") }
        if (record.configHash != null && record.baseline != null && remote != null)
            require(parsed?.identity != null && parsed.metadata != null) {
                "Adapter metadata removed; restore the section before syncing this issue"
            }
        val remoteValue =
            if (remote != null)
                incoming(remote, parsed!!, record, catalog, profile, records, session, configHash)
            else null
        var target =
            when {
                local == null -> remoteValue ?: error("Missing issue endpoints")
                remoteValue == null -> local
                record.baseline == null -> {
                    require(local == remoteValue) {
                        "Both endpoints exist without a common baseline; choose a reconciliation baseline"
                    }
                    local
                }
                else ->
                    try {
                        Merge.reconcile(record.baseline, local, remoteValue)
                    } catch (e: SyncConflict) {
                        val resolution = profile.resolutions[record.syncId]
                        if (resolution?.fingerprint != e.token) throw e
                        when (resolution.side) {
                            "gropius" -> local
                            "gitlab" -> remoteValue
                            else -> throw e
                        }
                    }
            }
        val selectedPolicy = policy(profile, catalog, target.template)
        // Initial endpoint creation must not depend on a relation target that has not yet been
        // created.
        val initialImport = record.gropiusId == null && remote != null && firstPass
        if (initialImport) target = target.copy(relations = emptyList())
        if (local == null || target != local || record.baseline == null) {
            for (assignment in target.assignments.filter { it.user.startsWith("gitlab:") }) {
                val id = assignment.user.removePrefix("gitlab:").toLong()
                val data = session.request("GET", "users/$id")
                domain.user(
                    remoteJson.decodeFromJsonElement(data),
                    binding.ims().value,
                )
            }
            val labelDetails =
                if ((target.labels - local?.labels.orEmpty()).isNotEmpty())
                    session.labels().associate {
                        val name = it.getValue("name").jsonPrimitive.content
                        name to
                            LabelDetails(
                                name,
                                it["color"]?.jsonPrimitive?.content ?: "#6699cc",
                                it["description"]?.jsonPrimitive?.contentOrNull ?: "",
                            )
                    }
                else emptyMap()
            record =
                domain.apply(
                    binding,
                    record,
                    local,
                    target,
                    catalog,
                    records,
                    selectedPolicy,
                    parsed?.metadata?.fields.orEmpty(),
                    labelDetails,
                )
            if (initialImport) {
                // Preserve the original GitLab document until all endpoints exist. The second pass
                // imports its relations.
                registry.save(
                    record.copy(
                        baseline = target,
                        pending = null,
                        operation = null,
                        lastLabels = remote.labels.toSet(),
                        lastAssignee = remote.assignees.singleOrNull()?.id,
                        lastOpen = remote.state == "opened",
                    )
                )
                return
            }
        } else if (
            remoteValue != target || record.configHash != configHash || record.error != null
        ) {
            record =
                record.copy(
                    pending = target,
                    operation = fingerprint(syncJson.encodeToString(target)),
                )
            registry.save(record)
        }
        if (record.pending != null)
            record = export(binding, record, remote, catalog, profile, configHash, session, records)
        if (record.iid != null && record.gropiusId != null) syncComments(binding, record, session)
    }

    private suspend fun incoming(
        remote: GitLabIssue,
        parsed: ParsedDescription,
        record: IssueRecord,
        catalog: Catalog,
        profile: SyncProfile,
        records: List<IssueRecord>,
        session: GitLabClient.Session,
        configHash: String,
    ): IssueValue {
        if (parsed.identity != null)
            require(
                parsed.identity.binding == record.binding &&
                    parsed.identity.syncId == record.syncId ||
                    remote.iid in profile.adoptCopies && record.configHash == null
            ) {
                "Issue identity was changed"
            }
        val metadata = parsed.metadata
        val template =
            metadata?.template
                ?: profile.defaultTemplate
                ?: error("Choose the default incoming issue template")
        val policy = policy(profile, catalog, template)
        val labels = remote.labels.toSet()
        val componentLabels = ComponentLabels(catalog, profile)
        val components =
            labels
                .filter { it.startsWith("component/") }
                .map { componentLabels.component(it) }
                .toSet()
        components.forEach { catalog.entry("component", it) }
        fun label(kind: String, fallback: String?): String? {
            val aliases = if (kind == "type") profile.typeLabels else profile.priorityLabels
            val vocabulary = VocabularyLabels(catalog, kind, aliases)
            val values = labels.filter { it.startsWith("$kind/") }.map { vocabulary.key(it) }
            require(values.size <= 1) { "Choose at most one $kind label" }
            values.forEach { catalog.entry(kind, it, template) }
            return values.singleOrNull() ?: fallback
        }
        val templateChanged = record.baseline != null && template != record.baseline.template
        val typeLabelsChanged =
            labels.filter { it.startsWith("type/") }.toSet() !=
                record.lastLabels.filter { it.startsWith("type/") }.toSet()
        val type =
            if (templateChanged && !typeLabelsChanged) metadata!!.issue_type
            else label("type", metadata?.issue_type ?: policy.defaultType)!!
        if (
            metadata != null &&
                metadata.issue_type != record.baseline?.type &&
                labels.filter { it.startsWith("type/") }.toSet() !=
                    record.lastLabels.filter { it.startsWith("type/") }.toSet()
        )
            require(type == metadata.issue_type) { "Type label and exact type edit conflict" }
        val exactType =
            if (
                metadata != null &&
                    metadata.issue_type != record.baseline?.type &&
                    labels.filter { it.startsWith("type/") }.toSet() ==
                        record.lastLabels.filter { it.startsWith("type/") }.toSet()
            )
                metadata.issue_type
            else type
        val exact =
            metadata?.state
                ?: if (remote.state == "opened") policy.initialState else policy.closedState
        val workflows = labels.filter { it.startsWith("workflow/") }
        val state =
            if (workflows.isEmpty() && record.configHash == null) exact
            else if (record.configHash != null && record.configHash != configHash) exact
            else
                Workflow.incoming(
                    exact,
                    labels,
                    remote.state == "opened",
                    record,
                    policy,
                    catalog,
                    template,
                )
        val assignments = metadata?.assignments.orEmpty().toMutableList()
        var primary = metadata?.primary_assignment ?: if (metadata != null) null else "auto"
        val nativeChanged = record.lastAssignee != remote.assignees.singleOrNull()?.id
        if (policy.nativeAssignmentEnabled && (nativeChanged || record.baseline == null)) {
            val oldPrimary = selectedAssignment(record.baseline, policy, record.lastAssignee)?.id
            val newUser = remote.assignees.singleOrNull()
            val exactChanged =
                metadata != null &&
                    record.baseline != null &&
                    (metadata.assignments != record.baseline.assignments ||
                        metadata.primary_assignment != record.baseline.primary)
            require(!exactChanged) { "Native assignee and structured assignments changed together" }
            if (oldPrimary != null) assignments.removeAll { it.id == oldPrimary }
            if (newUser == null) primary = if (record.baseline == null) primary else null
            else {
                val value =
                    assignments.firstOrNull {
                        it.user == "gitlab:${newUser.id}" && it.role == policy.nativeAssignmentRole
                    }
                        ?: AssignmentValue(
                                "gl-user-${newUser.id}",
                                "gitlab:${newUser.id}",
                                policy.nativeAssignmentRole,
                            )
                            .also { assignments += it }
                primary = value.id
            }
        }
        val relations =
            metadata
                ?.relations
                .orEmpty()
                .map { value ->
                    val target =
                        when {
                            value.target.startsWith("#") ->
                                records.singleOrNull {
                                    it.iid == value.target.removePrefix("#").toLong()
                                }
                            value.target.startsWith("sync:") ->
                                records.singleOrNull {
                                    it.syncId == value.target.removePrefix("sync:")
                                }
                            else -> null
                        } ?: error("Relation target ${value.target} is not enrolled yet")
                    require(!target.detached) { "Relation target is detached" }
                    value.copy(target = target.syncId)
                }
                .toMutableList()
        val links =
            session.links(remote.iid).filter {
                it.project_id == session.project && it.link_type == "relates_to"
            }
        for ((relationId, linkId) in record.nativeLinks) if (
            links.none { it.issue_link_id == linkId }
        ) {
            val current = relations.firstOrNull { it.id == relationId }
            val before = record.baseline?.relations?.firstOrNull { it.id == relationId }
            require(current == null || current == before) {
                "Native relation removed while its metadata changed"
            }
            relations.removeAll { it.id == relationId }
        }
        if (policy.genericRelationEnabled)
            for (link in links) {
                if (records.any { link.issue_link_id in it.nativeLinks.values }) continue
                val target =
                    records.singleOrNull { it.remoteId == link.id && !it.detached } ?: continue
                val otherPolicy = target.baseline?.template?.let { profile.templates[it] }
                if (otherPolicy?.genericRelationEnabled == true && record.syncId > target.syncId)
                    continue
                if (
                    relations.none {
                        it.target == target.syncId && it.type == policy.genericRelationType
                    }
                )
                    relations +=
                        RelationValue(
                            "gl-link-${link.issue_link_id}",
                            target.syncId,
                            policy.genericRelationType,
                        )
            }
        catalog.entry("type", exactType, template)
        val priorityLabelsChanged =
            labels.filter { it.startsWith("priority/") }.toSet() !=
                record.lastLabels.filter { it.startsWith("priority/") }.toSet()
        val exactPriorityChanged =
            metadata != null &&
                record.baseline != null &&
                metadata.priority != record.baseline.priority
        val priority =
            when {
                templateChanged && !priorityLabelsChanged -> metadata?.priority
                priorityLabelsChanged ->
                    label("priority", null).also {
                        if (exactPriorityChanged)
                            require(it == metadata.priority) {
                                "Priority label and metadata edit conflict"
                            }
                    }
                exactPriorityChanged -> metadata.priority
                else -> label("priority", metadata?.priority)
            }
        priority?.let { catalog.entry("priority", it, template) }
        return IssueValue(
            remote.title,
            parsed.body,
            template,
            exactType,
            state,
            priority,
            components,
            primary,
            assignments.sortedBy { it.id },
            relations.sortedBy { it.id },
            remote.labels
                .filter { !semanticLabel(it) }
                .onEach { validateOrdinaryLabel(it) }
                .toSet(),
        )
    }

    private fun selectedAssignment(
        value: IssueValue?,
        policy: TemplatePolicy,
        native: Long?,
    ): AssignmentValue? {
        if (value == null || !policy.nativeAssignmentEnabled || value.primary == null) return null
        val eligible =
            value.assignments.filter {
                it.role == policy.nativeAssignmentRole && it.user.startsWith("gitlab:")
            }
        if (value.primary != "auto") return eligible.singleOrNull { it.id == value.primary }
        return eligible.firstOrNull { it.user == "gitlab:$native" }
            ?: eligible.minByOrNull { it.id }
    }

    private suspend fun export(
        binding: IMSProject,
        original: IssueRecord,
        existing: GitLabIssue?,
        catalog: Catalog,
        profile: SyncProfile,
        configHash: String,
        session: GitLabClient.Session,
        records: List<IssueRecord>,
    ): IssueRecord {
        var record = original
        val target = record.pending ?: return record
        val policy = policy(profile, catalog, target.template)
        val state = catalog.entry("state", target.state, target.template)
        val selected = selectedAssignment(target, policy, record.lastAssignee)
        if (policy.nativeAssignmentEnabled && target.primary != null && target.primary != "auto")
            require(selected != null) {
                "Primary assignment must reference an eligible GitLab user"
            }
        val componentLabels = ComponentLabels(catalog, profile)
        target.labels.forEach { validateOrdinaryLabel(it) }
        if (target.labels.isNotEmpty()) {
            val existingLabels =
                session.labels().map { it.getValue("name").jsonPrimitive.content }.toMutableSet()
            for (name in target.labels.sorted()) if (name !in existingLabels) {
                val details =
                    domain.labelDetails(
                        record.gropiusId ?: error("Missing local label source"),
                        name,
                    )
                session.label(name, details.color, details.description)
                existingLabels += name
            }
        }
        val labels =
            target.components.map { componentLabels.label(it) }.toSet() +
                "workflow/${Workflow.bucket(target.state, policy)}" +
                VocabularyLabels(catalog, "type", profile.typeLabels).label(target.type) +
                target.priority?.let {
                    setOf(VocabularyLabels(catalog, "priority", profile.priorityLabels).label(it))
                }.orEmpty() +
                target.labels
        val metadata =
            EditableMetadata(
                revision = profile.revision,
                template = target.template,
                issue_type = target.type,
                state = target.state,
                priority = target.priority,
                primary_assignment = target.primary,
                assignments = target.assignments,
                relations =
                    target.relations.map { relation ->
                        relation.copy(
                            target =
                                records
                                    .singleOrNull { it.syncId == relation.target }
                                    ?.iid
                                    ?.let { "#$it" } ?: "sync:${relation.target}"
                        )
                    },
            )
        val assignmentSummary =
            target.assignments.map {
                val display = domain.displayUser(it.user, binding.ims().value)
                "$display (${it.role?.let { role -> catalog.entry("role", role, target.template).name } ?: "untyped"})"
            }
        val summary = buildString {
            append("\n**Assignments:** ")
            append(assignmentSummary.joinToString().ifEmpty { "none" })
            append("\n\n**Relations:** ")
            append(
                metadata.relations
                    .joinToString {
                        "${it.type?.let { type -> catalog.entry("relation", type, target.template).name.substringBefore(" / ") } ?: "untyped"} ${it.target}"
                    }
                    .ifEmpty { "none" }
            )
        }
        val description =
            MetadataCodec.render(
                target.body,
                Identity(binding = record.binding, syncId = record.syncId),
                metadata,
                summary,
            )
        var remote = existing
        if (remote == null) {
            // The intent was committed before HTTP. An unknown prior outcome is reconciled by exact
            // marker.
            val matches =
                session.issues().filter {
                    runCatching {
                            MetadataCodec.parse(it.description ?: "").identity?.syncId ==
                                record.syncId
                        }
                        .getOrDefault(false)
                }
            require(matches.size <= 1) { "Duplicate remote sync identity" }
            remote = matches.singleOrNull()
            if (remote == null) {
                val previousAttempt =
                    registry.get("create-attempt:${record.syncId}")?.takeIf { it.isNotBlank() }
                if (
                    previousAttempt != null &&
                        profile.retryCreates[record.syncId] != fingerprint(previousAttempt)
                )
                    error(
                        "Prior create outcome unresolved. Confirm absence, then set retryCreates[${record.syncId}] to ${fingerprint(previousAttempt)}"
                    )
                registry.put("create-attempt:${record.syncId}", Instant.now().toString())
                remote =
                    try {
                        session.create(
                            buildJsonObject {
                                put("title", target.title)
                                put("description", description)
                                put("labels", labels.joinToString(","))
                            }
                        )
                    } catch (e: RemoteFailure) {
                        if (!e.uncertain) registry.put("create-attempt:${record.syncId}", "")
                        throw e
                    }
            }
            registry.claim(
                "remote:${session.instance}:${remote.id}",
                "${record.binding}:${record.syncId}",
            )
            record = record.copy(remoteId = remote.id, iid = remote.iid)
            registry.save(record)
        }
        val fresh = session.issue(remote.iid)
        require(!fresh.confidential) { "Issue became confidential; pending write paused" }
        if (record.baseline != null) {
            val parsed = MetadataCodec.parse(fresh.description ?: "")
            require(
                parsed.identity?.syncId == record.syncId ||
                    (record.configHash == null &&
                        (parsed.identity == null || fresh.iid in profile.adoptCopies))
            ) {
                "Remote identity changed before write"
            }
            val latest =
                incoming(
                    fresh,
                    parsed,
                    record,
                    catalog,
                    profile,
                    registry.records(record.binding),
                    session,
                    configHash,
                )
            if (latest != target) {
                // Merge against the last common state and verify the desired pending write remains
                // valid.
                if (Merge.reconcile(record.baseline, target, latest) != target)
                    throw RemoteChanged()
            }
        }
        val oldLabels = fresh.labels.toSet()
        val payload = buildJsonObject {
            if (fresh.title != target.title) put("title", target.title)
            if ((fresh.description ?: "") != description) put("description", description)
            if ((labels - oldLabels).isNotEmpty())
                put("add_labels", (labels - oldLabels).joinToString(","))
            if ((oldLabels - labels).isNotEmpty())
                put("remove_labels", (oldLabels - labels).joinToString(","))
            if ((fresh.state == "opened") != state.open)
                put("state_event", if (state.open == true) "reopen" else "close")
        }
        if (payload.isNotEmpty()) session.update(fresh.iid, payload)
        val selection = selected?.user ?: "unassigned"
        val blockedKey = "$configHash:$selection"
        if (
            policy.nativeAssignmentEnabled &&
                fresh.assignees.singleOrNull()?.id !=
                    selected?.user?.removePrefix("gitlab:")?.toLong() &&
                record.blockedAssignee != blockedKey
        ) {
            try {
                session.update(
                    fresh.iid,
                    buildJsonObject {
                        put(
                            "assignee_ids",
                            JsonArray(
                                listOfNotNull(
                                    selected?.user?.removePrefix("gitlab:")?.toLong()?.let {
                                        JsonPrimitive(it)
                                    }
                                )
                            ),
                        )
                    },
                )
                record = record.copy(blockedAssignee = null, warnings = emptyList())
            } catch (e: RemoteFailure) {
                if (e.status !in setOf(400, 403, 422)) throw e
                record =
                    record.copy(
                        blockedAssignee = blockedKey,
                        warnings =
                            listOf(
                                "Cannot project $selection to the native assignee. Full assignments are preserved; update permissions or profile revision to retry."
                            ),
                    )
            }
        }
        val native = record.nativeLinks.toMutableMap()
        val links = session.links(fresh.iid)
        val currentRecords = registry.records(record.binding)
        val desiredGeneric =
            if (policy.genericRelationEnabled)
                target.relations
                    .filter { it.type == policy.genericRelationType }
                    .groupBy { it.target }
                    .values
                    .map { group -> group.firstOrNull { it.id in native } ?: group.minBy { it.id } }
                    .filter { relation ->
                        val opposite = currentRecords.singleOrNull { it.syncId == relation.target }
                        val oppositeValue = opposite?.pending ?: opposite?.baseline
                        val oppositePolicy = oppositeValue?.template?.let { profile.templates[it] }
                        val oppositeCandidates =
                            if (oppositePolicy?.genericRelationEnabled == true)
                                oppositeValue.relations.filter {
                                    it.target == record.syncId &&
                                        it.type == oppositePolicy.genericRelationType
                                }
                            else emptyList()
                        val otherOwns =
                            opposite?.nativeLinks?.keys?.any { key ->
                                oppositeValue?.relations?.any {
                                    it.id == key && it.target == record.syncId
                                } == true
                            } == true
                        !otherOwns &&
                            (relation.id in native ||
                                oppositeCandidates.isEmpty() ||
                                record.syncId < relation.target)
                    }
            else emptyList()
        for ((relationId, linkId) in native.toMap()) if (
            desiredGeneric.none { it.id == relationId }
        ) {
            if (links.any { it.issue_link_id == linkId }) session.unlink(fresh.iid, linkId)
            native.remove(relationId)
        }
        for (relation in desiredGeneric) {
            val destination =
                registry.records(record.binding).single { it.syncId == relation.target }
            val targetIid = destination.iid ?: continue
            native[relation.id] =
                links
                    .firstOrNull { it.iid == targetIid && it.project_id == session.project }
                    ?.issue_link_id ?: session.link(fresh.iid, targetIid)
        }
        record = record.copy(nativeLinks = native)
        registry.save(
            record
        ) // Keep link receipts before verifying and acknowledging the final projection.
        val verified = session.issue(fresh.iid)
        check(
            verified.title == target.title &&
                verified.description == description &&
                verified.labels.toSet() == labels &&
                (verified.state == "opened") == state.open
        ) {
            "GitLab changed during export; pending operation retained"
        }
        record =
            record.copy(
                baseline = target,
                pending = null,
                expectedRemote = null,
                operation = null,
                lastLabels = verified.labels.toSet(),
                lastAssignee = verified.assignees.singleOrNull()?.id,
                lastOpen = verified.state == "opened",
                configHash = configHash,
                error = null,
            )
        registry.save(record)
        return record
    }

    private suspend fun syncComments(
        binding: IMSProject,
        original: IssueRecord,
        session: GitLabClient.Session,
    ) {
        var record = registry.record(original.binding, original.syncId)!!
        val comments = record.comments.toMutableMap()
        val notes =
            session.notes(record.iid!!).filter {
                !it.system &&
                    !it.internal &&
                    !(it.author.id == session.bot.id && it.body.contains("<!-- ipraktikum-note:"))
            }
        for (note in notes) if (comments.values.none { it.remoteId == note.id }) {
            val imported = domain.importNote(binding, record, note)
            comments[imported.localId] = imported
        }
        val allNotes = session.notes(record.iid)
        for (comment in domain.issue(record.gropiusId!!).issueComments()) {
            var known = comments[comment.rawId]
            if (known?.deleted == true) continue
            if (known == null) {
                known =
                    CommentRecord(
                        comment.rawId!!,
                        body = comment.body,
                        operation = "note-${UUID.randomUUID()}",
                    )
                comments[known.localId] = known
                registry.save(record.copy(comments = comments.toMap()))
                val marker = "<!-- ipraktikum-note:${known.operation} -->"
                val found = allNotes.singleOrNull { it.body.contains(marker) }
                val author = comment.createdBy().value.displayName
                val result =
                    found
                        ?: session.note(
                            record.iid,
                            "${comment.body}\n\n$marker\n_From Gropius: ${author}_",
                        )
                comments[known.localId] = known.copy(remoteId = result.id)
                continue
            }
            val note = allNotes.firstOrNull { it.id == known.remoteId }
            if (comment.isCommentDeleted || (known.remoteId != null && note == null)) {
                comments[known.localId] = known.copy(deleted = true)
                continue
            }
            if (note == null) {
                val marker = "<!-- ipraktikum-note:${known.operation} -->"
                val found = allNotes.singleOrNull { it.body.contains(marker) }
                val result = found ?: session.note(record.iid, "${comment.body}\n$marker")
                comments[known.localId] = known.copy(remoteId = result.id, body = comment.body)
                continue
            }
            val remoteBody =
                if (note.author.id == session.bot.id && note.body.contains("<!-- ipraktikum-note:"))
                    note.body
                        .substringBefore("<!-- ipraktikum-note:")
                        .removeSuffix("\n\n")
                        .removeSuffix("\n")
                else note.body
            require(
                comment.body == known.body ||
                    remoteBody == known.remoteBody ||
                    comment.body == remoteBody
            ) {
                "Comment ${note.id} has conflicting edits"
            }
            if (comment.body == known.body && remoteBody != known.remoteBody) {
                domain.updateNote(binding, known, remoteBody)
                comments[known.localId] = known.copy(body = remoteBody, remoteBody = remoteBody)
            } else if (comment.body != known.body) {
                val marker = "<!-- ipraktikum-note:${known.operation} -->"
                val correction = note.author.id != session.bot.id
                val result =
                    session.note(
                        record.iid,
                        (if (correction) "Correction to note ${note.id}:\n\n" else "") +
                            "${comment.body}\n$marker",
                        if (correction) known.correctionId else note.id,
                    )
                comments[known.localId] =
                    known.copy(
                        body = comment.body,
                        remoteBody = if (correction) remoteBody else comment.body,
                        correctionId = if (correction) result.id else null,
                    )
            }
        }
        record = registry.record(record.binding, record.syncId)!!
        registry.save(record.copy(comments = comments.toMap()))
    }
}

private class RemoteChanged :
    IllegalStateException("GitLab changed before export; reconcile fresh values")
