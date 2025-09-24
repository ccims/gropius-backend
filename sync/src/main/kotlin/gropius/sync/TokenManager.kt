package gropius.sync

import gropius.model.architecture.IMS
import gropius.model.architecture.IMSProject
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
import kotlinx.coroutines.delay
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

@Serializable
data class LinkImsUserQuery(val imsUserIds: List<String>)

/**
 * Exception thrown when no valid token is available
 */
class NoTokenValidException : Exception()

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
     * Check if a user is allowed to be used for syncing
     *
     * @param imsProject The IMS to work with
     * @param user The user to check
     * @param owner The user that created the data, empty if fetching/other non-owned operations
     * @return true if the user is allowed
     */
    private suspend fun isAllowed(imsProject: IMSProject, user: IMSUser, owner: List<GropiusUser>): Boolean {
        val ownerSet = owner.toSet()
        if ((owner.isEmpty() || ownerSet.contains(user.gropiusUser().value)) && imsProject.ims().value.syncSelfAllowedBy()
                .contains(user.gropiusUser().value)
        ) {
            return true
        }
        if ((owner.isEmpty() || ownerSet.contains(user.gropiusUser().value)) && imsProject.syncSelfAllowedBy()
                .contains(user.gropiusUser().value)
        ) {
            return true
        }
        if (imsProject.ims().value.syncOthersAllowedBy().contains(user.gropiusUser().value)) {
            return true
        }
        if (imsProject.syncOthersAllowedBy().contains(user.gropiusUser().value)) {
            return true
        }
        return false
    }

    /**
     * Attempt a query for a list of users until it works
     *
     * @param imsProject The IMS to work with
     * @param users The list of users, sorted with best first
     * @param executor The function to execute
     * @param owner The user that created the data, empty if fetching/other non-owned operations
     *
     * @return The user it worked with and the result of the executor
     */
    private suspend fun <T> executeUntilWorking(
        imsProject: IMSProject,
        users: List<IMSUser>,
        executor: suspend (token: ResponseType) -> Optional<T>,
        owner: List<GropiusUser>
    ): Pair<IMSUser, T> {
        for (user in users) {
            if (isAllowed(imsProject, user, owner)) {
                val token = getUserToken(user)
                if (token?.token != null) {
                    logger.debug("Trying token of user ${user.rawId}")
                    val ret = executor(token)
                    if (ret.isPresent) {
                        return user to ret.get()
                    }
                } else {
                    logger.debug("User ${user.rawId} had no token")
                }
            } else {
                logger.debug("User $user does not allow sync from $owner")
            }
        }
        throw NoTokenValidException()
    }

    /**
     * Attempt a query for a list of users until it works
     *
     * @param imsProject The IMS to work with
     * @param user The list of users, sorted with best first
     * @param executor The function to execute
     * @param owner The user that created the data, empty if fetching/other non-owned operations
     *
     * @return The user it worked with and the result of the executor
     */
    suspend fun <T> executeUntilWorking(
        imsProject: IMSProject,
        user: List<User>,
        owner: List<GropiusUser>,
        executor: suspend (token: ResponseType) -> Optional<T>
    ): Pair<IMSUser, T> {
        val users = user.map { getPossibleUsersForUser(imsProject.ims().value, it) }.flatten().distinct()
        logger.info("Expanding ${user.map { "${it::class.simpleName}:${it.rawId}(${it.username})" }} to ${users.map { "${it::class.simpleName}:${it.rawId}(${it.username})" }}")
        return executeUntilWorking(imsProject, users, {
            delay(1100)
            executor(it)
        }, owner)
    }

    /**
     * Advertise to the login service that an IMSUser  has been created
     * @param user The user AFTER BEING SAVED TO DB (valid, non-null rawId)
     */
    suspend fun advertiseIMSUser(user: IMSUser) {
        client.put(syncConfigurationProperties.loginServiceBase.toString()) {
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
