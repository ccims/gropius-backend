package gropius.sync.ipraktikum

import kotlin.test.*
import org.junit.jupiter.api.Test

class ProtocolTest {
    @Test
    fun `ordinary label membership merges independent additions and removals`() {
        val b = IssueValue("Title", "Body", "template", "bug", "open", labels = setOf("Old"))
        val merged =
            Merge.reconcile(
                b,
                b.copy(labels = setOf("Old", "UI")),
                b.copy(labels = setOf("Accessibility")),
            )
        assertEquals(setOf("UI", "Accessibility"), merged.labels)
        validateOrdinaryLabel("UI")
        assertFails { validateOrdinaryLabel("workflow/ready") }
        assertFails { validateOrdinaryLabel("a,b") }
    }

    @Test
    fun `component aliases preserve stable keys and reject ambiguous labels`() {
        val catalog =
            Catalog(
                entries =
                    listOf(
                        CatalogEntry("one", "frontend-abc", "Frontend", "component"),
                        CatalogEntry("two", "backend-def", "Backend", "component"),
                    )
            )
        val profile =
            SyncProfile(
                componentLabels = mapOf("frontend-abc" to "frontend", "backend-def" to "backend")
            )
        val labels = ComponentLabels(catalog, profile)
        assertEquals("component/frontend", labels.label("frontend-abc"))
        assertEquals("frontend-abc", labels.component("component/frontend"))
        assertEquals("frontend-abc", labels.component("component/frontend-abc"))
        assertEquals(
            "component/frontend-abc",
            ComponentLabels(catalog, SyncProfile()).label("frontend-abc"),
        )
        assertFails {
            ComponentLabels(
                catalog,
                profile.copy(componentLabels = mapOf("frontend-abc" to "backend-def")),
            )
        }
        assertFails {
            ComponentLabels(
                catalog,
                profile.copy(
                    componentLabels = mapOf("frontend-abc" to "same", "backend-def" to "same")
                ),
            )
        }
        assertFails {
            ComponentLabels(
                catalog,
                profile.copy(componentLabels = mapOf("frontend-abc" to "bad/name")),
            )
        }
        assertFails { labels.component("component/unknown") }
    }

    private val base = IssueValue("Title", "Description\n", "template", "bug", "testing")
    private val metadata =
        EditableMetadata(
            template = "template",
            issue_type = "bug",
            state = "testing",
            assignments = listOf(AssignmentValue("a-1", "gitlab:12", "reviewer")),
            relations = listOf(RelationValue("r-1", "#42", "depends-on")),
        )
    private val identity = Identity(binding = "binding", syncId = "01234567-89ab-cdef")

    @Test
    fun `metadata preserves body whitespace identity and rich types`() {
        val description = MetadataCodec.render(base.body, identity, metadata)
        val parsed = MetadataCodec.parse(description)
        assertEquals(base.body, parsed.body)
        assertEquals(identity, parsed.identity)
        assertEquals(metadata, parsed.metadata)
        assertEquals(
            description,
            MetadataCodec.render(parsed.body, parsed.identity!!, parsed.metadata!!),
        )
    }

    @Test
    fun `accept human YAML and reject duplicate keys aliases and incomplete sections`() {
        val rendered = MetadataCodec.render("text", identity, metadata)
        val start = rendered.indexOf("```yaml\n") + 8
        val end = rendered.indexOf("\n```", start)
        fun yaml(value: String) = rendered.substring(0, start) + value + rendered.substring(end)
        val good =
            "schema: 1\ntemplate: template\nissue_type: bug\nstate: testing\nassignments: []\nrelations: []"
        assertEquals("testing", MetadataCodec.parse(yaml(good)).metadata!!.state)
        assertFails { MetadataCodec.parse(yaml("$good\nstate: open")) }
        assertFails { MetadataCodec.parse(yaml("$good\nx: &a [1]\ny: *a")) }
        assertFails { MetadataCodec.parse(rendered + rendered) }
        assertFails { MetadataCodec.parse(rendered.replace(MetadataCodec.END, "")) }
        assertFails {
            MetadataCodec.parse(
                yaml(good.replace("state: testing", "state: !!java/object java.lang.Runtime"))
            )
        }
    }

    @Test
    fun `merge independent edits without losing role or relation identity`() {
        val result =
            Merge.reconcile(
                base,
                base.copy(title = "new title", components = setOf("api")),
                base.copy(
                    body = "new body",
                    components = setOf("app"),
                    assignments = metadata.assignments,
                ),
            )
        assertEquals("new title", result.title)
        assertEquals("new body", result.body)
        assertEquals(setOf("api", "app"), result.components)
        assertEquals(metadata.assignments, result.assignments)
    }

    @Test
    fun `deletion versus role edit is a conflict rather than silent data loss`() {
        val b = base.copy(assignments = metadata.assignments)
        val error =
            assertFailsWith<SyncConflict> {
                Merge.reconcile(
                    b,
                    b.copy(assignments = emptyList()),
                    b.copy(
                        assignments = listOf(metadata.assignments.single().copy(role = "tester"))
                    ),
                )
            }
        assertEquals(listOf("assignments/a-1"), error.fields)
        assertEquals(64, error.token.length)
    }

    @Test
    fun `same content with distinct IDs remains separate and merge is idempotent`() {
        val values =
            listOf(
                AssignmentValue("one", "gitlab:12", "reviewer"),
                AssignmentValue("two", "gitlab:12", "reviewer"),
            )
        val value = base.copy(assignments = values)
        assertEquals(value, Merge.reconcile(value, value, value))
    }

    private val catalog =
        Catalog(
            entries =
                listOf(
                    CatalogEntry("t", "template", "Template", "template"),
                    CatalogEntry("b", "bug", "Bug", "type", "template"),
                    CatalogEntry("d", "development", "Development", "state", "template", true),
                    CatalogEntry("r", "review", "Review", "state", "template", true),
                    CatalogEntry("tst", "testing", "Testing", "state", "template", true),
                    CatalogEntry("done", "done", "Done", "state", "template", false),
                )
        )
    private val policy =
        TemplatePolicy(
            "bug",
            "development",
            "done",
            "development",
            states =
                mapOf(
                    "development" to "doing",
                    "review" to "review",
                    "testing" to "review",
                    "done" to "done",
                ),
            bucketDefaults =
                mapOf("doing" to "development", "review" to "review", "done" to "done"),
            autoStates = false,
        )
    private val record =
        IssueRecord(
            "id",
            "binding",
            baseline = base,
            lastLabels = setOf("workflow/review"),
            lastOpen = true,
        )

    @Test
    fun `unchanged coarse column preserves fine state and board move chooses default`() {
        Workflow.validate(policy, catalog, "template")
        assertEquals(
            "testing",
            Workflow.incoming(
                "testing",
                setOf("workflow/review"),
                true,
                record,
                policy,
                catalog,
                "template",
            ),
        )
        assertEquals(
            "development",
            Workflow.incoming(
                "testing",
                setOf("workflow/doing"),
                true,
                record,
                policy,
                catalog,
                "template",
            ),
        )
        assertEquals(
            "done",
            Workflow.incoming(
                "testing",
                setOf("workflow/review"),
                false,
                record,
                policy,
                catalog,
                "template",
            ),
        )
    }

    @Test
    fun `native close with conflicting exact state edit requires resolution`() {
        assertFails {
            Workflow.incoming(
                "development",
                setOf("workflow/review"),
                false,
                record,
                policy,
                catalog,
                "template",
            )
        }
        assertFails {
            Workflow.incoming(
                "testing",
                setOf("workflow/review", "workflow/doing"),
                true,
                record,
                policy,
                catalog,
                "template",
            )
        }
        assertFails {
            Workflow.incoming("testing", emptySet(), true, record, policy, catalog, "template")
        }
    }

    @Test
    fun `runtime keys distinguish duplicate display names without fixed vocabulary`() {
        assertNotEquals(
            stableKey("role", "1", "Team member"),
            stableKey("role", "2", "Team member"),
        )
        assertEquals(stableKey("role", "1", "Team member"), stableKey("role", "1", "Team member"))
    }
}
