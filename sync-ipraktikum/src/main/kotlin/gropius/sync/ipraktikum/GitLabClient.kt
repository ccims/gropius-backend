package gropius.sync.ipraktikum

import gropius.model.architecture.IMSProject
import gropius.model.user.IMSUser
import gropius.sync.SyncConfigurationProperties
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import jakarta.annotation.PreDestroy
import java.net.URI
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.neo4j.core.ReactiveNeo4jOperations
import org.springframework.data.neo4j.core.findById
import org.springframework.stereotype.Component

@Serializable
data class GitLabUser(
    val id: Long,
    val username: String,
    val name: String,
    val avatar_url: String? = null,
    val state: String = "active",
)

@Serializable
data class GitLabIssue(
    val id: Long,
    val iid: Long,
    val title: String,
    val description: String? = null,
    val state: String,
    val labels: List<String> = emptyList(),
    val assignees: List<GitLabUser> = emptyList(),
    val confidential: Boolean = false,
    val author: GitLabUser,
    val web_url: String = "",
    val issue_type: String = "issue",
)

@Serializable
data class GitLabNote(
    val id: Long,
    val body: String,
    val author: GitLabUser,
    val system: Boolean = false,
    val internal: Boolean = false,
)

@Serializable
data class GitLabLink(
    val issue_link_id: Long,
    val id: Long,
    val iid: Long,
    val project_id: Long,
    val link_type: String = "relates_to",
)

class RemoteFailure(val status: Int, val uncertain: Boolean) :
    IllegalStateException(
        "GitLab request failed ($status)${if (uncertain) "; write outcome must be reconciled before retry" else ""}"
    )

@Component
class GitLabClient(
    @Qualifier("graphglueNeo4jOperations") private val neo: ReactiveNeo4jOperations,
    private val properties: SyncConfigurationProperties,
    private val registry: Registry,
    private val limits: IPraktikumProperties,
) {
    private val client =
        HttpClient(OkHttp) {
            followRedirects = false
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 10_000
            }
        }
    private val json = Json { ignoreUnknownKeys = true }

    @PreDestroy
    fun close() {
        client.close()
    }

    suspend fun connect(binding: IMSProject, dryRun: Boolean): Session {
        val ims = binding.ims().value
        val root = URI(textField(ims.templatedFields["gitlab-url"]) ?: error("Missing GitLab URL"))
        require(
            root.scheme in setOf("https", "http") &&
                root.host != null &&
                root.userInfo == null &&
                root.query == null &&
                root.fragment == null
        ) {
            "GitLab URL must be an HTTP(S) instance base URL"
        }
        val userId = textField(ims.templatedFields["read-user"]) ?: error("Select the bot IMS user")
        val user = neo.findById<IMSUser>(userId) ?: error("Bot IMS user does not exist")
        require(user.ims().value.rawId == ims.rawId) { "Bot belongs to another IMS" }
        val owner = user.gropiusUser().value ?: error("Link the bot IMS user to a Gropius account")
        require(owner in ims.syncOthersAllowedBy() || owner in binding.syncOthersAllowedBy()) {
            "Grant the bot account permission to sync others on this binding or IMS"
        }
        val response =
            client.get(
                properties.loginServiceBase.toString().trimEnd('/') +
                    "/auth/api/sync/get-ims-token/" +
                    user.rawId
            ) {
                bearerAuth(properties.apiSecret)
            }
        require(response.status.isSuccess()) {
            "Login service could not provide the bot credential"
        }
        val token =
            Json.parseToJsonElement(response.bodyAsText())
                .jsonObject["token"]
                ?.jsonPrimitive
                ?.contentOrNull
                ?: error("Bot has no GitLab credential; link a gitlab-token authentication")
        val project =
            binding.templatedFields["gitlab-project-id"]?.let {
                Json.parseToJsonElement(it).jsonPrimitive.long
            } ?: error("Missing numeric GitLab project ID")
        val session = Session(binding.rawId!!, root.toString().trimEnd('/'), project, token, dryRun)
        session.bot = json.decodeFromJsonElement(session.request("GET", "user"))
        require(
            session.bot.id ==
                Json.parseToJsonElement(user.templatedFields.getValue("gitlab-user-id"))
                    .jsonPrimitive
                    .long
        ) {
            "Credential user identity changed"
        }
        session.request("GET", "projects/$project")
        return session
    }

    inner class Session(
        val binding: String,
        val root: String,
        val project: Long,
        private val token: String,
        val dryRun: Boolean,
    ) {
        lateinit var bot: GitLabUser
        var writes = 0
            private set

        val instance: String
            get() = fingerprint(root)

        suspend fun request(
            method: String,
            path: String,
            body: JsonObject? = null,
            query: Map<String, String> = emptyMap(),
        ): JsonElement {
            val mutation = method != "GET"
            if (mutation) {
                check(!dryRun) { "Dry-run forbids GitLab writes" }
                check(writes < limits.maxWrites) {
                    "Per-cycle write budget reached; remaining operations stay pending"
                }
                registry.fence(binding)
                writes++
            }
            val response =
                try {
                    client.request("$root/api/v4/$path") {
                        this.method = HttpMethod.parse(method)
                        header("PRIVATE-TOKEN", token)
                        query.forEach { (key, value) -> parameter(key, value) }
                        if (body != null) {
                            contentType(ContentType.Application.Json)
                            setBody(body.toString())
                        }
                    }
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    throw RemoteFailure(0, mutation)
                }
            if (!response.status.isSuccess())
                throw RemoteFailure(response.status.value, mutation && response.status.value >= 500)
            val text = response.bodyAsText()
            return if (text.isBlank()) JsonNull else Json.parseToJsonElement(text)
        }

        suspend fun pages(
            path: String,
            query: Map<String, String> = emptyMap(),
        ): List<JsonElement> {
            val result = mutableListOf<JsonElement>()
            for (page in 1..10000) {
                val values =
                    request(
                            "GET",
                            path,
                            query = query + mapOf("page" to "$page", "per_page" to "100"),
                        )
                        .jsonArray
                result.addAll(values)
                if (values.size < 100) return result
            }
            error("Pagination safety limit reached; checkpoint not advanced")
        }

        suspend fun issues(): List<GitLabIssue> =
            pages("projects/$project/issues", mapOf("scope" to "all", "state" to "all")).map {
                json.decodeFromJsonElement(it)
            }

        suspend fun issue(iid: Long): GitLabIssue =
            json.decodeFromJsonElement(request("GET", "projects/$project/issues/$iid"))

        suspend fun create(body: JsonObject): GitLabIssue =
            json.decodeFromJsonElement(request("POST", "projects/$project/issues", body))

        suspend fun update(iid: Long, body: JsonObject): GitLabIssue =
            json.decodeFromJsonElement(request("PUT", "projects/$project/issues/$iid", body))

        suspend fun notes(iid: Long): List<GitLabNote> =
            pages("projects/$project/issues/$iid/notes").map { json.decodeFromJsonElement(it) }

        suspend fun note(iid: Long, body: String, id: Long? = null): GitLabNote =
            json.decodeFromJsonElement(
                request(
                    if (id == null) "POST" else "PUT",
                    "projects/$project/issues/$iid/notes" + (id?.let { "/$it" } ?: ""),
                    buildJsonObject { put("body", body) },
                )
            )

        suspend fun links(iid: Long): List<GitLabLink> =
            pages("projects/$project/issues/$iid/links").map { json.decodeFromJsonElement(it) }

        suspend fun link(iid: Long, target: Long): Long {
            val result =
                request(
                        "POST",
                        "projects/$project/issues/$iid/links",
                        buildJsonObject {
                            put("target_project_id", project)
                            put("target_issue_iid", target)
                            put("link_type", "relates_to")
                        },
                    )
                    .jsonObject
            return result["id"]?.jsonPrimitive?.long
                ?: links(iid).single { it.iid == target }.issue_link_id
        }

        suspend fun unlink(iid: Long, id: Long) {
            request("DELETE", "projects/$project/issues/$iid/links/$id")
        }

        suspend fun labels(): List<JsonObject> =
            pages("projects/$project/labels").map { it.jsonObject }

        suspend fun label(name: String, color: String = "#6699cc", description: String = "") {
            request(
                "POST",
                "projects/$project/labels",
                buildJsonObject {
                    put("name", name)
                    put("color", color)
                    put("description", description)
                },
            )
        }
    }
}
