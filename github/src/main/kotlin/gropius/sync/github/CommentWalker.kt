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
import org.slf4j.LoggerFactory

/**
 * Walker for a single comment
 * @param imsProject the Gropius IMSProject to use as input
 * @param issue the issue to comment on
 * @param comment the comment to write
 * @param config the config of the parent IMS
 * @param budget the budget to use
 * @param apolloClient the apollo client to use
 * @param issuePileService the issue pile service to use
 * @param cursorResourceWalkerDataService the data service to use
 */
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
    /**
     * Logger used to print notifications
     */
    private val logger = LoggerFactory.getLogger(CommentWalker::class.java)

    override suspend fun execute(): GithubGithubResourceWalkerBudgetUsageType {
        logger.info("EXECUTE CommentWalker")
        try {
            val issuePile = issuePileService.findById(issue).awaitSingle()
            val query = CommentReadQuery(
                comment = comment
            )
            val response = apolloClient.query(query).execute()
            val isRateLimited = response.errors?.any {
                it.nonStandardFields?.get("type") == "RATE_LIMITED"
            } ?: false
            if (isRateLimited) {
                return GithubGithubResourceWalkerBudgetUsageType()//TODO: rate limit max err
            }
            if (response.errors.isNullOrEmpty()) {
                val ic = response.data?.node?.asIssueComment()!!
                issuePileService.markCommentDone(issue, comment, ic.updatedAt, ic.body)
            }
        } catch (e: ApolloException) {
            e.printStackTrace()
        }
        return GithubGithubResourceWalkerBudgetUsageType();
    }
}
