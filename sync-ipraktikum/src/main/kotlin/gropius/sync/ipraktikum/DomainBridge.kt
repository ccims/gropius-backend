package gropius.sync.ipraktikum

import gropius.dto.input.common.JSONFieldInput
import gropius.model.architecture.*
import gropius.model.issue.Issue
import gropius.model.issue.timeline.*
import gropius.model.template.*
import gropius.model.user.*
import gropius.repository.common.NodeRepository
import gropius.service.NodeBatchUpdateContext
import gropius.service.issue.IssueService
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.serialization.json.*
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.neo4j.core.ReactiveNeo4jOperations
import org.springframework.data.neo4j.core.findAll
import org.springframework.data.neo4j.core.findById
import org.springframework.stereotype.Component
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import java.net.URI
import java.time.OffsetDateTime

/** All imported mutations use the core services and commit with their integration receipt. */
@Component
class DomainBridge(
    @Qualifier("graphglueNeo4jOperations") private val neo: ReactiveNeo4jOperations,
    private val nodes: NodeRepository,
    private val service: IssueService,
    private val transactions: TransactionalOperator,
    private val registry: Registry
) {
    suspend fun issue(id: String): Issue = neo.findById<Issue>(id) ?: error("Gropius issue is no longer accessible")
    suspend fun binding(id: String): IMSProject = neo.findById<IMSProject>(id) ?: error("Binding no longer exists")

    suspend fun discover(binding: IMSProject, components: List<gropius.model.architecture.Component>): List<Issue> {
        val result = binding.trackable().value.issues().toMutableList()
        for (component in components) result += component.issues()
        return result.distinctBy { it.rawId }
    }

    suspend fun userReference(user: User, ims: IMS): String {
        val external = when (user) {
            is IMSUser -> if (user.ims().value.rawId == ims.rawId) user else null
            is GropiusUser -> user.imsUsers().firstOrNull { it.ims().value.rawId == ims.rawId }
            else -> null
        }
        return external?.templatedFields?.get("gitlab-user-id")?.let { "gitlab:${Json.parseToJsonElement(it).jsonPrimitive.long}" }
            ?: "gropius:${user.rawId}"
    }

    suspend fun user(remote: GitLabUser, ims: IMS): IMSUser {
        // A binding is reused throughout a polling cycle. Its loaded user collection may
        // contain instances already saved by an earlier assignment or comment import.
        val freshIms = neo.findById<IMS>(ims.rawId!!) ?: error("IMS no longer exists")
        val existing = freshIms.users().filter { it.templatedFields["gitlab-user-id"] == remote.id.toString() }
        require(existing.size <= 1) { "Duplicate GitLab user identities" }
        existing.singleOrNull()?.let {
            if (it.displayName != remote.name || it.username != remote.username) {
                it.displayName = remote.name; it.username = remote.username
                val saved = neo.save(it).awaitSingle()
                return neo.findById<IMSUser>(saved.rawId!!)!!
            }
            return it
        }
        return transactions.executeAndAwait {
            registry.lock("user:${ims.rawId}:${remote.id}")
            val all = neo.findById<IMS>(ims.rawId!!)!!.users()
            all.firstOrNull { it.templatedFields["gitlab-user-id"] == remote.id.toString() } ?: run {
                val value = IMSUser(remote.name, null, remote.avatar_url?.let { URI(it) }, remote.username,
                    mutableMapOf("gitlab-user-id" to remote.id.toString()))
                value.template().value = freshIms.template().value.imsUserTemplate().value
                value.ims().value = freshIms
                val saved = neo.save(value).awaitSingle()
                neo.findById<IMSUser>(saved.rawId!!)!!
            }
        }
    }

    private suspend fun resolveUser(reference: String, ims: IMS): User = when {
        reference.startsWith("gitlab:") -> {
            val id = reference.removePrefix("gitlab:").toLong()
            neo.findById<IMS>(ims.rawId!!)!!.users().singleOrNull {
                it.templatedFields["gitlab-user-id"] == id.toString()
            } ?: error("Unknown GitLab user $id; add them through the native assignee or user catalog first")
        }
        reference.startsWith("gropius:") -> neo.findById<User>(reference.removePrefix("gropius:"))
            ?: error("Unknown Gropius user")
        else -> error("User reference must be gitlab:<numeric ID> or gropius:<ID>")
    }

    suspend fun displayUser(reference: String, ims: IMS): String {
        val user = resolveUser(reference, ims)
        return if (reference.startsWith("gitlab:") && user.username != null) "@${user.username}"
            else user.displayName.replace(Regex("[\\r\\n\\[\\]<>]"), " ")
    }

    suspend fun snapshot(issue: Issue, record: IssueRecord, catalog: Catalog, binding: IMSProject,
        records: List<IssueRecord>): IssueValue {
        val assignments = issue.assignments().map {
            val id = record.assignmentIds.entries.firstOrNull { e -> e.value == it.rawId }?.key ?: "a-${it.rawId}"
            AssignmentValue(id, userReference(it.user().value, binding.ims().value),
                it.type().value?.let { type -> catalog.key("role", type.rawId!!) })
        }.sortedBy { it.id }
        val relations = issue.outgoingRelations().mapNotNull {
            val target = it.relatedIssue().value ?: return@mapNotNull null
            val identity = records.singleOrNull { r -> r.gropiusId == target.rawId } ?: return@mapNotNull null
            if (identity.detached) return@mapNotNull null
            val id = record.relationIds.entries.firstOrNull { e -> e.value == it.rawId }?.key ?: "r-${it.rawId}"
            RelationValue(id, identity.syncId, it.type().value?.let { type -> catalog.key("relation", type.rawId!!) })
        }.sortedBy { it.id }
        val componentIds = catalog.entries.filter { it.kind == "component" }.associate { it.id to it.key }
        return IssueValue(issue.title, issue.bodyBody, catalog.key("template", issue.template().value.rawId!!),
            catalog.key("type", issue.type().value.rawId!!), catalog.key("state", issue.state().value.rawId!!),
            issue.priority().value?.let { catalog.key("priority", it.rawId!!) },
            issue.trackables().mapNotNull { componentIds[it.rawId] }.toSet(),
            binding.profile().let { p -> if (record.syncId in p.primary) p.primary[record.syncId]
                else if (record.baseline != null) record.baseline.primary else "auto" }, assignments, relations)
    }

    suspend fun apply(
        binding: IMSProject, original: IssueRecord, expected: IssueValue?, target: IssueValue,
        catalog: Catalog, records: List<IssueRecord>, policy: TemplatePolicy,
        fields: Map<String, JsonElement> = emptyMap()
    ): IssueRecord = transactions.executeAndAwait {
        registry.lock("issue:${binding.rawId}:${original.syncId}")
        val record = registry.record(original.binding, original.syncId) ?: original
        if (record.pending != null) return@executeAndAwait record // Receipt/outbox from a committed transaction.
        val actor = binding.ims().value.users().firstOrNull {
            it.rawId == textField(binding.ims().value.templatedFields["read-user"])
        } ?: error("Missing integration principal")
        val template = neo.findById<IssueTemplate>(catalog.entry("template", target.template).id)!!
        val type = neo.findById<IssueType>(catalog.entry("type", target.type, target.template).id)!!
        val state = neo.findById<IssueState>(catalog.entry("state", target.state, target.template).id)!!
        val priority = target.priority?.let { neo.findById<IssuePriority>(catalog.entry("priority", it, target.template).id)!! }
        val context = NodeBatchUpdateContext()
        var at = OffsetDateTime.now()
        fun time(): OffsetDateTime { at = at.plusNanos(1); return at }
        val fieldValues = (policy.requiredFields + fields).map { (name, value) ->
            JSONFieldInput(name, jsonValue(value))
        }
        var issue = record.gropiusId?.let { issue(it) }
        if (issue != null && expected != null) {
            check(snapshot(issue, record, catalog, binding, records) == expected) { "Gropius issue changed during reconciliation" }
        }
        if (issue == null) {
            require(!template.isAbstract && !template.isDeprecated) { "Template cannot create new issues" }
            issue = service.createIssue(listOf(binding.trackable().value), template, target.title, target.body,
                type, state, fieldValues, time(), actor, context)
        } else {
            context.cache.add(issue)
            issue.body().value.issue(context.cache)
            for (assignment in issue.assignments(context.cache)) assignment.issue(context.cache)
            for (relation in issue.outgoingRelations(context.cache)) {
                relation.issue(context.cache)
                relation.relatedIssue(context.cache).value?.let { context.internalUpdatedNodes += it }
            }
            if (issue.template().value.rawId != template.rawId) {
                service.changeIssueTemplate(issue, issue.template().value, template, fieldValues, type, state, priority,
                    emptyMap(), emptyMap(), time(), actor, context)
            }
            if (issue.title != target.title) service.changeIssueTitle(issue, issue.title, target.title, time(), actor, context)
            if (issue.bodyBody != target.body) {
                val body = issue.body().value
                context.cache.add(issue)
                body.issue(context.cache)
                service.updateBody(body, target.body, time(), actor)
            }
            if (issue.type().value.rawId != type.rawId) service.changeIssueType(issue, issue.type().value, type, time(), actor, context)
            if (issue.state().value.rawId != state.rawId) service.changeIssueState(issue, issue.state().value, state, time(), actor, context)
        }
        if (issue.priority().value?.rawId != priority?.rawId)
            service.changeIssuePriority(issue, issue.priority().value, priority, time(), actor, context)
        if (issue.rawId != null) context.cache.add(issue)
        if (issue.trackables().none { it.rawId == binding.trackable().value.rawId })
            service.addIssueToTrackable(issue, binding.trackable().value, time(), actor, context)
        val currentComponents = issue.trackables().toList().filter { t -> catalog.entries.any { it.kind == "component" && it.id == t.rawId } }
        for (key in target.components) {
            val componentId = catalog.entry("component", key).id
            if (issue.trackables().none { it.rawId == componentId }) service.addIssueToTrackable(issue,
                neo.findById<gropius.model.architecture.Component>(componentId)!!, time(), actor, context)
        }
        for (component in currentComponents) if (catalog.key("component", component.rawId!!) !in target.components)
            service.removeIssueFromTrackable(issue, component, time(), actor, context)

        val assignmentNodes = issue.assignments().associateBy {
            record.assignmentIds.entries.firstOrNull { e -> e.value == it.rawId }?.key ?: "a-${it.rawId}"
        }.toMutableMap()
        assignmentNodes.values.forEach { it.issue(context.cache) }
        for ((id, assignment) in assignmentNodes.toMap()) if (target.assignments.none { it.id == id }) {
            service.removeAssignment(assignment, time(), actor); assignmentNodes.remove(id)
        }
        for (value in target.assignments) {
            val user = resolveUser(value.user, binding.ims().value)
            val role = value.role?.let { neo.findById<AssignmentType>(catalog.entry("role", it, target.template).id)!! }
            var node = assignmentNodes[value.id]
            if (node != null && userReference(node.user().value, binding.ims().value) != value.user) {
                service.removeAssignment(node, time(), actor); node = null
            }
            if (node == null) node = service.createAssignment(issue, user, role, time(), actor)
            else if (node.type().value?.rawId != role?.rawId)
                service.changeAssignmentType(node, node.type().value, role, time(), actor)
            assignmentNodes[value.id] = node
        }

        val relationNodes = issue.outgoingRelations().associateBy {
            record.relationIds.entries.firstOrNull { e -> e.value == it.rawId }?.key ?: "r-${it.rawId}"
        }.toMutableMap()
        for (relation in relationNodes.values) {
            relation.issue(context.cache)
            relation.relatedIssue(context.cache)
        }
        val managedTargets = records.filter { !it.detached }.mapNotNull { it.gropiusId }.toSet()
        for ((id, relation) in relationNodes.toMap()) {
            if (relation.relatedIssue().value?.rawId in managedTargets && target.relations.none { it.id == id }) {
                service.removeIssueRelation(relation, time(), actor, context); relationNodes.remove(id)
            }
        }
        for (value in target.relations) {
            val targetRecord = records.singleOrNull { it.syncId == value.target && !it.detached }
                ?: error("Relation target is outside this binding")
            val related = targetRecord.gropiusId?.let { context.cache.getOrAdd(issue(it)) } ?: error("Relation target is not yet imported; retry next cycle")
            val relationType = value.type?.let { neo.findById<IssueRelationType>(catalog.entry("relation", it, target.template).id)!! }
            var node = relationNodes[value.id]
            if (node != null && node.relatedIssue().value?.rawId != related.rawId) {
                service.removeIssueRelation(node, time(), actor, context); node = null
            }
            if (node == null) node = service.createIssueRelation(issue, related, relationType, time(), actor, context)
            else if (node.type().value?.rawId != relationType?.rawId)
                service.changeIssueRelationType(node, node.type().value, relationType, time(), actor)
            context.internalUpdatedNodes += related
            relationNodes[value.id] = node
        }
        nodes.deleteAll(context.deletedNodes).awaitSingleOrNull()
        val persisted = nodes.saveAll(context.updatedNodes + issue).collectList().awaitSingle()
        // A newly created source can coexist with existing target Issues in the aggregation batch.
        val saved = persisted.filterIsInstance<Issue>().single {
            if (issue.rawId != null) it.rawId == issue.rawId else it.createdAt == issue.createdAt && it.title == issue.title
        }
        val refreshed = this.issue(saved.rawId!!)
        val assignmentIds = assignmentNodes.mapValues { (_, node) -> node.rawId ?: refreshed.assignments().single {
            it.createdAt == node.createdAt && it.user().value.rawId == node.user().value.rawId
        }.rawId!! }
        val relationIds = relationNodes.mapValues { (_, node) -> node.rawId ?: refreshed.outgoingRelations().single {
            it.createdAt == node.createdAt && it.relatedIssue().value?.rawId == node.relatedIssue().value?.rawId
        }.rawId!! }
        if (refreshed.imsIssues().none { it.imsProject().value.rawId == binding.rawId }) {
            val imsIssue = IMSIssue(mutableMapOf("sync-id" to syncJson.encodeToString(record.syncId)))
            val currentBinding = this.binding(binding.rawId!!)
            imsIssue.issue().value = refreshed; imsIssue.imsProject().value = currentBinding
            imsIssue.template().value = currentBinding.ims().value.template().value.imsIssueTemplate().value
            neo.save(imsIssue).awaitSingle()
        }
        registry.claim("local:${saved.rawId}", "${record.binding}:${record.syncId}")
        record.copy(gropiusId = saved.rawId, pending = target,
            operation = fingerprint(syncJson.encodeToString(target)), assignmentIds = assignmentIds,
            relationIds = relationIds).also { registry.save(it) }
    }

    suspend fun importNote(binding: IMSProject, record: IssueRecord, note: GitLabNote): CommentRecord = transactions.executeAndAwait {
        val key = "note:${record.binding}:${note.id}"
        registry.lock(key)
        registry.get(key)?.let { return@executeAndAwait syncJson.decodeFromString<CommentRecord>(it) }
        val issue = issue(record.gropiusId!!)
        val author = user(note.author, binding.ims().value)
        val comment = service.createIssueComment(issue, null, note.body, emptyList(), OffsetDateTime.now(), author)
        val saved = neo.save(comment).awaitSingle()
        CommentRecord(saved.rawId!!, note.id, note.body, key).also { registry.put(key, syncJson.encodeToString(it)) }
    }

    suspend fun updateNote(binding: IMSProject, comment: CommentRecord, body: String) = transactions.executeAndAwait {
        val node = neo.findById<IssueComment>(comment.localId) ?: return@executeAndAwait
        val actor = binding.ims().value.users().first { it.rawId == textField(binding.ims().value.templatedFields["read-user"]) }
        service.updateIssueComment(node, body, emptyList(), emptyList(), OffsetDateTime.now(), actor)
        neo.save(node).awaitSingle()
    }

    fun jsonValue(value: JsonElement): Any? = when (value) {
        JsonNull -> null
        is JsonObject -> value.mapValues { jsonValue(it.value) }
        is JsonArray -> value.map { jsonValue(it) }
        is JsonPrimitive -> if (value.isString) value.content else value.booleanOrNull ?: value.longOrNull ?: value.doubleOrNull
    }
}
