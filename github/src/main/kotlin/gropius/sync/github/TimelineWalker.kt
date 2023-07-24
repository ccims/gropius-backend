package gropius.sync.github

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.exception.ApolloException
import gropius.sync.CursorResourceWalker
import gropius.sync.CursorResourceWalkerDataService
import gropius.sync.github.generated.TimelineReadQuery
import gropius.sync.github.generated.TimelineReadQuery.Data.Node.Companion.asIssue
import kotlinx.coroutines.reactor.awaitSingle
import org.bson.types.ObjectId

class TimelineWalker(
    imsProject: String,
    val issue: ObjectId,
    val config: GitHubResourceWalkerConfig,
    budget: GithubResourceWalkerBudget,
    val apolloClient: ApolloClient,
    val issuePileService: IssuePileService,
    cursorResourceWalkerDataService: CursorResourceWalkerDataService
) : CursorResourceWalker<GithubGithubResourceWalkerBudgetUsageType, GithubGithubResourceWalkerEstimatedBudgetUsageType, GithubResourceWalkerBudget>(
    imsProject, imsProject, config.resourceWalkerConfig, budget, cursorResourceWalkerDataService
) {
    override suspend fun execute(): GithubGithubResourceWalkerBudgetUsageType {
        println("EXECUTE TimelineWalker")
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
                    if (response.data?.node?.asIssue()?.timelineItems?.pageInfo?.hasNextPage == true) response.data?.node?.asIssue()?.timelineItems?.pageInfo?.endCursor
                    else null;
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
