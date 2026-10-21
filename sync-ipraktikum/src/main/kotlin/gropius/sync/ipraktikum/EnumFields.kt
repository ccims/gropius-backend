package gropius.sync.ipraktikum

import kotlinx.serialization.json.*

/** Optional, template-specific projections of string enums. Unconfigured fields stay local. */
class EnumFields(private val profile: SyncProfile, private val catalog: Catalog) {
    private val mappings =
        profile.templates.flatMap { (template, policy) ->
            policy.enumFields.map { (field, mapping) -> Triple(template, field, mapping) }
        }

    init {
        require(mappings.count { it.third.weights.isNotEmpty() } <= 1) {
            "Only one enum field per repository may project to native weight"
        }
        for ((template, field, mapping) in mappings) {
            val schema = schema(template, field)
            val values =
                schema["enum"]?.jsonArray?.map { it.jsonPrimitive.content }
                    ?: error("Mapped field $field on $template must be a string enum")
            require(mapping.labels.keys == values.toSet()) {
                "Enum mapping for $field must cover exactly the template's allowed values"
            }
            require(
                mapping.labelPrefix.endsWith('/') &&
                    mapping.labelPrefix.length > 1 &&
                    !semanticLabel(mapping.labelPrefix)
            ) {
                "Invalid enum label prefix for $field"
            }
            require(mapping.labels.values.distinct().size == mapping.labels.size) {
                "Enum labels for $field must be unique"
            }
            mapping.labels.values.forEach { label ->
                validateOrdinaryLabel(label)
                require(
                    label.startsWith(mapping.labelPrefix) &&
                        label.length > mapping.labelPrefix.length
                ) {
                    "Enum labels must belong to ${mapping.labelPrefix}"
                }
            }
            require(
                mapping.weights.isEmpty() ||
                    (mapping.weights.keys == values.toSet() &&
                        mapping.weights.values.all { it >= 0 } &&
                        mapping.weights.values.distinct().size == mapping.weights.size)
            ) {
                "Weight mapping for $field must cover all values with distinct non-negative integers"
            }
        }
        for (a in mappings.indices) for (b in a + 1 until mappings.size) {
            val first = mappings[a]
            val second = mappings[b]
            val p = first.third.labelPrefix
            val q = second.third.labelPrefix
            require(
                !(p.startsWith(q) || q.startsWith(p)) || (first.first != second.first && p == q)
            ) {
                "Enum label prefixes overlap within a template or across different namespaces"
            }
        }
    }

    private fun schema(template: String, field: String): JsonObject =
        catalog.entry("template", template).schema[field]?.let {
            Json.parseToJsonElement(it).jsonObject
        } ?: error("Mapped enum field $field no longer exists on template $template")

    fun ownsLabel(label: String): Boolean = mappings.any { label.startsWith(it.third.labelPrefix) }

    fun labels(): Set<String> = mappings.flatMap { it.third.labels.values }.toSet()

    fun values(template: String, fields: Map<String, JsonElement>): Map<String, JsonElement> =
        profile.templates[template]?.enumFields.orEmpty().mapValues { (field, mapping) ->
            val value = fields[field] ?: JsonNull
            if (value == JsonNull)
                require(schema(template, field)["nullable"]?.jsonPrimitive?.booleanOrNull == true) {
                    "Mapped field $field does not allow an unset value"
                }
            else
                require(
                    value is JsonPrimitive && value.isString && value.content in mapping.labels
                ) {
                    "Invalid enum value for $field: $value"
                }
            value
        }

    fun labels(value: IssueValue): Set<String> =
        values(value.template, value.fields)
            .mapNotNull { (field, content) ->
                if (content == JsonNull) null
                else
                    profile.templates
                        .getValue(value.template)
                        .enumFields
                        .getValue(field)
                        .labels
                        .getValue(content.jsonPrimitive.content)
            }
            .toSet()

    fun hasWeight(template: String): Boolean =
        profile.templates[template]?.enumFields.orEmpty().values.any { it.weights.isNotEmpty() }

    fun weight(value: IssueValue): Int? {
        val entry =
            profile.templates[value.template]?.enumFields.orEmpty().entries.singleOrNull {
                it.value.weights.isNotEmpty()
            } ?: return null
        val content = values(value.template, value.fields).getValue(entry.key)
        return if (content == JsonNull) null
        else entry.value.weights.getValue(content.jsonPrimitive.content)
    }

    fun incoming(
        template: String,
        metadata: Map<String, JsonElement>,
        remote: GitLabIssue,
        record: IssueRecord,
        configHash: String,
    ): Map<String, JsonElement> {
        val fields =
            profile.templates[template]
                ?.enumFields
                .orEmpty()
                .keys
                .associateWith {
                    metadata[it]
                        ?: (if (record.configHash == null) record.baseline?.fields?.get(it)
                        else null)
                        ?: profile.templates[template]?.requiredFields?.get(it)
                        ?: JsonNull
                }
                .toMutableMap()
        for ((field, mapping) in profile.templates[template]?.enumFields.orEmpty()) {
            val selected = remote.labels.filter { it.startsWith(mapping.labelPrefix) }.toSet()
            require(selected.size <= 1) { "Choose at most one ${mapping.labelPrefix} label" }
            val labelValue =
                selected
                    .singleOrNull()
                    ?.let { label ->
                        mapping.labels.entries.singleOrNull { it.value == label }?.key
                            ?: error("Unknown enum label $label for $field")
                    }
                    ?.let { JsonPrimitive(it) } ?: JsonNull
            val changedConfig = record.configHash != null && record.configHash != configHash
            if (changedConfig)
                continue // Re-project existing exact/local values after configuration changes.
            val base = record.baseline?.fields?.get(field) ?: JsonNull
            val candidates = mutableListOf<JsonElement>()
            val exact = fields.getValue(field)
            if (record.baseline == null) {
                if (field in metadata) candidates += exact
                if (selected.isNotEmpty()) candidates += labelValue
            } else {
                if (exact != base) candidates += exact
                if (
                    selected !=
                        record.lastLabels.filter { it.startsWith(mapping.labelPrefix) }.toSet()
                )
                    candidates += labelValue
            }
            if (
                mapping.weights.isNotEmpty() &&
                    (if (record.baseline == null) remote.weight != null
                    else remote.weight != record.lastWeight)
            ) {
                val fromWeight =
                    remote.weight
                        ?.let { weight ->
                            mapping.weights.entries.singleOrNull { it.value == weight }?.key
                                ?: error("Unmapped native weight $weight for $field")
                        }
                        ?.let { JsonPrimitive(it) } ?: JsonNull
                candidates += fromWeight
            }
            require(candidates.distinct().size <= 1) {
                "Enum field $field has conflicting metadata, label or weight edits"
            }
            fields[field] = candidates.firstOrNull() ?: exact
        }
        return values(template, fields)
    }
}
