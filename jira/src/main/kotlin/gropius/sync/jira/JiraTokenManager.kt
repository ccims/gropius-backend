package gropius.sync.jira

import gropius.sync.BaseResponseType
import gropius.sync.SyncConfigurationProperties
import gropius.sync.TokenManager
import io.ktor.client.call.*
import io.ktor.client.statement.*
import kotlinx.serialization.Serializable
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.neo4j.core.ReactiveNeo4jOperations
import org.springframework.stereotype.Component
import java.util.*

@Serializable
data class JiraCloudId(val id: String, val url: String, val name: String, val scopes: List<String>)

/**
 * Response of the getIMSToken login endpoint
 * @param token Token if available
 * @param isImsUserKnown True if the user exists and just has no token
 */
@Serializable
data class JiraTokenResponse(
    override val token: String?,
    override val isImsUserKnown: Boolean,
    val cloudIds: List<JiraCloudId>? = null,
    val type: String? = null
) : BaseResponseType

@Component
class JiraTokenManager(
    @Qualifier("graphglueNeo4jOperations")
    neoOperations: ReactiveNeo4jOperations, syncConfigurationProperties: SyncConfigurationProperties
) : TokenManager<JiraTokenResponse>(neoOperations, syncConfigurationProperties) {
    override suspend fun parseHttpBody(response: HttpResponse): JiraTokenResponse? {
        return response.body<JiraTokenResponse>()
    }
}