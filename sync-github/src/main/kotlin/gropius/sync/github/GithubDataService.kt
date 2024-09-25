package gropius.sync.github

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.network.http.HttpInfo
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import gropius.model.architecture.IMSProject
import gropius.model.issue.Label
import gropius.model.template.*
import gropius.model.user.GropiusUser
import gropius.model.user.IMSUser
import gropius.model.user.User
import gropius.repository.user.GropiusUserRepository
import gropius.sync.JsonHelper
import gropius.sync.SyncDataService
import gropius.sync.github.config.IMSConfig
import gropius.sync.github.config.IMSProjectConfig
import gropius.sync.github.generated.fragment.LabelData
import gropius.sync.github.generated.fragment.UserData
import gropius.sync.github.generated.fragment.UserData.Companion.asUser
import gropius.sync.model.LabelInfo
import gropius.sync.repository.LabelInfoRepository
import gropius.util.JsonNodeMapper
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.neo4j.core.ReactiveNeo4jOperations
import org.springframework.data.neo4j.core.findById
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Exception for GitHub response errors
 * @param errors the errors GitHub sent us
 */
class GitHubResponseException(val errors: List<com.apollographql.apollo3.api.Error>) : Exception(errors.toString())

/**
 * Service to handle data from GitHub
 * @param issuePileService the issue pile service to use
 * @param neoOperations Reference for the spring instance of ReactiveNeo4jOperations
 * @param labelInfoRepository the label info repository to use
 * @param tokenManager Reference for the spring instance of GithubTokenManager
 * @param helper Reference for the spring instance of JsonHelper
 * @param objectMapper Reference for the spring instance of ObjectMapper
 * @param jsonNodeMapper Reference for the spring instance of JsonNodeMapper
 * @param gropiusUserRepository Reference for the spring instance of GropiusUserRepository
 */
@Component
class GithubDataService(
    val issuePileService: IssuePileService,
    @Qualifier("graphglueNeo4jOperations")
    val neoOperations: ReactiveNeo4jOperations,
    val labelInfoRepository: LabelInfoRepository,
    val tokenManager: GithubTokenManager,
    val helper: JsonHelper,
    val objectMapper: ObjectMapper,
    val jsonNodeMapper: JsonNodeMapper,
    val gropiusUserRepository: GropiusUserRepository
) : SyncDataService {

    companion object {
        const val FALLBACK_USER_NAME = "github"
        val TOKEN_WOUND_UP_IN_JAIL = ConcurrentHashMap<String, OffsetDateTime>()
    }

    /**
     * Logger used to print notifications
     */
    final val logger = LoggerFactory.getLogger(GithubDataService::class.java)

    /**
     * Find and ensure the IMSTemplate in the database
     * @param imsProject The IMSProject to work with
     * @return the IssueTemplate
     */
    suspend fun issueTemplate(imsProject: IMSProject): IssueTemplate {
        val imsProjectConfig = IMSProjectConfig(helper, imsProject)
        if (imsProjectConfig.defaultTemplate != null) {
            val template = neoOperations.findById<IssueTemplate>(imsProjectConfig.defaultTemplate)
            if (template != null) {
                return template
            }
        }
        val imsConfig = IMSConfig(helper, imsProject.ims().value, imsProject.ims().value.template().value)
        if (imsConfig.defaultTemplate != null) {
            val template = neoOperations.findById<IssueTemplate>(imsConfig.defaultTemplate)
            if (template != null) {
                return template
            }
        }
        return neoOperations.findAll(IssueTemplate::class.java).awaitFirstOrNull() ?: neoOperations.save(
            IssueTemplate("noissue", "", mutableMapOf(), false)
        ).awaitSingle()
    }

    /**
     * Get a IMSUser for a GitHub user
     * @param imsProject the project to map the user to
     * @param userData the Jira user
     * @return The gropius user
     */
    suspend fun mapUser(imsProject: IMSProject, userData: UserData?): User {
        val databaseId = userData?.asUser()?.databaseId ?: 0
        val encodedAccountId =
            jsonNodeMapper.jsonNodeToDeterministicString(objectMapper.valueToTree<JsonNode>(databaseId))
        val foundImsUser =
            imsProject.ims().value.users().firstOrNull { it.templatedFields["github_id"] == encodedAccountId }
        if (foundImsUser != null) {
            return foundImsUser
        }
        val imsUser = IMSUser(
            userData?.asUser()?.name ?: userData?.login ?: FALLBACK_USER_NAME,
            userData?.asUser()?.email,
            null,
            userData?.login ?: FALLBACK_USER_NAME,
            mutableMapOf("github_id" to encodedAccountId)
        )
        imsUser.ims().value = imsProject.ims().value
        imsUser.template().value = imsUser.ims().value.template().value.imsUserTemplate().value
        val newUser = neoOperations.save(imsUser).awaitSingle()
        tokenManager.advertiseIMSUser(newUser)
        imsProject.ims().value.users() += newUser
        return newUser
    }

    /**
     * Find and ensure the IMSIssueTemplate in the database
     * @param imsProject The IMSProject to work with
     * @return the IssueType
     */
    suspend fun issueType(imsProject: IMSProject): IssueType {
        val template = issueTemplate(imsProject)
        val imsProjectConfig = IMSProjectConfig(helper, imsProject)
        if (imsProjectConfig.defaultType != null) {
            val type = neoOperations.findById<IssueType>(imsProjectConfig.defaultType)
            if ((type != null) && (type.partOf().contains(template))) {
                return type
            }
        }
        val imsConfig = IMSConfig(helper, imsProject.ims().value, imsProject.ims().value.template().value)
        if (imsConfig.defaultType != null) {
            val type = neoOperations.findById<IssueType>(imsConfig.defaultType)
            if ((type != null) && (type.partOf().contains(template))) {
                return type
            }
        }
        if (template.issueTypes().isNotEmpty()) {
            return template.issueTypes().first()
        }
        val newIssueType = IssueType("type", "", "")
        newIssueType.partOf() += template
        return neoOperations.findAll(IssueType::class.java).awaitFirstOrNull() ?: neoOperations.save(newIssueType)
            .awaitSingle()
    }

    /**
     * Find and ensure the IMSIssueTemplate in the database
     * @param isOpen whether the state is open or closed
     * @return the IssueState
     */
    suspend fun issueState(imsProject: IMSProject, isOpen: Boolean): IssueState {
        val template = issueTemplate(imsProject)
        val imsProjectConfig = IMSProjectConfig(helper, imsProject)
        val unknownState =
            template.issueStates().filter { imsProjectConfig.labelStateMapper.containsValue(it.rawId!!) }
                .firstOrNull { it.isOpen == isOpen }
        if (unknownState != null) {
            return unknownState
        }
        val firstState = template.issueStates().firstOrNull { it.isOpen == isOpen }
        if (firstState != null) {
            return firstState
        }
        val newIssueState = IssueState(if (isOpen) "open" else "closed", "", isOpen)
        newIssueState.partOf() += template
        return neoOperations.save(newIssueState).awaitSingle()
    }

    /**
     * Map a Label from GitHub to Gropius
     * @param imsProject the Gropius IMSProject to use as input
     * @param labelData the label data to map
     * @return the mapped Label
     */
    suspend fun mapLabel(imsProject: IMSProject, labelData: LabelData): Label? {
        val labelInfo = labelInfoRepository.findByImsProjectAndGithubId(imsProject.rawId!!, labelData.id)
        if (labelInfo != null) {
            return neoOperations.findById<Label>(labelInfo.neo4jId)
        }
        var label = Label(
            labelData.createdAt ?: OffsetDateTime.MIN,
            labelData.createdAt ?: OffsetDateTime.MIN,
            labelData.name,
            "GitHub Label",
            labelData.color
        )
        label.createdBy().value = gropiusUserRepository.findByUsername(FALLBACK_USER_NAME) ?: GropiusUser(
            "GitHub", null, null, FALLBACK_USER_NAME, true
        )
        label.lastModifiedBy().value = label.createdBy().value
        label.trackables() += imsProject.trackable().value
        label = neoOperations.save(label).awaitSingle()
        labelInfoRepository.save(LabelInfo(imsProject.rawId!!, labelData.id, label.rawId!!)).awaitSingle()
        return label
    }

    /**
     * Check windup timer for token
     * @param token the token to check
     * @return true if the token is in timeout
     */
    suspend fun tokenInTimeout(token: String): Boolean {
        if (TOKEN_WOUND_UP_IN_JAIL.contains(token)) {
            return TOKEN_WOUND_UP_IN_JAIL[token]!! > OffsetDateTime.now()
        }
        return false
    }

    /**
     * Windup request blocking timer
     * @param token the token to windup
     * @param seconds the time to windup
     */
    suspend fun windToken(token: String, seconds: Int) {
        TOKEN_WOUND_UP_IN_JAIL[token] = OffsetDateTime.now().plusSeconds(seconds.toLong())
    }

    /**
     * Send a mutation to the IMS
     *
     * @param D The type of the mutation to send
     * @param imsProject The IMSProject to work on
     * @param users The users sorted with best first
     * @param body The content of the mutation
     * @param owner The user that created the data, empty if fetching/other non-owned operations
     * @return The selected user and the response for the mutation
     */
    final suspend inline fun <reified D : Mutation.Data> mutation(
        imsProject: IMSProject, users: List<User>, body: Mutation<D>, owner: List<GropiusUser>
    ): Pair<IMSUser, ApolloResponse<D>> {
        val imsConfig = IMSConfig(helper, imsProject.ims().value, imsProject.ims().value.template().value)
        val userList = users.toMutableList()
        if (imsConfig.readUser != null) {
            val imsUser = neoOperations.findById(imsConfig.readUser, IMSUser::class.java).awaitSingle()
            if (imsUser.ims().value != imsProject.ims().value) {
                TODO("Error handling")
            }
            userList.add(imsUser)
        }
        logger.info("Requesting with users: $userList")
        return tokenManager.executeUntilWorking(imsProject, userList, owner) { token ->
            if (tokenInTimeout(token.token!!)) {
                return@executeUntilWorking Optional.empty()
            }
            val apolloClient = ApolloClient.Builder().serverUrl(imsConfig.graphQLUrl.toString())
                .addHttpHeader("Authorization", "Bearer ${token.token}").build()
            val res = apolloClient.mutation(body).execute()
            logger.info("Response Code for request with token $token is ${res.data} ${res.errors}")
            val headers = res.executionContext[HttpInfo]?.headers
            if ((headers?.firstOrNull { it.name == "x-ratelimit-remaining" }?.value?.toInt() ?: 0) < 100) {
                windToken(token.token!!, 3600)
            }
            if (res.errors?.isNotEmpty() != true) {
                Optional.of(res)
            } else if (res.errors?.all { it.nonStandardFields?.get("type") == "RATE_LIMITED" } == true) {
                windToken(token.token!!, 10800)
                Optional.empty()
            } else {
                throw GitHubResponseException(res.errors!!)
            }
        }
    }

    /**
     * Send a query to the IMS
     *
     * @param D The type of the query to send
     * @param imsProject The IMSProject to work on
     * @param users The users sorted with best first
     * @param body The content of the query
     * @param owner The user that created the data, empty if fetching/other non-owned operations
     * @return The selected user and the response for the query
     */
    final suspend inline fun <reified D : Query.Data> query(
        imsProject: IMSProject, users: List<User>, body: Query<D>
    ): Pair<IMSUser, ApolloResponse<D>> {
        val imsProjectConfig = IMSProjectConfig(helper, imsProject)
        val imsConfig = IMSConfig(helper, imsProject.ims().value, imsProject.ims().value.template().value)
        val userList = users.toMutableList()
        if (imsConfig.readUser != null) {
            val imsUser = neoOperations.findById(imsConfig.readUser, IMSUser::class.java).awaitSingleOrNull()
            if (imsUser == null) {
                TODO("Error handling: Invalid read user")
            }
            if (imsUser.ims().value != imsProject.ims().value) {
                TODO("Error handling")
            }
            userList.add(imsUser)
        }
        logger.info("Requesting with users: $userList ")
        return tokenManager.executeUntilWorking(imsProject, userList, listOf()) { token ->
            if (tokenInTimeout(token.token!!)) {
                return@executeUntilWorking Optional.empty()
            }
            val apolloClient = ApolloClient.Builder().serverUrl(imsConfig.graphQLUrl.toString())
                .addHttpHeader("Authorization", "Bearer ${token.token}").build()
            val res = apolloClient.query(body).execute()
            logger.info("Response Code for request with token $token is ${res.data} ${res.errors}")
            val headers = res.executionContext[HttpInfo]?.headers
            if ((headers?.firstOrNull { it.name == "x-ratelimit-remaining" }?.value?.toInt() ?: 0) < 100) {
                windToken(token.token!!, 3600)
            }
            if (res.errors?.isNotEmpty() != true) {
                Optional.of(res)
            } else if (res.errors?.all { it.nonStandardFields?.get("type") == "RATE_LIMITED" } == true) {
                windToken(token.token!!, 10800)
                Optional.empty()
            } else {
                throw GitHubResponseException(res.errors!!)
            }
        }
    }
}