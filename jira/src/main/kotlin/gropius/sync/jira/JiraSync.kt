package gropius.sync.jira

//import gropius.sync.github.config.IMSConfigManager
//import gropius.sync.github.config.IMSProjectConfig
import gropius.model.architecture.IMSProject
import gropius.model.issue.timeline.IssueComment
import gropius.model.template.IMSTemplate
import gropius.sync.*
import gropius.sync.jira.config.IMSConfig
import gropius.sync.jira.config.IMSConfigManager
import gropius.sync.jira.config.IMSProjectConfig
import gropius.sync.jira.model.IssueDataService
import gropius.sync.jira.model.ProjectQuery
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Component
final class JiraSync(
    val jiraDataService: JiraDataService,
    val helper: JsonHelper,
    val cursorResourceWalkerDataService: CursorResourceWalkerDataService,
    val imsConfigManager: IMSConfigManager,
    collectedSyncInfo: CollectedSyncInfo,
    val loadBalancedDataFetcher: LoadBalancedDataFetcher = LoadBalancedDataFetcher(),
    val issueDataService: IssueDataService
) : AbstractSync(collectedSyncInfo) {

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
     * Logger used to print notifications
     */
    private val logger = LoggerFactory.getLogger(JiraSync::class.java)

    override fun syncDataService(): SyncDataService {
        return jiraDataService
    }

    override suspend fun findTemplates(): Set<IMSTemplate> {
        return imsConfigManager.findTemplates()
    }

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun fetchData(imsProjects: List<IMSProject>) {
        for (imsProject in imsProjects) {
            val imsProjectConfig = IMSProjectConfig(helper, imsProject)
            val imsConfig = IMSConfig(helper, imsProject.ims().value, imsProject.ims().value.template().value)
            val basicContent: String = System.getenv("JIRA_DUMMY_EMAIL") + ":" + System.getenv("JIRA_DUMMY_TOKEN")
            val basicToken = Base64.encode(basicContent.toByteArray())
            val q = client.get(imsConfig.rootUrl.toString()) {
                url {
                    appendPathSegments("search")
                    parameters.append("jql", "project=FUCK")
                    parameters.append("expand", "names,schema,editmeta,changelog")
                }
                headers {
                    append(
                        HttpHeaders.Authorization, "Basic ${basicToken}"
                    )
                }
            }.body<ProjectQuery>()
            q.issues[0].fields.forEach({ println("${it.key}: ${it.value}") })
            q.issues(imsProject).forEach {
                println(it)
                issueDataService.insertIssue(imsProject, it)
            }
        }
    }

    override suspend fun findUnsyncedIssues(imsProject: IMSProject): List<IncomingIssue> {
        return issueDataService.findByImsProject(imsProject.rawId!!)
    }

    override suspend fun syncComment(
        imsProject: IMSProject, issueId: String, issueComment: IssueComment
    ): TimelineItemConversionInformation? {
        TODO()/*val response = apolloClient.mutation(MutateCreateCommentMutation(issueId, issueComment.body)).execute()
        val item = response.data?.addComment?.commentEdge?.node?.asIssueTimelineItems()
        if (item != null) {
            return TODOTimelineItemConversionInformation(imsProject.rawId!!, item.id)
        }
        TODO("ERROR HANDLING")
        return null*/
    }
}