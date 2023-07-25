package gropius.sync.github

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.exception.ApolloException
import gropius.model.architecture.IMSProject
import gropius.sync.CursorResourceWalker
import gropius.sync.CursorResourceWalkerDataService
import gropius.sync.github.generated.CommentReadQuery
import gropius.sync.github.generated.CommentReadQuery.Data.Node.Companion.asIssueComment
import kotlinx.coroutines.reactor.awaitSingle
import org.bson.types.ObjectId

class CommentWalker(
    imsProject: IMSProject,
    val issue: ObjectId,
    val comment: String,
    val config: GitHubResourceWalkerConfig,
    budget: GithubResourceWalkerBudget,
    val apolloClient: ApolloClient,
    val issuePileService: IssuePileService,
    cursorResourceWalkerDataService: CursorResourceWalkerDataService
) : CursorResourceWalker<GithubGithubResourceWalkerBudgetUsageType, GithubGithubResourceWalkerEstimatedBudgetUsageType, GithubResourceWalkerBudget>(
    imsProject, issue.toHexString(), config.resourceWalkerConfig, budget, cursorResourceWalkerDataService
) {
    override suspend fun execute(): GithubGithubResourceWalkerBudgetUsageType {
        println("EXECUTE CommentWalker")
        try {
            val issuePile = issuePileService.findById(issue).awaitSingle()
            val query = CommentReadQuery(
                comment = comment
            )
            val response = apolloClient.query(query).execute()
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
                val ic = response.data?.node?.asIssueComment()!!
                issuePileService.markCommentDone(issue, comment, ic.updatedAt, ic.body)
            }
        } catch (e: ApolloException) {
            e.printStackTrace()
        }
        return GithubGithubResourceWalkerBudgetUsageType();
    }
}
