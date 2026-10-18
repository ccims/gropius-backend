package gropius.sync.ipraktikum

import gropius.model.architecture.IMSProject
import gropius.model.architecture.Project
import gropius.model.architecture.Component as SoftwareComponent
import gropius.model.template.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.serialization.json.JsonPrimitive
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.data.neo4j.core.ReactiveNeo4jOperations
import org.springframework.data.neo4j.core.findAll
import org.springframework.data.neo4j.core.findById
import org.springframework.stereotype.Component

const val TEMPLATE_NAME = "iPraktikum-Issue-Sync"
fun textField(value: String?): String? = value?.let { syncJson.decodeFromString<String?>(it) }
fun IMSProject.profile(): SyncProfile = textField(templatedFields["sync-profile"])?.let {
    syncJson.decodeFromString<SyncProfile>(it)
} ?: SyncProfile()

@ConfigurationProperties("gropius.ipraktikum")
data class IPraktikumProperties(val interval: Long = 60_000, val maxWrites: Int = 100)

@Component
class Configuration(
    @Qualifier("graphglueNeo4jOperations") private val neo: ReactiveNeo4jOperations,
    private val registry: Registry
) {
    private val nullableString = """{"type":"string","nullable":true}"""

    suspend fun bootstrap(): IMSTemplate {
        val existing = neo.findAll<IMSTemplate>().toList().filter { it.name == TEMPLATE_NAME && !it.isDeprecated }
        require(existing.size <= 1) { "Multiple active iPraktikum templates; retire duplicate templates" }
        existing.singleOrNull()?.let { return it }
        val ims = IMSTemplate(TEMPLATE_NAME, "GitLab Free monorepo synchronization", mutableMapOf(
            "gitlab-url" to """{"type":"string"}""", "read-user" to
                """{"type":"string","nullable":true,"metadata":{"gropius-node":"IMSUser"}}"""
        ), false, false)
        ims.imsProjectTemplate().value = IMSProjectTemplate(TEMPLATE_NAME, "Repository binding", mutableMapOf(
            "gitlab-project-id" to """{"type":"int32"}""",
            "sync-profile" to nullableString, "sync-catalog" to nullableString, "sync-status" to nullableString
        ))
        ims.imsIssueTemplate().value = IMSIssueTemplate("iPraktikum issue", "Stable identity", mutableMapOf(
            "sync-id" to """{"type":"string"}""", "url" to nullableString
        ))
        ims.imsUserTemplate().value = IMSUserTemplate("iPraktikum GitLab user", "Instance-scoped user identity",
            mutableMapOf("gitlab-user-id" to """{"type":"int32"}"""))
        ims.imsUserTemplate().value.partOf().value = ims
        return neo.save(ims).awaitSingle()
    }

    suspend fun discover(binding: IMSProject, profile: SyncProfile): Pair<Catalog, List<SoftwareComponent>> {
        val project = binding.trackable().value as? Project ?: error("Repository binding must refer to a Gropius Project")
        val components = project.components().map { it.component().value }.distinctBy { it.rawId }
            .filter { it.rawId !in profile.excludedComponents }
        val entries = mutableListOf<CatalogEntry>()
        for (c in components) entries += CatalogEntry(c.rawId!!,
            registry.key("component", c.rawId!!, c.name), c.name, "component")
        for (template in neo.findAll<IssueTemplate>().toList()) {
            val key = registry.key("template", template.rawId!!, template.name)
            entries += CatalogEntry(template.rawId!!, key, template.name, "template",
                schema = template.templateFieldSpecifications.toMap(), creatable = !template.isDeprecated && !template.isAbstract)
            for (s in template.issueStates()) entries += CatalogEntry(s.rawId!!,
                registry.key("state", s.rawId!!, s.name), s.name, "state", key, s.isOpen)
            for (t in template.issueTypes()) entries += CatalogEntry(t.rawId!!,
                registry.key("type", t.rawId!!, t.name), t.name, "type", key)
            for (t in template.issuePriorities()) entries += CatalogEntry(t.rawId!!,
                registry.key("priority", t.rawId!!, t.name), t.name, "priority", key)
            for (t in template.assignmentTypes()) entries += CatalogEntry(t.rawId!!,
                registry.key("role", t.rawId!!, t.name), t.name, "role", key)
            for (t in template.relationTypes()) entries += CatalogEntry(t.rawId!!,
                registry.key("relation", t.rawId!!, t.name), "${t.name} / ${t.inverseName}", "relation", key)
        }
        val sorted = entries.sortedWith(compareBy({ it.kind }, { it.key }, { it.template }))
        val catalog = Catalog(fingerprint(syncJson.encodeToString(sorted)), sorted)
        publish(binding, "sync-catalog", syncJson.encodeToString(catalog))
        return catalog to components
    }

    suspend fun publish(binding: IMSProject, field: String, value: String) {
        val current = neo.findById<IMSProject>(binding.rawId!!) ?: return
        current.templatedFields[field] = syncJson.encodeToString(value)
        neo.save(current).awaitSingle() // BaseNode optimistic version protects concurrent profile edits.
    }
}
