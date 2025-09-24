package gropius.sync.github

import gropius.sync.BaseResponseType
import gropius.sync.SyncConfigurationProperties
import gropius.sync.TokenManager
import io.ktor.client.call.*
import io.ktor.client.statement.*
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.neo4j.core.ReactiveNeo4jOperations
import org.springframework.stereotype.Component

/**
 * Response of the getIMSToken login endpoint
 * @param token Token if available
 * @param isImsUserKnown True if the user exists and just has no token
 */
@Serializable
data class GithubTokenResponse(override val token: String?, override val isImsUserKnown: Boolean) : BaseResponseType

@Component
class GithubTokenManager(
    @Qualifier("graphglueNeo4jOperations")
    neoOperations: ReactiveNeo4jOperations, syncConfigurationProperties: SyncConfigurationProperties
) : TokenManager<GithubTokenResponse>(neoOperations, syncConfigurationProperties) {
    override suspend fun parseHttpBody(response: HttpResponse): GithubTokenResponse? {
        return response.body()
    }
}