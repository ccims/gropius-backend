package gropius.sync.github

import com.apollographql.apollo3.ApolloClient
import gropius.model.architecture.IMSProject
import gropius.sync.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.URI

@Component
final class GithubSync(
    val githubDataService: GithubDataService,
    val issuePileService: IssuePileService,
    val cursorResourceWalkerDataService: CursorResourceWalkerDataService,
    collectedSyncInfo: CollectedSyncInfo,
    val loadBalancedDataFetcher: LoadBalancedDataFetcher = LoadBalancedDataFetcher()
) : AbstractSync(collectedSyncInfo), LoadBalancedDataFetcherImplementation, DataFetcher by loadBalancedDataFetcher {

    init {
        loadBalancedDataFetcher.start(this)
    }

    val apolloClient = ApolloClient.Builder().serverUrl(URI("https://api.github.com/graphql").toString())
        .addHttpHeader("Authorization", "bearer " + System.getenv("GITHUB_DUMMY_PAT")).build()

    /**
     * Logger used to print notifications
     */
    private val logger = LoggerFactory.getLogger(SyncSelector::class.java)

    override suspend fun createBudget(): GeneralResourceWalkerBudget {
        return GithubResourceWalkerBudget()
    }

    override fun syncDataService(): SyncDataService {
        return githubDataService
    }

    override suspend fun balancedFetchData(
        imsProject: IMSProject, generalBudget: GeneralResourceWalkerBudget
    ): List<ResourceWalker> {
        val budget = generalBudget as GithubResourceWalkerBudget

        val walkers = mutableListOf<ResourceWalker>()
        walkers += IssueWalker(
            imsProject, GitHubResourceWalkerConfig(
                CursorResourceWalkerConfig<GithubGithubResourceWalkerBudgetUsageType, GithubGithubResourceWalkerEstimatedBudgetUsageType>(
                    10.0,
                    1.0,
                    GithubGithubResourceWalkerEstimatedBudgetUsageType(),
                    GithubGithubResourceWalkerBudgetUsageType()
                ), "terralang", "terra", 100
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
                    ), "terralang", "terra", 100
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
                        ), "terralang", "terra", 100
                    ), budget, apolloClient, issuePileService, cursorResourceWalkerDataService
                )
            }
        }
        return walkers
    }

    override suspend fun findUnsyncedIssues(imsProject: IMSProject): List<IncomingIssue> {
        return issuePileService.findByImsProjectAndHasUnsyncedData(imsProject.rawId!!, true)
    }
}