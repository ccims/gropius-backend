package gropius.sync.ipraktikum

/** Human-facing label aliases are independent of the canonical component keys. */
class ComponentLabels(catalog: Catalog, profile: SyncProfile) {
    private val names = catalog.entries.filter { it.kind == "component" }.associate {
        it.key to (profile.componentLabels[it.key] ?: it.key)
    }

    init {
        require(names.values.all { it.matches(Regex("[a-z0-9][a-z0-9-]{0,63}")) }) {
            "Component label aliases must contain lowercase letters, digits and hyphens (1–64 characters)"
        }
        require(names.values.toSet().size == names.size) { "Component label aliases must be unique" }
        require(names.all { (key, alias) -> alias !in names || alias == key }) {
            "Component label alias collides with another component's stable key"
        }
    }

    fun label(key: String): String = "component/${names[key] ?: error("Unknown component $key")}"

    fun component(label: String): String {
        val suffix = label.removePrefix("component/")
        // Accept the previous stable-key spelling while a changed profile migrates labels.
        return names.entries.singleOrNull { it.value == suffix }?.key
            ?: suffix.takeIf { it in names }
            ?: error("Unknown component label $label")
    }
}
