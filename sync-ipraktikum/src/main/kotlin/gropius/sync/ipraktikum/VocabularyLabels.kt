package gropius.sync.ipraktikum

/** Readable type and priority labels retain canonical keys in issue metadata. */
class VocabularyLabels(catalog: Catalog, kind: String, aliases: Map<String, String>) {
    private val prefix = "$kind/"
    private val names =
        catalog.entries.filter { it.kind == kind }.associate { it.key to (aliases[it.key] ?: it.key) }

    init {
        require(kind in setOf("type", "priority")) { "Unsupported vocabulary label kind" }
        require(names.values.all { it.matches(Regex("[a-zA-Z0-9][a-zA-Z0-9-]{0,63}")) }) {
            "$kind label aliases must contain letters, digits and hyphens (1–64 characters)"
        }
        require(names.values.toSet().size == names.size) { "$kind label aliases must be unique" }
        require(names.all { (key, alias) -> alias !in names || alias == key }) {
            "$kind label alias collides with another entry's stable key"
        }
    }

    fun label(key: String): String = prefix + (names[key] ?: error("Unknown vocabulary key $key"))

    fun key(label: String): String {
        require(label.startsWith(prefix)) { "Unexpected vocabulary label $label" }
        val suffix = label.removePrefix(prefix)
        // Continue accepting canonical labels while a changed profile migrates existing issues.
        return names.entries.singleOrNull { it.value == suffix }?.key
            ?: suffix.takeIf { it in names }
            ?: error("Unknown vocabulary label $label")
    }
}
