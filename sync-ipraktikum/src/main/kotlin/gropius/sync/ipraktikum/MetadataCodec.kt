package gropius.sync.ipraktikum

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.snakeyaml.engine.v2.api.Load
import org.snakeyaml.engine.v2.api.LoadSettings

@Serializable
data class EditableMetadata(
    val schema: Int = 1,
    val revision: Int = 1,
    val template: String,
    val issue_type: String,
    val state: String,
    val priority: String? = null,
    val primary_assignment: String? = "auto",
    val assignments: List<AssignmentValue> = emptyList(),
    val relations: List<RelationValue> = emptyList(),
    val fields: Map<String, JsonElement> = emptyMap(),
)

@Serializable data class Identity(val schema: Int = 1, val binding: String, val syncId: String)

data class ParsedDescription(
    val body: String,
    val identity: Identity?,
    val metadata: EditableMetadata?,
)

object MetadataCodec {
    const val START = "<!-- ipraktikum-sync:start -->"
    const val END = "<!-- ipraktikum-sync:end -->"
    private val identityPattern = Regex("<!-- ipraktikum-sync:identity (\\{[^\\r\\n]+}) -->")
    private val yamlPattern = Regex("```yaml\\r?\\n([\\s\\S]*?)\\r?\\n```", RegexOption.MULTILINE)
    private val pretty = Json(syncJson) { prettyPrint = true }

    fun parse(description: String): ParsedDescription {
        require(description.length <= 1_000_000) { "Description exceeds metadata limit" }
        val starts = Regex(Regex.escape(START)).findAll(description).toList()
        val ends = Regex(Regex.escape(END)).findAll(description).toList()
        if (starts.isEmpty() && ends.isEmpty()) {
            require(!description.contains("ipraktikum-sync:")) { "Incomplete adapter section" }
            return ParsedDescription(description, null, null)
        }
        require(starts.size == 1 && ends.size == 1 && starts[0].range.first < ends[0].range.first) {
            "Expected exactly one complete adapter section"
        }
        val start = starts.single().range.first
        val end = ends.single().range.last + 1
        val section = description.substring(start, end)
        val identities = identityPattern.findAll(section).toList()
        require(identities.size == 1) { "Missing or duplicate identity" }
        val identity = syncJson.decodeFromString<Identity>(identities.single().groupValues[1])
        require(identity.schema == 1 && identity.syncId.matches(Regex("[a-zA-Z0-9-]{1,80}"))) {
            "Invalid identity"
        }
        val yamls = yamlPattern.findAll(section).toList()
        require(yamls.size == 1) { "Expected exactly one metadata YAML document" }
        val yaml = yamls.single().groupValues[1]
        require(yaml.length <= 100_000) { "Metadata is too large" }
        val loader =
            Load(
                LoadSettings.builder()
                    .setAllowDuplicateKeys(false)
                    .setMaxAliasesForCollections(0)
                    .setCodePointLimit(100_000)
                    .build()
            )
        fun json(value: Any?, depth: Int = 0): JsonElement {
            require(depth < 20) { "Metadata nesting limit exceeded" }
            return when (value) {
                null -> JsonNull
                is String -> JsonPrimitive(value)
                is Boolean -> JsonPrimitive(value)
                is Number -> JsonPrimitive(value)
                is Map<*, *> ->
                    JsonObject(
                        value.entries.associate { (k, v) ->
                            require(k is String) { "Metadata keys must be strings" }
                            k to json(v, depth + 1)
                        }
                    )
                is List<*> -> JsonArray(value.map { json(it, depth + 1) })
                else -> error("Unsupported YAML value")
            }
        }
        val metadata =
            syncJson.decodeFromJsonElement<EditableMetadata>(json(loader.loadFromString(yaml)))
        require(metadata.schema == 1) { "Unsupported metadata schema" }
        require(metadata.assignments.size <= 200 && metadata.relations.size <= 500) {
            "Too many metadata entries"
        }
        for (ids in listOf(metadata.assignments.map { it.id }, metadata.relations.map { it.id })) {
            require(
                ids.size == ids.toSet().size &&
                    ids.all { it.matches(Regex("[a-zA-Z0-9_-]{1,128}")) }
            ) {
                "Entry IDs must be unique and contain only letters, digits, underscores or hyphens"
            }
        }
        val before = description.substring(0, start).removeSuffix("\n\n")
        return ParsedDescription(before + description.substring(end), identity, metadata)
    }

    fun render(
        body: String,
        identity: Identity,
        metadata: EditableMetadata,
        summary: String = "",
    ): String {
        require(!body.contains("ipraktikum-sync:")) {
            "User text contains reserved adapter markers"
        }
        return body +
            "\n\n" +
            START +
            "\n<!-- ipraktikum-sync:identity " +
            syncJson.encodeToString(identity) +
            " -->\n" +
            "<details>\n<summary>iPraktikum state, relations and assignments (editable)</summary>\n\n" +
            "```yaml\n" +
            pretty.encodeToString(metadata) +
            "\n```\n\n</details>\n" +
            summary +
            "\n" +
            END
    }
}
