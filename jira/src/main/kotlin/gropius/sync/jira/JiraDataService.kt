package gropius.sync.jira

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
import gropius.sync.jira.config.IMSConfig
import gropius.util.JsonNodeMapper
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.neo4j.core.ReactiveNeo4jOperations
import org.springframework.stereotype.Component
import java.net.URI
import java.time.OffsetDateTime
import java.util.*

/**
 * Context for Jira Operations
 * @param neoOperations Neo4jOperations to access the database
 * @param tokenManager Reference for the spring instance of JiraTokenManager
 * @param helper Reference for the spring instance of JsonHelper
 * @param objectMapper Reference for the spring instance of ObjectMapper
 * @param jsonNodeMapper Reference for the spring instance of JsonNodeMapper
 * @param gropiusUserRepository Reference for the spring instance of GropiusUserRepository
 */
@Component
class JiraDataService(
    @Qualifier("graphglueNeo4jOperations")
    val neoOperations: ReactiveNeo4jOperations,
    val tokenManager: JiraTokenManager,
    val helper: JsonHelper,
    val objectMapper: ObjectMapper,
    val jsonNodeMapper: JsonNodeMapper,
    val gropiusUserRepository: GropiusUserRepository
) : SyncDataService {

    /**
     * Logger used to print notifications
     */
    final val logger = LoggerFactory.getLogger(JiraDataService::class.java)

    /**
     * Http Client for Jira API requests
     */
    val client = HttpClient() {
        expectSuccess = true
        install(Logging)
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            }, contentType = ContentType.parse("application/json; charset=utf-8"))
        }
    }

    /**
     * Get the default issue template
     * @return the default issue template
     */
    suspend fun issueTemplate(): IssueTemplate {
        val newTemplate = IssueTemplate("noissue", "", mutableMapOf(), false)
        newTemplate.issueStates() += IssueState("open", "", true)
        newTemplate.issueStates() += IssueState("closed", "", false)
        for (t in newTemplate.issueStates()) t.partOf() += newTemplate
        return neoOperations.findAll(IssueTemplate::class.java).awaitFirstOrNull() ?: neoOperations.save(
            newTemplate
        ).awaitSingle()
    }

    /**
     * Get the default issue type
     * @return the default issue type
     */
    suspend fun issueType(): IssueType {
        val newIssueType = IssueType("type", "", "")
        newIssueType.partOf() += issueTemplate()
        return neoOperations.findAll(IssueType::class.java).awaitFirstOrNull() ?: neoOperations.save(newIssueType)
            .awaitSingle()
    }

    /**
     * Get the default issue state
     * @param isOpen whether the issue state is open or closed
     * @return the default issue state
     */
    suspend fun issueState(isOpen: Boolean): IssueState {
        val newIssueState = IssueState(if (isOpen) "open" else "closed", "", isOpen)
        newIssueState.partOf() += issueTemplate()
        return neoOperations.findAll(IssueState::class.java).filter { it.isOpen == isOpen }.awaitFirstOrNull()
            ?: neoOperations.save(newIssueState).awaitSingle()
    }

    /**
     * Get a IMSUser for a Jira user
     * @param imsProject the project to map the user to
     * @param user the Jira user
     */
    suspend fun mapUser(imsProject: IMSProject, user: JsonElement): User {
        val encodedAccountId =
            jsonNodeMapper.jsonNodeToDeterministicString(objectMapper.valueToTree<JsonNode>(user.jsonObject["accountId"]!!.jsonPrimitive.content))
        val foundImsUser =
            imsProject.ims().value.users().firstOrNull { it.templatedFields["jira_id"] == encodedAccountId }
        if (foundImsUser != null) {
            return foundImsUser
        }
        val imsUser = IMSUser(
            user.jsonObject["displayName"]?.jsonPrimitive?.content
                ?: user.jsonObject["emailAddress"]?.jsonPrimitive?.content ?: "Unnamed User",
            user.jsonObject["emailAddress"]?.jsonPrimitive?.content,
            null,
            user.jsonObject["emailAddress"]?.jsonPrimitive?.content,
            mutableMapOf("jira_id" to encodedAccountId)
        )
        imsUser.ims().value = imsProject.ims().value
        imsUser.template().value = imsUser.ims().value.template().value.imsUserTemplate().value
        val newUser = neoOperations.save(imsUser).awaitSingle()
        imsProject.ims().value.users() += newUser
        return newUser
    }

    /**
     * Map a label from Jira to Gropius
     * @param imsProject the project to map the label to
     * @param label the label to map
     */
    suspend fun mapLabel(imsProject: IMSProject, label: String): Label {
        val trackable = imsProject.trackable().value
        val labels = trackable.labels().filter { it.name == label }
        return if (labels.isEmpty()) {
            val label = Label(OffsetDateTime.now(), OffsetDateTime.now(), label, "Jira Label", "000000")
            label.createdBy().value =
                gropiusUserRepository.findByUsername("jira") ?: GropiusUser("Jira", null, null, "jira", true)
            label.lastModifiedBy().value = label.createdBy().value
            label.trackables() += trackable
            //neoOperations.save(label).awaitSingle() //TODO: Cache created labels on per transaction basis
            label
        } else if (labels.size == 1) {
            labels.single()
        } else {
            labels.first()
        }
    }

    /**
     * Process the request for a single token
     *
     * @param imsProject the project to work on
     * @param requestMethod The HTTP method to use
     * @param body the body to send
     * @param urlBuilder the URL to send the request to
     * @param token the token to use
     *
     * @return an optional containing the response or empty if the request failed
     */
    final suspend inline fun <reified T> sendRequest(
        imsProject: IMSProject,
        requestMethod: HttpMethod,
        body: T? = null,
        crossinline urlBuilder: URLBuilder .(URLBuilder) -> Unit,
        token: JiraTokenResponse
    ): Optional<HttpResponse> {
        val imsConfig = IMSConfig(helper, imsProject.ims().value, imsProject.ims().value.template().value)
        val cloudId =
            token.cloudIds?.filter { URI(it.url + "/rest/api/2") == URI(imsConfig.rootUrl.toString()) }?.map { it.id }
                ?.firstOrNull()
        if (cloudId != null) {
            try {
                val res = client.request("https://api.atlassian.com/ex/jira/") {
                    method = requestMethod
                    url {
                        appendPathSegments(cloudId)
                        appendPathSegments("/rest/api/2/")
                        urlBuilder(this)
                    }
                    headers {
                        append(
                            HttpHeaders.Authorization, "Bearer ${token.token}"
                        )
                    }
                    if (body != null) {
                        contentType(ContentType.parse("application/json; charset=utf-8"))
                        setBody(body)
                    }
                }
                logger.info("Response Code for request with token token is ${res.status}(${res.status.isSuccess()}): $body is ${res.bodyAsText()}")
                return if (res.status.isSuccess()) {
                    logger.trace("Response for {} {}", res.request.url, res.bodyAsText())
                    Optional.of(res)
                } else {
                    Optional.empty()
                }
            } catch (e: ClientRequestException) {
                e.printStackTrace()
                return Optional.empty()
            }
        } else {
            return Optional.empty()
        }
    }

    /**
     * Process a request for a given set of users
     *
     * @param imsProject the project to work on
     * @param users the list of users to process, sorted with best first
     * @param requestMethod The HTTP method to use
     * @param body the body to send
     * @param urlBuilder the URL to send the request to
     *
     * @return an optional containing the response or empty if the request failed
     */
    final suspend inline fun <reified T> request(
        imsProject: IMSProject,
        users: List<User>,
        requestMethod: HttpMethod,
        body: T? = null,
        crossinline urlBuilder: URLBuilder .(URLBuilder) -> Unit
    ): Pair<IMSUser, HttpResponse> {
        val imsConfig = IMSConfig(helper, imsProject.ims().value, imsProject.ims().value.template().value)
        val rawUserList = users.toMutableList()
        if (imsConfig.readUser != null) {
            val imsUser = neoOperations.findById(imsConfig.readUser, IMSUser::class.java).awaitSingleOrNull()
                ?: throw IllegalArgumentException("Read user not found")
            if (imsUser.ims().value != imsProject.ims().value) {
                TODO("Error handling")
            }
            rawUserList.add(imsUser)
        }
        val userList = rawUserList.distinct()
        logger.info("Requesting with users: $userList")
        return tokenManager.executeUntilWorking(imsProject.ims().value, userList) {
            sendRequest<T>(
                imsProject, requestMethod, body, urlBuilder, it
            )
        }
    }
}