package gropius.sync

import gropius.model.architecture.IMS
import gropius.model.user.GropiusUser
import gropius.model.user.IMSUser
import gropius.model.user.User
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.neo4j.core.ReactiveNeo4jOperations
import org.springframework.data.neo4j.core.findById
import java.util.*

/**
 * Base class for Login Service API responses
 */
interface BaseResponseType {
    /**
     * Token from the login service
     */
    val token: String?

    /**
     * If the IMSUser is known to the login service
     */
    val isImsUserKnown: Boolean
}

data class LinkImsUserQuery(val imsUserIds: List<String>)

/**
 * Manager for token from login service
 * @param neoOperations Reference for the spring instance of ReactiveNeo4jOperations
 * @param syncConfigurationProperties Reference for the spring instance of GropiusGithubConfigurationProperties
 */
abstract class TokenManager<ResponseType : BaseResponseType>(
    @Qualifier("graphglueNeo4jOperations")
    private val neoOperations: ReactiveNeo4jOperations,
    private val syncConfigurationProperties: SyncConfigurationProperties
) {

    companion object {
        const val JSON_CHARSET_MIME_TYPE = "application/json; charset=utf-8"
    }

    /**
     * Logger used to print notifications
     */
    private val logger = LoggerFactory.getLogger(TokenManager::class.java)

    /**
     * Http Client for Login Service requests
     */
    private val client = HttpClient() {
        expectSuccess = true
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
            json(Json {
                ignoreUnknownKeys = true
            }, contentType = ContentType.parse(JSON_CHARSET_MIME_TYPE))
        }
    }

    /**
     * Request an user token from the auth service
     * @param imsUser The IMSUser the token should be for
     * @return token if available
     */
    private suspend fun getUserToken(imsUser: IMSUser): ResponseType? {
        val tokenResponse: ResponseType? =
            parseHttpBody(client.get(syncConfigurationProperties.loginServiceBase.toString()) {
                url {
                    appendPathSegments("auth", "api", "sync", "get-ims-token", imsUser.rawId!!)
                }
                headers {
                    append(HttpHeaders.Authorization, "Bearer " + syncConfigurationProperties.apiSecret)
                }
            })
        return tokenResponse
    }

    /**
     * Parse the response of a token query
     *
     * @param response The response to parse
     * @return The parsed response
     */
    abstract suspend fun parseHttpBody(response: HttpResponse): ResponseType?

    /**
     * Load the token of from the login service
     * @param ims IMS the user has to belong to
     * @param readUser default user-id to fallback on
     * @param imsUser user to ask the token for
     * @return GitHub auth token
     */
    suspend fun getTokenForIMSUser(ims: IMS, readUser: String, imsUser: IMSUser?): ResponseType {
        val readUserInfo =
            imsUser ?: neoOperations.findById<IMSUser>(readUser) ?: throw SyncNotificator.NotificatedError(
                "SYNC_GITHUB_USER_NOT_FOUND"
            )
        if (readUserInfo.ims().value != ims) {
            throw SyncNotificator.NotificatedError(
                "SYNC_GITHUB_USER_INVALID_IMS"
            )
        }
        val tokenData = getUserToken(readUserInfo)
        if (tokenData?.token == null) throw SyncNotificator.NotificatedError(
            "SYNC_GITHUB_USER_NO_TOKEN"
        )
        return tokenData
    }

    /**
     * Collect neighboring users for a given ims and user combo
     *
     * @param ims The IMS to work with
     * @param user The user to get the neighbors for
     * @return The list of users, empty if none found
     */
    private suspend fun getPossibleUsersForUser(ims: IMS, user: User): List<IMSUser> {
        val ret = mutableListOf<IMSUser>()
        if (user is IMSUser) {
            if (user.ims().value == ims) {
                ret.add(user)
            }
        }
        val gropiusUser = if (user is IMSUser) user.gropiusUser().value else user as GropiusUser
        if (gropiusUser != null) {
            for (imsUser in gropiusUser.imsUsers()) {
                if (imsUser.ims().value == ims) {
                    ret.add(imsUser)
                }
            }
        }
        return ret
    }

    /**
     * Attempt a query for a list of users until it works
     *
     * @param users The list of users, sorted with best first
     * @param executor The function to execute
     *
     * @return The user it worked with and the result of the executor
     */
    private suspend fun <T> executeUntilWorking(
        users: List<IMSUser>, executor: suspend (token: ResponseType) -> Optional<T>
    ): Pair<IMSUser, T> {
        for (user in users) {
            val token = getUserToken(user)
            if (token?.token != null) {
                logger.trace("Trying token of user ${user.rawId}")
                val ret = executor(token)
                if (ret.isPresent) {
                    return user to ret.get()
                }
            } else {
                logger.trace("User ${user.rawId} had no token")
            }
        }
        TODO("Error Message")
    }

    /**
     * Attempt a query for a list of users until it works
     *
     * @param ims The IMS to work with
     * @param user The list of users, sorted with best first
     * @param executor The function to execute
     *
     * @return The user it worked with and the result of the executor
     */
    suspend fun <T> executeUntilWorking(
        ims: IMS, user: List<User>, executor: suspend (token: ResponseType) -> Optional<T>
    ): Pair<IMSUser, T> {
        val users = user.map { getPossibleUsersForUser(ims, it) }.flatten().distinct()
        logger.info("Expanding ${user.map { "${it::class.simpleName}:${it.rawId}(${it.username})" }} to ${users.map { "${it::class.simpleName}:${it.rawId}(${it.username})" }}")
        return executeUntilWorking(users, executor)
    }

    /**
     * Advertise to the login service that an IMSUser  has been created
     * @param user The user AFTER BEING SAVED TO DB (valid, non-null rawId)
     */
    suspend fun advertiseIMSUser(user: IMSUser) {
        client.post(syncConfigurationProperties.loginServiceBase.toString()) {
            url {
                appendPathSegments("auth", "api", "sync", "link-ims-users")
            }
            contentType(ContentType.Application.Json)
            setBody(LinkImsUserQuery(listOf(user.rawId!!)))
            headers {
                append(HttpHeaders.Authorization, "Bearer " + syncConfigurationProperties.apiSecret)
            }
        }
    }
}
