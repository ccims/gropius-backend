package gropius.sync.github

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.exception.ApolloException
import gropius.model.architecture.IMSProject
import gropius.sync.CursorResourceWalker
import gropius.sync.CursorResourceWalkerDataService
import gropius.sync.github.generated.TimelineReadQuery
import gropius.sync.github.generated.TimelineReadQuery.Data.Node.Companion.asIssue
import kotlinx.coroutines.reactor.awaitSingle
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory

/**
 * A walker that walks over all issues of a github repository
 * @param imsProject the ims project to walk over
 * @param issue the issue to walk over
 * @param config the config of the walker
 * @param budget the budget of the walker
 * @param apolloClient the apollo client to use for the walker
 * @param issuePileService the issue pile service to use for the walker
 * @param cursorResourceWalkerDataService the data service to use for the walker
 */
class TimelineWalker(
    imsProject: IMSProject,
    val issue: ObjectId,
    val config: GitHubResourceWalkerConfig,
    budget: GithubResourceWalkerBudget,
    val apolloClient: ApolloClient,
    val issuePileService: IssuePileService,
    cursorResourceWalkerDataService: CursorResourceWalkerDataService
) : CursorResourceWalker<GithubGithubResourceWalkerBudgetUsageType, GithubGithubResourceWalkerEstimatedBudgetUsageType, GithubResourceWalkerBudget>(
    imsProject, issue.toHexString(), config.resourceWalkerConfig, budget, cursorResourceWalkerDataService
) {
    /**
     * Logger used to print notifications
     */
    private val logger = LoggerFactory.getLogger(TimelineWalker::class.java)

    override suspend fun execute(): GithubGithubResourceWalkerBudgetUsageType {
        logger.info("EXECUTE TimelineWalker")
        try {
            val issuePile = issuePileService.findById(issue).awaitSingle()
            val since = issuePile?.timelineItems?.maxOfOrNull { it.createdAt }
            var cursor: String? = null
            do {
                val query = TimelineReadQuery(
                    issue = issuePile.githubId, since = since, cursor = cursor, issueCount = config.count
                )
                val response = apolloClient.query(query).execute()
                cursor =
                    if (response.data?.node?.asIssue()?.timelineItems?.pageInfo?.hasNextPage == true) {
                        response.data?.node?.asIssue()?.timelineItems?.pageInfo?.endCursor
                    } else null;
                var isRateLimited = false
                response.errors?.forEach {
                    if (it.nonStandardFields?.get("type") == "RATE_LIMITED") {
                        isRateLimited = true;
                    }
                };
                if (isRateLimited) {
                    return GithubGithubResourceWalkerBudgetUsageType()//TODO: rate limit max err
                }
                if (response.errors?.isEmpty() != false) {
                    response.data?.node?.asIssue()?.timelineItems?.nodes?.forEach {
                        issuePileService.integrateTimelineItem(
                            issue, it!!
                        )
                    }
                }
            } while (cursor != null);
            issuePileService.markIssueTimelineDone(issue)
        } catch (e: ApolloException) {
            e.printStackTrace()
        }
        return GithubGithubResourceWalkerBudgetUsageType();
    }
}
