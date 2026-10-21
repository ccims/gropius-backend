package gropius.sync.ipraktikum

import kotlin.test.*
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test

class EnumFieldsTest {
    private val mapping =
        EnumFieldMapping(
            "effort/",
            mapOf("S" to "effort/S", "L" to "effort/L"),
            mapOf("S" to 2, "L" to 5),
        )
    private val policy =
        TemplatePolicy("bug", "open", "closed", "open", enumFields = mapOf("estimate" to mapping))
    private val profile = SyncProfile(templates = mapOf("template" to policy))
    private val catalog =
        Catalog(
            entries =
                listOf(
                    CatalogEntry(
                        "t",
                        "template",
                        "Issues",
                        "template",
                        schema = mapOf("estimate" to """{"enum":["S","L"],"nullable":true}"""),
                    )
                )
        )

    private fun config(value: EnumFieldMapping) =
        profile.copy(
            templates = mapOf("template" to policy.copy(enumFields = mapOf("estimate" to value)))
        )

    private val fields = EnumFields(profile, catalog)
    private val base =
        IssueValue(
            "Title",
            "Body",
            "template",
            "bug",
            "open",
            fields = mapOf("estimate" to JsonPrimitive("S")),
        )
    private val record =
        IssueRecord(
            "sync",
            "binding",
            baseline = base,
            lastLabels = setOf("effort/S"),
            lastWeight = 2,
            configHash = "config",
        )

    private fun remote(labels: List<String> = listOf("effort/S"), weight: Int? = 2) =
        GitLabIssue(
            1,
            1,
            "Title",
            state = "opened",
            labels = labels,
            weight = weight,
            author = GitLabUser(1, "bot", "Bot"),
        )

    private fun incoming(remote: GitLabIssue, exact: JsonElement = JsonPrimitive("S")) =
        fields
            .incoming("template", mapOf("estimate" to exact), remote, record, "config")["estimate"]

    @Test
    fun `custom field names values and labels are configuration only`() {
        assertEquals(setOf("effort/S"), fields.labels(base))
        assertEquals(2, fields.weight(base))
        val other =
            EnumFields(
                profile.copy(
                    templates =
                        mapOf(
                            "template" to
                                policy.copy(
                                    enumFields =
                                        mapOf(
                                            "complexity" to
                                                EnumFieldMapping(
                                                    "complexity/",
                                                    mapOf(
                                                        "Simple" to "complexity/easy",
                                                        "Hard" to "complexity/hard",
                                                    ),
                                                )
                                        )
                                )
                        )
                ),
                catalog.copy(
                    entries =
                        listOf(
                            catalog.entries
                                .single()
                                .copy(
                                    schema =
                                        mapOf(
                                            "complexity" to
                                                """{"enum":["Simple","Hard"],"nullable":true}"""
                                        )
                                )
                        )
                ),
            )
        assertEquals(
            setOf("complexity/easy"),
            other.labels(base.copy(fields = mapOf("complexity" to JsonPrimitive("Simple")))),
        )
        assertFalse(other.hasWeight("template"))
    }

    @Test
    fun `metadata label and native weight edits each round trip`() {
        assertEquals(JsonPrimitive("L"), incoming(remote(), JsonPrimitive("L")))
        assertEquals(JsonPrimitive("L"), incoming(remote(labels = listOf("effort/L"))))
        assertEquals(JsonPrimitive("L"), incoming(remote(weight = 5)))
        assertEquals(JsonNull, incoming(remote(labels = emptyList())))
        assertEquals(JsonNull, incoming(remote(weight = null)))
        assertEquals(JsonNull, incoming(remote(), JsonNull))
        assertNull(fields.weight(base.copy(fields = mapOf("estimate" to JsonNull))))
        assertTrue(fields.labels(base.copy(fields = mapOf("estimate" to JsonNull))).isEmpty())
    }

    @Test
    fun `invalid and conflicting remote projections never guess`() {
        assertFails { incoming(remote(labels = listOf("effort/S", "effort/L"))) }
        assertFails { incoming(remote(labels = listOf("effort/unknown"))) }
        assertFails { incoming(remote(weight = 4)) }
        assertFails { incoming(remote(weight = 5), JsonNull) }
        assertEquals(JsonPrimitive("L"), incoming(remote(labels = listOf("effort/L"), weight = 5)))
    }

    @Test
    fun `missing renamed nonenum and incompatible schemas stop configuration`() {
        assertFails {
            EnumFields(
                profile,
                catalog.copy(entries = listOf(catalog.entries.single().copy(schema = emptyMap()))),
            )
        }
        assertFails {
            EnumFields(
                profile,
                catalog.copy(
                    entries =
                        listOf(
                            catalog.entries
                                .single()
                                .copy(schema = mapOf("estimate" to """{"type":"string"}"""))
                        )
                ),
            )
        }
        assertFails { EnumFields(config(mapping.copy(labels = mapOf("S" to "effort/S"))), catalog) }
        assertFails {
            EnumFields(config(mapping.copy(weights = mapOf("S" to 2, "L" to 2))), catalog)
        }
        assertFails { fields.values("template", mapOf("estimate" to JsonPrimitive("unknown"))) }
    }

    @Test
    fun `no configuration leaves fields and weight unmanaged`() {
        val unmanaged = EnumFields(SyncProfile(), Catalog())
        assertEquals(
            emptyMap(),
            unmanaged.incoming(
                "anything",
                mapOf("unrelated" to JsonPrimitive("keep")),
                remote(weight = 99),
                record,
                "config",
            ),
        )
        assertFalse(unmanaged.hasWeight("anything"))
        assertFalse(unmanaged.ownsLabel("effort/S"))
    }

    @Test
    fun `new native values import without metadata and existing values reproject on configuration change`() {
        assertEquals(
            JsonPrimitive("L"),
            fields
                .incoming(
                    "template",
                    emptyMap(),
                    remote(listOf("effort/L"), 5),
                    IssueRecord("new", "binding"),
                    "config",
                )["estimate"],
        )
        assertEquals(
            JsonPrimitive("S"),
            fields
                .incoming(
                    "template",
                    base.fields,
                    remote(listOf("effort/L"), 5),
                    record,
                    "new-config",
                )["estimate"],
        )
    }

    @Test
    fun `only one field can project to weight and label namespaces cannot collide`() {
        assertFails {
            EnumFields(profile.copy(templates = profile.templates + ("other" to policy)), catalog)
        }
        val noWeight = mapping.copy(weights = emptyMap())
        assertFails {
            EnumFields(
                profile.copy(
                    templates =
                        mapOf(
                            "template" to
                                policy.copy(
                                    enumFields = mapOf("estimate" to noWeight, "other" to noWeight)
                                )
                        )
                ),
                catalog.copy(
                    entries =
                        listOf(
                            catalog.entries
                                .single()
                                .copy(
                                    schema =
                                        catalog.entries.single().schema +
                                            ("other" to
                                                catalog.entries
                                                    .single()
                                                    .schema
                                                    .getValue("estimate"))
                                )
                        )
                ),
            )
        }
    }

    @Test
    fun `mapped fields participate in independent three way merging and conflict detection`() {
        val local = base.copy(fields = mapOf("estimate" to JsonPrimitive("L")))
        assertEquals(
            JsonPrimitive("L"),
            Merge.reconcile(base, local, base.copy(title = "Edited title")).fields["estimate"],
        )
        assertFailsWith<SyncConflict> {
            Merge.reconcile(base, local, base.copy(fields = mapOf("estimate" to JsonNull)))
        }
    }
}
