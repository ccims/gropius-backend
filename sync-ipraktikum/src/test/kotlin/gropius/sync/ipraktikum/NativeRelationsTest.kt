package gropius.sync.ipraktikum

import kotlin.test.*
import org.junit.jupiter.api.Test

class NativeRelationsTest {
    private val dependency = RelationValue("dependency", "b", "runtime-depends-on")
    private val duplicate = RelationValue("duplicate", "b", "runtime-duplicate")

    private fun record(id: String, vararg relations: RelationValue) =
        IssueRecord(
            id,
            "binding",
            remoteId = if (id == "a") 1 else 2,
            iid = if (id == "a") 1 else 2,
            baseline =
                IssueValue(
                    "Title",
                    "",
                    "template-$id",
                    "bug",
                    "ready",
                    relations = relations.toList(),
                ),
        )

    private val link = GitLabLink(50, 2, 2, 42)

    private fun receipt(vararg relations: RelationValue) =
        NativeRelationReceipt(
            50,
            relations.map { NativeRelations.member("a", it) }.toSet(),
        )

    @Test
    fun `parallel and reverse typed relations share one deterministic pair across templates`() {
        val reverse = RelationValue("reverse", "a", "another-runtime-type")
        val desired =
            NativeRelations.desired(
                listOf(record("a", dependency, duplicate), record("b", reverse))
            )
        assertEquals(
            mapOf(
                NativeRelations.PairKey("a", "b") to
                    setOf("a/dependency", "a/duplicate", "b/reverse")
            ),
            desired,
        )
        assertEquals(NativeRelations.pair("b", "a"), NativeRelations.pair("a", "b"))
    }

    @Test
    fun `only the last relation removes a desired pair`() {
        assertEquals(1, NativeRelations.desired(listOf(record("a", dependency), record("b"))).size)
        assertTrue(NativeRelations.desired(listOf(record("a"), record("b"))).isEmpty())
        assertTrue(
            NativeRelations.desired(
                    listOf(record("a", dependency), record("b").copy(detached = true))
                )
                .isEmpty()
        )
    }

    @Test
    fun `new GitLab links import once as untyped relations without relation vocabulary`() {
        val a = record("a")
        val b = record("b")
        assertEquals(
            listOf(RelationValue("gl-link-50", "b")),
            NativeRelations.incoming(a, emptyList(), listOf(a, b), listOf(link)),
        )
        assertTrue(
            NativeRelations.incoming(
                    b,
                    emptyList(),
                    listOf(a, b),
                    listOf(link.copy(id = 1, iid = 1)),
                )
                .isEmpty()
        )
        assertTrue(
            NativeRelations.incoming(
                    a,
                    emptyList(),
                    listOf(a, b),
                    listOf(link.copy(link_type = "blocks")),
                )
                .isEmpty()
        )
    }

    @Test
    fun `existing typed relations are adopted without adding a neutral relation`() {
        val a = record("a", dependency)
        val b = record("b")
        assertEquals(
            listOf(dependency),
            NativeRelations.incoming(a, listOf(dependency), listOf(a, b), listOf(link)),
        )
        val reverse = RelationValue("reverse", "a", "typed")
        assertTrue(
            NativeRelations.incoming(
                    record("a"),
                    emptyList(),
                    listOf(record("a"), record("b", reverse)),
                    listOf(link),
                )
                .isEmpty()
        )
    }

    @Test
    fun `removing a native link removes one unchanged underlying relation`() {
        val a = record("a", dependency).copy(relationLinks = mapOf("b" to receipt(dependency)))
        val b = record("b")
        assertEquals(
            listOf(dependency),
            NativeRelations.incoming(
                a,
                listOf(dependency),
                listOf(a, b),
                listOf(link.copy(issue_link_id = 51)),
            ),
        )
        assertTrue(
            NativeRelations.incoming(a, listOf(dependency), listOf(a, b), emptyList()).isEmpty()
        )
        assertFailsWith<IllegalArgumentException> {
            NativeRelations.incoming(
                a,
                listOf(dependency.copy(type = "changed")),
                listOf(a, b),
                emptyList(),
            )
        }
        // Changing to an unsupported native link kind does not delete the rich relation.
        assertEquals(
            listOf(dependency),
            NativeRelations.incoming(
                a,
                listOf(dependency),
                listOf(a, b),
                listOf(link.copy(link_type = "blocks")),
            ),
        )
    }

    @Test
    fun `removing a shared native link preserves all typed relations even after partial metadata edits`() {
        val a =
            record("a", dependency, duplicate)
                .copy(relationLinks = mapOf("b" to receipt(dependency, duplicate)))
        val b = record("b")
        assertEquals(
            listOf(dependency, duplicate),
            NativeRelations.incoming(a, listOf(dependency, duplicate), listOf(a, b), emptyList()),
        )
        assertEquals(
            listOf(dependency),
            NativeRelations.incoming(a, listOf(dependency), listOf(a, b), emptyList()),
        )
    }

    @Test
    fun `receipt owner can differ from the directed relation source`() {
        val reverse = RelationValue("reverse", "a", "typed")
        val a =
            record("a")
                .copy(relationLinks = mapOf("b" to NativeRelationReceipt(50, setOf("b/reverse"))))
        val b = record("b", reverse)
        assertTrue(
            NativeRelations.incoming(b, listOf(reverse), listOf(a, b), emptyList()).isEmpty()
        )
    }
}
