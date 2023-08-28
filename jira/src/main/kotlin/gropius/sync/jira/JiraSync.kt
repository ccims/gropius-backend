package gropius.sync.jira

//import gropius.sync.github.config.IMSConfigManager
//import gropius.sync.github.config.IMSProjectConfig
import gropius.model.architecture.IMSProject
import gropius.model.issue.timeline.IssueComment
import gropius.model.template.IMSTemplate
import gropius.sync.*
import gropius.sync.jira.config.IMSConfigManager
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
final class JiraSync(
    val jiraDataService: JiraDataService,
    val helper: JsonHelper,
    val cursorResourceWalkerDataService: CursorResourceWalkerDataService,
    val imsConfigManager: IMSConfigManager,
    collectedSyncInfo: CollectedSyncInfo,
    val loadBalancedDataFetcher: LoadBalancedDataFetcher = LoadBalancedDataFetcher()
) : AbstractSync(collectedSyncInfo), LoadBalancedDataFetcherImplementation, DataFetcher by loadBalancedDataFetcher {

    init {
        loadBalancedDataFetcher.start(this)
    }

    /**
     * Logger used to print notifications
     */
    private val logger = LoggerFactory.getLogger(JiraSync::class.java)

    override suspend fun createBudget(): GeneralResourceWalkerBudget {
        return GithubResourceWalkerBudget()
    }

    override fun syncDataService(): SyncDataService {
        return jiraDataService
    }

    override suspend fun findTemplates(): Set<IMSTemplate> {
        return imsConfigManager.findTemplates()
    }

    override suspend fun balancedFetchData(
        imsProject: IMSProject, generalBudget: GeneralResourceWalkerBudget
    ): List<ResourceWalker> {
        /*val imsProjectConfig = IMSProjectConfig(helper, imsProject)
        val budget = generalBudget as GithubResourceWalkerBudget

        val walkers = mutableListOf<ResourceWalker>()
        walkers += IssueWalker(
            imsProject, GitHubResourceWalkerConfig(
                CursorResourceWalkerConfig<GithubGithubResourceWalkerBudgetUsageType, GithubGithubResourceWalkerEstimatedBudgetUsageType>(
                    10.0,
                    1.0,
                    GithubGithubResourceWalkerEstimatedBudgetUsageType(),
                    GithubGithubResourceWalkerBudgetUsageType()
                ), imsProjectConfig.repo.owner, imsProjectConfig.repo.repo, 100
            ), budget, apolloClient, issuePileService, cursorResourceWalkerDataService
        )

        walkers += issuePileService.findByImsProjectAndNeedsTimelineRequest(
            imsProject.rawId!!, true
        ).map {
            TimelineWalker(
                imsProject, it.id!!, GitHubResourceWalkerConfig(
                    CursorResourceWalkerConfig<GithubGithubResourceWalkerBudgetUsageType, GithubGithubResourceWalkerEstimatedBudgetUsageType>(
                        1.0,
                        0.1,
                        GithubGithubResourceWalkerEstimatedBudgetUsageType(),
                        GithubGithubResourceWalkerBudgetUsageType()
                    ), imsProjectConfig.repo.owner, imsProjectConfig.repo.repo, 100
                ), budget, apolloClient, issuePileService, cursorResourceWalkerDataService
            )
        }
        for (dirtyIssue in issuePileService.findByImsProjectAndNeedsCommentRequest(
            imsProject.rawId!!, true
        )) {
            for (comment in dirtyIssue.timelineItems.mapNotNull { it as? IssueCommentTimelineItem }) {
                walkers += CommentWalker(
                    imsProject, dirtyIssue.id!!, comment.githubId, GitHubResourceWalkerConfig(
                        CursorResourceWalkerConfig<GithubGithubResourceWalkerBudgetUsageType, GithubGithubResourceWalkerEstimatedBudgetUsageType>(
                            1.0,
                            0.1,
                            GithubGithubResourceWalkerEstimatedBudgetUsageType(),
                            GithubGithubResourceWalkerBudgetUsageType()
                        ), imsProjectConfig.repo.owner, imsProjectConfig.repo.repo, 100
                    ), budget, apolloClient, issuePileService, cursorResourceWalkerDataService
                )
            }
        }
        return walkers*/
        TODO()
    }

    override suspend fun findUnsyncedIssues(imsProject: IMSProject): List<IncomingIssue> {
        //return issuePileService.findByImsProjectAndHasUnsyncedData(imsProject.rawId!!, true)
        TODO()
    }

    override suspend fun syncComment(
        imsProject: IMSProject, issueId: String, issueComment: IssueComment
    ): TimelineItemConversionInformation? {
        TODO()
        /*val response = apolloClient.mutation(MutateCreateCommentMutation(issueId, issueComment.body)).execute()
        val item = response.data?.addComment?.commentEdge?.node?.asIssueTimelineItems()
        if (item != null) {
            return TODOTimelineItemConversionInformation(imsProject.rawId!!, item.id)
        }
        TODO("ERROR HANDLING")
        return null*/
    }
}