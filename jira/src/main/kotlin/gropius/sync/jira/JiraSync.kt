package gropius.sync.jira

//import gropius.sync.github.config.IMSConfigManager
//import gropius.sync.github.config.IMSProjectConfig
import gropius.model.architecture.IMSProject
import gropius.model.issue.timeline.IssueComment
import gropius.model.template.IMSTemplate
import gropius.sync.*
import gropius.sync.jira.config.IMSConfigManager
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

    override suspend fun fetchData(imsProjects: List<IMSProject>) {
        for (imsProject in imsProjects) {
            val q = client.get("https://itscalledccims.atlassian.net/rest/api/2/") {
                url {
                    appendPathSegments("search")
                    parameters.append("jql", "project=FUCK")
                    parameters.append("expand", "names,schema,editmeta,changelog")
                }
                headers {
                    append(
                        HttpHeaders.Authorization,
                        "Basic Y2hyaWt1dmVsbGJlcmdAZ21haWwuY29tOkFUQVRUM3hGZkdGMFBZcWhIcUlCU25kaTk3NFg0N2Zv" + "NmRIVHRFbjF4eVJtS2o4REJIbUtBLTZrS0pNNHBXYkJvS0p4SFN1Z09mSWtocVJIdkdZazUwT0RX" + "cmMxRmZJN2VNNExNdEd1SExpZDctcllHTDRqNk5ZRnFFd01CVm82TnI5dTB0NC0ySXlfNXlOQXk3" + "QWxuTFJ4SzliM3hEWFZpLXNDSmZWTjAwanBTWC1icURmRUNNMD0zNDNBOEJCQQ=="
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