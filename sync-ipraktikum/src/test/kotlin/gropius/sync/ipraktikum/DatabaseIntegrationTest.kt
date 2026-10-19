package gropius.sync.ipraktikum

import com.sun.net.httpserver.HttpServer
import gropius.Application
import gropius.model.architecture.*
import gropius.model.template.*
import gropius.model.template.style.ShapeType
import gropius.model.user.GropiusUser
import gropius.model.user.IMSUser
import java.net.InetSocketAddress
import java.util.UUID
import kotlin.test.*
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.neo4j.core.ReactiveNeo4jOperations
import org.springframework.data.neo4j.core.findById
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

@EnabledIfEnvironmentVariable(named = "IPRAKTIKUM_INTEGRATION", matches = "true")
@SpringBootTest(
    classes = [Application::class],
    properties =
        [
            "spring.neo4j.uri=bolt://localhost:17687",
            "spring.mongodb.uri=mongodb://localhost:17017/ipraktikum_test",
            "gropius.sync.api-secret=test",
            "gropius.ipraktikum.initial-delay=36000000",
        ],
)
class DatabaseIntegrationTest {
    @Autowired @Qualifier("graphglueNeo4jOperations") lateinit var neo: ReactiveNeo4jOperations
    @Autowired lateinit var registry: Registry
    @Autowired lateinit var configuration: Configuration
    @Autowired lateinit var domain: DomainBridge
    @Autowired lateinit var coordinator: Coordinator

    companion object {
        private val fake = FakeGitLab()

        @JvmStatic
        @DynamicPropertySource
        fun properties(properties: DynamicPropertyRegistry) {
            properties.add("gropius.sync.login-service-base") { fake.url }
        }
    }

    @Test
    fun `repository round trip uses real domain transactions and creates no duplicate issue`() =
        runBlocking {
            registry.initialize()
            val imsTemplate = configuration.bootstrap()
            var project = neo.save(Project("Cohort-${UUID.randomUUID()}", "", null)).awaitSingle()
            val componentTemplate =
                ComponentTemplate(
                    "Component",
                    "",
                    mutableMapOf(),
                    false,
                    false,
                    null,
                    ShapeType.RECT,
                )
            componentTemplate.componentVersionTemplate().value =
                ComponentVersionTemplate("Version", "", mutableMapOf())
            val savedTemplate = neo.save(componentTemplate).awaitSingle()
            val component = Component("Booking", "", null, mutableMapOf())
            component.template().value = savedTemplate
            val version = ComponentVersion("1", emptyList(), mutableMapOf())
            version.component().value = component
            version.template().value = savedTemplate.componentVersionTemplate().value
            project.components() += version
            project = neo.save(project).awaitSingle()
            val botOwner =
                neo.save(GropiusUser("Bot", null, null, "test-${UUID.randomUUID()}", false))
                    .awaitSingle()
            val ims =
                IMS("GitLab", "", mutableMapOf("gitlab-url" to syncJson.encodeToString(fake.url)))
            ims.template().value = imsTemplate
            ims.syncOthersAllowedBy() += botOwner
            val savedIms = neo.save(ims).awaitSingle()
            val bot = IMSUser("Bot", null, null, "bot", mutableMapOf("gitlab-user-id" to "1"))
            bot.ims().value = neo.findById<IMS>(savedIms.rawId!!)!!
            bot.template().value =
                neo.findById<IMSTemplate>(imsTemplate.rawId!!)!!.imsUserTemplate().value
            bot.gropiusUser().value = neo.findById<GropiusUser>(botOwner.rawId!!)!!
            val savedBot = neo.save(bot).awaitSingle()
            val refreshedIms = neo.findById<IMS>(savedIms.rawId!!)!!
            // Reusing the binding's IMS across imports must not save an already-saved user.
            refreshedIms.users().toList()
            val renamedBot = GitLabUser(1, "bot", "Renamed Bot")
            domain.user(renamedBot, refreshedIms)
            assertEquals("Renamed Bot", domain.user(renamedBot, refreshedIms).displayName)
            refreshedIms.templatedFields["read-user"] = syncJson.encodeToString(savedBot.rawId!!)
            neo.save(refreshedIms).awaitSingle()
            val template = IssueTemplate("Cohort issue", "", mutableMapOf(), false, false)
            template.issueTypes() += IssueType("Bug", "", "")
            template.issueStates() += IssueState("Ready", "", true)
            template.issueStates() += IssueState("Testing", "", true)
            template.issueStates() += IssueState("Done", "", false)
            template.assignmentTypes() += AssignmentType("Reviewer", "")
            template.relationTypes() += IssueRelationType("Depends on", "", "Required by")
            template.relationTypes() += IssueRelationType("Related", "", "Related")
            val savedIssueTemplate = neo.save(template).awaitSingle()
            val binding = IMSProject("Monorepo", "", mutableMapOf("gitlab-project-id" to "42"))
            binding.ims().value = neo.findById<IMS>(savedIms.rawId!!)!!
            binding.template().value =
                neo.findById<IMSTemplate>(imsTemplate.rawId!!)!!.imsProjectTemplate().value
            binding.trackable().value = neo.findById<Project>(project.rawId!!)!!
            val initialBinding = neo.save(binding).awaitSingle()
            var savedBinding = neo.findById<IMSProject>(initialBinding.rawId!!)!!
            val (catalog, components) = configuration.discover(savedBinding, SyncProfile())
            assertEquals(1, components.size)
            val templateKey =
                catalog.entries
                    .single { it.kind == "template" && it.id == savedIssueTemplate.rawId }
                    .key
            val type =
                catalog.entries.single { it.kind == "type" && it.template == templateKey }.key
            fun state(name: String) =
                catalog.entries
                    .single { it.kind == "state" && it.template == templateKey && it.name == name }
                    .key
            val role =
                catalog.entries.single { it.kind == "role" && it.template == templateKey }.key
            val relationType =
                catalog.entries
                    .single {
                        it.kind == "relation" &&
                            it.template == templateKey &&
                            it.name.startsWith("Depends on")
                    }
                    .key
            val policy =
                TemplatePolicy(
                    type,
                    state("Ready"),
                    state("Done"),
                    state("Ready"),
                    nativeAssignmentEnabled = true,
                    nativeAssignmentRole = role,
                )
            val profile =
                SyncProfile(
                    enabled = true,
                    dryRun = false,
                    defaultTemplate = templateKey,
                    templates = mapOf(templateKey to policy),
                    componentLabels =
                        mapOf(catalog.entries.single { it.kind == "component" }.key to "booking"),
                )
            savedBinding = neo.findById<IMSProject>(savedBinding.rawId!!)!!
            savedBinding.templatedFields["sync-profile"] =
                syncJson.encodeToString(syncJson.encodeToString(profile))
            savedBinding = neo.save(savedBinding).awaitSingle()
            val remote = fake.create("Incoming issue", "Student description")
            fake.addLabel("Reported", "#123456", "Imported label")
            fake.issues[remote] =
                JsonObject(
                    fake.issues.getValue(remote) +
                        ("labels" to JsonArray(listOf(JsonPrimitive("Reported"))))
                )
            coordinator.cycle(neo.findById<IMSProject>(savedBinding.rawId!!)!!)
            var records = registry.records(savedBinding.rawId!!)
            assertEquals(1, records.size)
            var record = records.single()
            assertNull(record.error, record.error)
            assertNotNull(record.gropiusId)
            assertEquals("Incoming issue", domain.issue(record.gropiusId).title)
            assertEquals("#123456", domain.issue(record.gropiusId).labels().single().color)
            assertEquals(1, fake.issues.size)

            // GitLab title and workflow edits reach Gropius, preserving identity.
            fake.issues[remote] =
                fake.issues.getValue(remote).let {
                    JsonObject(
                        it +
                            mapOf(
                                "title" to JsonPrimitive("Changed by student"),
                                "labels" to
                                    JsonArray(
                                        listOf(
                                            JsonPrimitive("workflow/${state("Testing")}"),
                                            JsonPrimitive("type/$type"),
                                            JsonPrimitive(
                                                "component/${catalog.entries.single { it.kind == "component" }.key}"
                                            ),
                                        )
                                    ),
                            )
                    )
                }
            coordinator.cycle(neo.findById<IMSProject>(savedBinding.rawId!!)!!)
            record = registry.records(savedBinding.rawId!!).single()
            assertNull(record.error, record.error)
            assertEquals("Testing", domain.issue(record.gropiusId!!).state().value.name)
            assertEquals("Changed by student", domain.issue(record.gropiusId).title)
            assertEquals(2, domain.issue(record.gropiusId).trackables().size)
            assertTrue(
                "component/booking" in
                    fake.issues.getValue(remote).getValue("labels").jsonArray.map {
                        it.jsonPrimitive.content
                    }
            )

            // An unchanged cycle is an identity/semantic no-op, including all HTTP writes.
            val before = fake.writes
            coordinator.cycle(neo.findById<IMSProject>(savedBinding.rawId!!)!!)
            assertNull(registry.records(savedBinding.rawId!!).single().error)
            assertEquals(before, fake.writes)
            assertEquals(1, fake.issues.size)

            // Create a Gropius-origin issue with parallel roles and a directed typed relation.
            val next = IssueRecord(UUID.randomUUID().toString(), savedBinding.rawId!!)
            registry.save(next)
            val value =
                record.baseline!!.copy(
                    title = "Gropius-origin issue",
                    primary = "auto",
                    assignments =
                        listOf(
                            AssignmentValue("reviewer", "gitlab:1", role),
                            AssignmentValue("other", "gitlab:1", null),
                        ),
                    relations = listOf(RelationValue("dependency", record.syncId, relationType)),
                )
            val created =
                domain.apply(
                    domain.binding(savedBinding.rawId!!),
                    next,
                    null,
                    value,
                    catalog,
                    registry.records(savedBinding.rawId!!),
                    policy,
                )
            coordinator.cycle(domain.binding(savedBinding.rawId!!))
            val exported =
                registry.records(savedBinding.rawId!!).single { it.syncId == next.syncId }
            assertNull(exported.error, exported.error)
            assertEquals(2, fake.issues.size)
            assertEquals(2, domain.issue(created.gropiusId!!).assignments().size)
            assertEquals(1, domain.issue(created.gropiusId).outgoingRelations().size)
            verifyEmptyPrimary(savedBinding.rawId!!, exported.iid!!, created.gropiusId)

            // Edit typed semantics through real serialized GitLab description metadata.
            val exportedRemote = fake.issues.getValue(exported.iid)
            val document =
                MetadataCodec.parse(exportedRemote.getValue("description").jsonPrimitive.content)
            val edited =
                document.metadata!!.copy(
                    primary_assignment = null,
                    assignments = listOf(AssignmentValue("reviewer", "gitlab:1", null)),
                    relations = document.metadata.relations.map { it.copy(type = null) },
                )
            fake.issues[exported.iid] =
                JsonObject(
                    exportedRemote +
                        ("description" to
                            JsonPrimitive(
                                MetadataCodec.render(document.body, document.identity!!, edited)
                            ))
                )
            coordinator.cycle(domain.binding(savedBinding.rawId!!))
            val updated = registry.records(savedBinding.rawId!!).single { it.syncId == next.syncId }
            assertNull(updated.error, updated.error)
            val changed = domain.issue(created.gropiusId)
            assertEquals(1, changed.assignments().size)
            assertNull(changed.assignments().single().type().value)
            assertNull(changed.outgoingRelations().single().type().value)
            assertEquals(
                record.gropiusId,
                changed.outgoingRelations().single().relatedIssue().value!!.rawId,
            )
            val unchangedWrites = fake.writes
            coordinator.cycle(domain.binding(savedBinding.rawId!!))
            assertEquals(unchangedWrites, fake.writes)

            verifyComments(savedBinding.rawId!!, created.gropiusId, next.syncId, exported.iid)
            verifyNativeLinks(
                savedBinding.rawId!!,
                created.gropiusId,
                next.syncId,
                record,
                catalog,
                templateKey,
                policy,
                profile,
            )
            verifyCopiedIdentity(savedBinding.rawId!!, exported.iid)
            verifyLabels(
                savedBinding.rawId!!,
                created.gropiusId,
                next.syncId,
                exported.iid,
                catalog,
                policy,
            )
        }

    private suspend fun verifyLabels(
        bindingId: String,
        localId: String,
        syncId: String,
        iid: Long,
        catalog: Catalog,
        policy: TemplatePolicy,
    ) {
        suspend fun localLabels(names: Set<String>) {
            val binding = domain.binding(bindingId)
            val records = registry.records(bindingId)
            val record = records.single { it.syncId == syncId }
            val value = domain.snapshot(domain.issue(localId), record, catalog, binding, records)
            domain.apply(
                binding,
                record,
                value,
                value.copy(labels = names),
                catalog,
                records,
                policy,
                labelDetails = mapOf("UI" to LabelDetails("UI", "#abcdef", "UI defects")),
            )
            coordinator.cycle(domain.binding(bindingId))
            assertNull(registry.record(bindingId, syncId)!!.error)
        }
        fun remoteLabels(names: Set<String>) {
            val issue = fake.issues.getValue(iid)
            val semantic =
                issue
                    .getValue("labels")
                    .jsonArray
                    .map { it.jsonPrimitive.content }
                    .filter { semanticLabel(it) }
            fake.issues[iid] =
                JsonObject(
                    issue + ("labels" to JsonArray((semantic + names).map { JsonPrimitive(it) }))
                )
        }
        localLabels(setOf("UI"))
        assertTrue(
            "UI" in
                fake.issues.getValue(iid).getValue("labels").jsonArray.map {
                    it.jsonPrimitive.content
                }
        )
        assertEquals(
            "#abcdef",
            fake.labelData.getValue("UI").getValue("color").jsonPrimitive.content,
        )
        fake.addLabel("Accessibility", "#112233", "Keyboard support")
        remoteLabels(setOf("UI", "Accessibility"))
        coordinator.cycle(domain.binding(bindingId))
        assertNull(registry.record(bindingId, syncId)!!.error)
        assertEquals(
            setOf("UI", "Accessibility"),
            domain.issue(localId).labels().map { it.name }.toSet(),
        )
        val imported = domain.issue(localId).labels().single { it.name == "Accessibility" }
        assertEquals("#112233", imported.color)
        assertEquals("Keyboard support", imported.description)
        val component =
            configuration.discover(domain.binding(bindingId), SyncProfile()).second.single()
        val owner =
            neo.findById<IMS>(domain.binding(bindingId).ims().value.rawId!!)!!.users().first()
        val now = java.time.OffsetDateTime.now()
        val scoped =
            gropius.model.issue.Label(now, now, "Component-only", "Shared cohort label", "#445566")
        scoped.createdBy().value = owner
        scoped.lastModifiedBy().value = owner
        scoped.trackables() += component
        val scopedId = neo.save(scoped).awaitSingle().rawId!!
        fake.addLabel("Component-only", "#445566", "Shared cohort label")
        remoteLabels(setOf("UI", "Component-only"))
        fake.issues[iid] =
            JsonObject(
                fake.issues.getValue(iid) +
                    ("labels" to
                        JsonArray(
                            fake.issues.getValue(iid).getValue("labels").jsonArray.filter {
                                !it.jsonPrimitive.content.startsWith("component/")
                            }
                        ))
            )
        coordinator.cycle(domain.binding(bindingId))
        assertNull(registry.record(bindingId, syncId)!!.error)
        assertEquals(
            scopedId,
            domain.issue(localId).labels().single { it.name == "Component-only" }.rawId,
        )
        remoteLabels(setOf("UI"))
        coordinator.cycle(domain.binding(bindingId))
        assertEquals(setOf("UI"), domain.issue(localId).labels().map { it.name }.toSet())
        localLabels(emptySet())
        assertTrue(
            fake.issues.getValue(iid).getValue("labels").jsonArray.all {
                semanticLabel(it.jsonPrimitive.content)
            }
        )
        val writes = fake.writes
        coordinator.cycle(domain.binding(bindingId))
        assertEquals(writes, fake.writes)
    }

    private suspend fun verifyEmptyPrimary(bindingId: String, iid: Long, localId: String) {
        val remote = fake.issues.getValue(iid)
        val parsed = MetadataCodec.parse(remote.getValue("description").jsonPrimitive.content)
        fake.issues[iid] =
            JsonObject(
                remote +
                    ("description" to
                        JsonPrimitive(
                            MetadataCodec.render(
                                parsed.body,
                                parsed.identity!!,
                                parsed.metadata!!.copy(primary_assignment = null),
                            )
                        ))
            )
        coordinator.cycle(domain.binding(bindingId))
        assertTrue(fake.issues.getValue(iid).getValue("assignees").jsonArray.isEmpty())
        assertEquals(2, domain.issue(localId).assignments().size)
        val before = fake.writes
        coordinator.cycle(domain.binding(bindingId))
        assertEquals(before, fake.writes)
        assertTrue(fake.issues.getValue(iid).getValue("assignees").jsonArray.isEmpty())
    }

    private suspend fun verifyCopiedIdentity(bindingId: String, sourceIid: Long) {
        val source = fake.issues.getValue(sourceIid)
        val copiedIid =
            fake.create("Copied issue", source.getValue("description").jsonPrimitive.content)
        val before = registry.records(bindingId).size
        coordinator.cycle(domain.binding(bindingId))
        assertEquals(before, registry.records(bindingId).size)
        val binding = domain.binding(bindingId)
        val profile = binding.profile().copy(adoptCopies = setOf(copiedIid))
        binding.templatedFields["sync-profile"] =
            syncJson.encodeToString(syncJson.encodeToString(profile))
        neo.save(binding).awaitSingle()
        coordinator.cycle(domain.binding(bindingId))
        val adopted = registry.records(bindingId).single { it.iid == copiedIid }
        assertNull(adopted.error, adopted.error)
        val original = registry.records(bindingId).single { it.iid == sourceIid }
        assertNotEquals(original.syncId, adopted.syncId)
        assertEquals(
            adopted.syncId,
            MetadataCodec.parse(
                    fake.issues.getValue(copiedIid).getValue("description").jsonPrimitive.content
                )
                .identity!!
                .syncId,
        )
    }

    private suspend fun verifyComments(
        bindingId: String,
        localId: String,
        syncId: String,
        iid: Long,
    ) {
        // Human-authored GitLab notes import/edit; Gropius edits become stable corrections.
        val noteId = fake.addNote(iid, "Student note", 2)
        coordinator.cycle(domain.binding(bindingId))
        val withNote = registry.records(bindingId).single { it.syncId == syncId }
        val comment = withNote.comments.values.single { it.remoteId == noteId }
        assertEquals("Student note", domain.issue(localId).issueComments().single().body)
        fake.editNote(iid, noteId, "Edited in GitLab")
        coordinator.cycle(domain.binding(bindingId))
        assertEquals("Edited in GitLab", domain.issue(localId).issueComments().single().body)
        domain.updateNote(domain.binding(bindingId), comment, "Correction from Gropius")
        coordinator.cycle(domain.binding(bindingId))
        assertEquals(
            "Edited in GitLab",
            fake.notes.getValue(iid).getValue(noteId).getValue("body").jsonPrimitive.content,
        )
        assertEquals(2, fake.notes.getValue(iid).size)
        val beforeCommentReplay = fake.writes
        coordinator.cycle(domain.binding(bindingId))
        assertEquals(beforeCommentReplay, fake.writes)
        assertEquals("Correction from Gropius", domain.issue(localId).issueComments().single().body)
    }

    private suspend fun verifyNativeLinks(
        bindingId: String,
        localId: String,
        syncId: String,
        target: IssueRecord,
        catalog: Catalog,
        templateKey: String,
        policy: TemplatePolicy,
        profile: SyncProfile,
    ) {
        // Native generic links coexist with rich relations and deleting one does not remove the
        // other.
        val generic =
            catalog.entries
                .single {
                    it.kind == "relation" &&
                        it.template == templateKey &&
                        it.name.startsWith("Related")
                }
                .key
        val genericPolicy =
            policy.copy(genericRelationEnabled = true, genericRelationType = generic)
        val genericProfile =
            profile.copy(revision = 2, templates = mapOf(templateKey to genericPolicy))
        val configured = domain.binding(bindingId)
        configured.templatedFields["sync-profile"] =
            syncJson.encodeToString(syncJson.encodeToString(genericProfile))
        neo.save(configured).awaitSingle()
        val latest = registry.records(bindingId).single { it.syncId == syncId }
        val snapshot =
            domain.snapshot(
                domain.issue(localId),
                latest,
                catalog,
                domain.binding(bindingId),
                registry.records(bindingId),
            )
        domain.apply(
            domain.binding(bindingId),
            latest,
            snapshot,
            snapshot.copy(
                relations = snapshot.relations + RelationValue("generic", target.syncId, generic)
            ),
            catalog,
            registry.records(bindingId),
            genericPolicy,
        )
        coordinator.cycle(domain.binding(bindingId))
        assertNull(registry.records(bindingId).single { it.syncId == syncId }.error)
        assertEquals(1, fake.links.size)
        fake.links.clear() // Human removes the native link.
        coordinator.cycle(domain.binding(bindingId))
        val afterUnlink = registry.records(bindingId).single { it.syncId == syncId }
        assertNull(afterUnlink.error, afterUnlink.error)
        assertEquals(1, domain.issue(localId).outgoingRelations().size)
        assertNull(domain.issue(localId).outgoingRelations().single().type().value)
    }
}

/** Stateful HTTP fixture; deliberately implements HTTP rather than mocking the client's methods. */
private class FakeGitLab {
    val issues = linkedMapOf<Long, JsonObject>()
    val notes = linkedMapOf<Long, MutableMap<Long, JsonObject>>()
    val links = linkedMapOf<Long, Pair<Long, Long>>()
    private val labels = linkedMapOf<String, Long>()
    val labelData = linkedMapOf<String, JsonObject>()
    var writes = 0
    private val user = buildJsonObject {
        put("id", 1)
        put("username", "bot")
        put("name", "Bot")
    }
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    val url: String
        get() = "http://127.0.0.1:${server.address.port}"

    init {
        server.createContext("/") { exchange ->
            try {
                val path = exchange.requestURI.path
                val method = exchange.requestMethod
                val body =
                    exchange.requestBody.readBytes().toString(Charsets.UTF_8).let {
                        if (it.isBlank()) buildJsonObject {}
                        else Json.parseToJsonElement(it).jsonObject
                    }
                if (method != "GET") writes++
                val result: JsonElement =
                    when {
                        path.startsWith("/auth/") ->
                            buildJsonObject { put("token", "fixture-token") }
                        path == "/api/v4/user" || path.startsWith("/api/v4/users/") -> user
                        path == "/api/v4/projects/42" -> buildJsonObject { put("id", 42) }
                        path.endsWith("/labels") && method == "POST" -> {
                            val name = body.getValue("name").jsonPrimitive.content
                            addLabel(
                                name,
                                body["color"]?.jsonPrimitive?.content ?: "#6699cc",
                                body["description"]?.jsonPrimitive?.content ?: "",
                            )
                            labelData.getValue(name)
                        }
                        path.endsWith("/labels") -> JsonArray(labelData.values.toList())
                        path.matches(Regex("/api/v4/projects/42/issues/\\d+/notes(/\\d+)?")) -> {
                            val parts = path.split('/')
                            val iid = parts[6].toLong()
                            if (method == "POST") {
                                val id =
                                    addNote(iid, body.getValue("body").jsonPrimitive.content, 1)
                                notes.getValue(iid).getValue(id)
                            } else if (method == "PUT") {
                                val id = parts[8].toLong()
                                editNote(iid, id, body.getValue("body").jsonPrimitive.content)
                                notes.getValue(iid).getValue(id)
                            } else JsonArray(notes[iid]?.values?.toList().orEmpty())
                        }
                        path.matches(Regex("/api/v4/projects/42/issues/\\d+/links(/\\d+)?")) -> {
                            val parts = path.split('/')
                            val iid = parts[6].toLong()
                            when (method) {
                                "POST" -> {
                                    val id = (links.size + 1).toLong()
                                    links[id] =
                                        iid to body.getValue("target_issue_iid").jsonPrimitive.long
                                    buildJsonObject { put("id", id) }
                                }
                                "DELETE" -> {
                                    links.remove(parts[8].toLong())
                                    JsonNull
                                }
                                else ->
                                    JsonArray(
                                        links
                                            .filter { (_, pair) ->
                                                iid == pair.first || iid == pair.second
                                            }
                                            .map { (id, pair) ->
                                                val target =
                                                    if (iid == pair.first) pair.second
                                                    else pair.first
                                                buildJsonObject {
                                                    put("issue_link_id", id)
                                                    put("id", target)
                                                    put("iid", target)
                                                    put("project_id", 42)
                                                    put("link_type", "relates_to")
                                                }
                                            }
                                    )
                            }
                        }
                        path == "/api/v4/projects/42/issues" && method == "GET" ->
                            JsonArray(issues.values.toList())
                        path == "/api/v4/projects/42/issues" && method == "POST" -> {
                            val id =
                                create(
                                    body.getValue("title").jsonPrimitive.content,
                                    body["description"]?.jsonPrimitive?.content ?: "",
                                )
                            issues[id] =
                                JsonObject(
                                    issues.getValue(id) +
                                        ("labels" to
                                            JsonArray(
                                                body["labels"]!!
                                                    .jsonPrimitive
                                                    .content
                                                    .split(',')
                                                    .map { JsonPrimitive(it) }
                                            ))
                                )
                            if (body["assignee_ids"]?.jsonArray?.isNotEmpty() == true)
                                issues[id] =
                                    JsonObject(
                                        issues.getValue(id) +
                                            ("assignees" to JsonArray(listOf(user)))
                                    )
                            issues.getValue(id)
                        }
                        path.matches(Regex("/api/v4/projects/42/issues/\\d+")) -> {
                            val id = path.substringAfterLast('/').toLong()
                            if (method == "PUT") {
                                val current = issues.getValue(id).toMutableMap()
                                for (key in listOf("title", "description")) body[key]?.let {
                                    current[key] = it
                                }
                                var ls =
                                    current
                                        .getValue("labels")
                                        .jsonArray
                                        .map { it.jsonPrimitive.content }
                                        .toSet()
                                body["add_labels"]?.let {
                                    ls += it.jsonPrimitive.content.split(',')
                                }
                                body["remove_labels"]?.let {
                                    ls -= it.jsonPrimitive.content.split(',').toSet()
                                }
                                current["labels"] = JsonArray(ls.map { JsonPrimitive(it) })
                                body["state_event"]?.let {
                                    current["state"] =
                                        JsonPrimitive(
                                            if (it.jsonPrimitive.content == "close") "closed"
                                            else "opened"
                                        )
                                }
                                body["assignee_ids"]?.let {
                                    current["assignees"] =
                                        JsonArray(
                                            if (it.jsonArray.isEmpty()) emptyList()
                                            else listOf(user)
                                        )
                                }
                                issues[id] = JsonObject(current)
                            }
                            issues.getValue(id)
                        }
                        else -> error("Unexpected fixture request $method $path")
                    }
                val bytes = result.toString().toByteArray()
                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            } catch (e: Exception) {
                val bytes = (e.message ?: "fixture failure").toByteArray()
                exchange.sendResponseHeaders(500, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
        }
        server.start()
    }

    fun create(title: String, description: String): Long {
        val id = (issues.size + 1).toLong()
        issues[id] = buildJsonObject {
            put("id", id)
            put("iid", id)
            put("title", title)
            put("description", description)
            put("state", "opened")
            put("labels", JsonArray(emptyList()))
            put("assignees", JsonArray(emptyList()))
            put("author", user)
        }
        return id
    }

    fun addLabel(name: String, color: String, description: String) {
        labels.getOrPut(name) { (labels.size + 1).toLong() }
        labelData[name] = buildJsonObject {
            put("id", labels.getValue(name))
            put("name", name)
            put("color", color)
            put("description", description)
        }
    }

    fun addNote(iid: Long, body: String, author: Int): Long {
        val values = notes.getOrPut(iid) { linkedMapOf() }
        val id = (notes.values.sumOf { it.size } + 1).toLong()
        values[id] = buildJsonObject {
            put("id", id)
            put("body", body)
            put(
                "author",
                buildJsonObject {
                    put("id", author)
                    put("username", "user$author")
                    put("name", "User $author")
                },
            )
        }
        return id
    }

    fun editNote(iid: Long, id: Long, body: String) {
        notes.getValue(iid)[id] =
            JsonObject(notes.getValue(iid).getValue(id) + ("body" to JsonPrimitive(body)))
    }
}
