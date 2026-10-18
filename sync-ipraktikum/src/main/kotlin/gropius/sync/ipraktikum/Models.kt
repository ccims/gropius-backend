package gropius.sync.ipraktikum

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.security.MessageDigest

val syncJson = Json { ignoreUnknownKeys = false; encodeDefaults = true }

@Serializable
data class SyncProfile(
    val enabled: Boolean = false,
    val dryRun: Boolean = true,
    val revision: Int = 1,
    val defaultTemplate: String? = null,
    val templates: Map<String, TemplatePolicy> = emptyMap(),
    val excludedComponents: Set<String> = emptySet(),
    val componentLabels: Map<String, String> = emptyMap(),
    val primary: Map<String, String?> = emptyMap(),
    val resolutions: Map<String, Resolution> = emptyMap(),
    val detached: Set<String> = emptySet(),
    val adoptCopies: Set<Long> = emptySet(),
    val retryCreates: Map<String, String> = emptyMap(),
    val repairLabels: Set<String> = emptySet()
)

@Serializable
data class TemplatePolicy(
    val defaultType: String,
    val initialState: String,
    val closedState: String,
    val reopenState: String,
    val states: Map<String, String> = emptyMap(),
    val bucketDefaults: Map<String, String> = emptyMap(),
    val autoStates: Boolean = true,
    val nativeAssignmentEnabled: Boolean = false,
    val nativeAssignmentRole: String? = null,
    val genericRelationEnabled: Boolean = false,
    val genericRelationType: String? = null,
    val requiredFields: Map<String, JsonElement> = emptyMap()
)

@Serializable
data class Resolution(val fingerprint: String, val side: String)

@Serializable
data class CatalogEntry(
    val id: String, val key: String, val name: String,
    val kind: String, val template: String? = null,
    val open: Boolean? = null, val schema: Map<String, String> = emptyMap(),
    val creatable: Boolean = true
)

@Serializable
data class Catalog(val revision: String = "", val entries: List<CatalogEntry> = emptyList()) {
    fun entry(kind: String, key: String, template: String? = null): CatalogEntry = entries.singleOrNull {
        it.kind == kind && it.key == key && (template == null || it.template == template)
    } ?: error("Unknown or incompatible $kind: $key")
    fun key(kind: String, id: String): String = entries.firstOrNull { it.kind == kind && it.id == id }?.key
        ?: error("$kind no longer exists in the catalog")
}

@Serializable
data class AssignmentValue(val id: String, val user: String, val role: String? = null)
@Serializable
data class RelationValue(val id: String, val target: String, val type: String? = null)

@Serializable
data class IssueValue(
    val title: String,
    val body: String,
    val template: String,
    val type: String,
    val state: String,
    val priority: String? = null,
    val components: Set<String> = emptySet(),
    val primary: String? = "auto",
    val assignments: List<AssignmentValue> = emptyList(),
    val relations: List<RelationValue> = emptyList()
)

@Serializable
data class IssueRecord(
    val syncId: String,
    val binding: String,
    val gropiusId: String? = null,
    val remoteId: Long? = null,
    val iid: Long? = null,
    val baseline: IssueValue? = null,
    val pending: IssueValue? = null,
    val operation: String? = null,
    val expectedRemote: IssueValue? = null,
    val assignmentIds: Map<String, String> = emptyMap(),
    val relationIds: Map<String, String> = emptyMap(),
    val nativeLinks: Map<String, Long> = emptyMap(),
    val comments: Map<String, CommentRecord> = emptyMap(),
    val configHash: String? = null,
    val lastLabels: Set<String> = emptySet(),
    val lastAssignee: Long? = null,
    val lastOpen: Boolean? = null,
    val blockedAssignee: String? = null,
    val warnings: List<String> = emptyList(),
    val detached: Boolean = false,
    val error: String? = null
)

@Serializable
data class CommentRecord(val localId: String, val remoteId: Long? = null, val body: String,
    val operation: String, val correctionId: Long? = null, val deleted: Boolean = false,
    val remoteBody: String = body)

@Serializable
data class SyncStatus(
    val at: String, val ready: Boolean = false, val dryRun: Boolean = true,
    val issues: Int = 0, val writes: Int = 0,
    val messages: List<String> = emptyList()
)

fun fingerprint(value: String): String = MessageDigest.getInstance("SHA-256")
    .digest(value.toByteArray()).joinToString("") { "%02x".format(it) }

/** Name only supplies readability. Identity supplies the stable, collision-resistant suffix. */
fun stableKey(kind: String, id: String, name: String): String =
    name.lowercase().replace(Regex("[^a-z0-9-]+"), "-").trim('-').take(40).ifEmpty { kind } +
        "-" + fingerprint("$kind:$id").take(12)

class SyncConflict(val fields: List<String>, val token: String) : IllegalStateException(
    "Concurrent changes in ${fields.joinToString()}; conflict fingerprint: $token"
)

/** Three-way merge by stable entry identity, rather than last-modified timestamps. */
object Merge {
    fun reconcile(base: IssueValue, local: IssueValue, remote: IssueValue): IssueValue {
        val conflicts = mutableListOf<String>()
        fun <T> field(name: String, b: T, l: T, r: T): T = when {
            l == r -> l
            l == b -> r
            r == b -> l
            else -> { conflicts += name; l }
        }
        fun <T> entries(name: String, b: List<T>, l: List<T>, r: List<T>, id: (T) -> String): List<T> {
            val bm = b.associateBy(id); val lm = l.associateBy(id); val rm = r.associateBy(id)
            return (bm.keys + lm.keys + rm.keys).sorted().mapNotNull { key ->
                field("$name/$key", bm[key], lm[key], rm[key])
            }
        }
        val components = (base.components + local.components + remote.components).filter { key ->
            field("components/$key", key in base.components, key in local.components, key in remote.components)
        }.toSet()
        val result = IssueValue(
            field("title", base.title, local.title, remote.title),
            field("body", base.body, local.body, remote.body),
            field("template", base.template, local.template, remote.template),
            field("type", base.type, local.type, remote.type),
            field("state", base.state, local.state, remote.state),
            field("priority", base.priority, local.priority, remote.priority), components,
            field("primary", base.primary, local.primary, remote.primary),
            entries("assignments", base.assignments, local.assignments, remote.assignments) { it.id },
            entries("relations", base.relations, local.relations, remote.relations) { it.id }
        )
        if (conflicts.isNotEmpty()) throw SyncConflict(conflicts,
            fingerprint(syncJson.encodeToString(local) + syncJson.encodeToString(remote)))
        return result
    }
}
