package gropius.sync.github

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.exception.ApolloException
import gropius.model.architecture.IMSProject
import gropius.sync.CursorResourceWalker
import gropius.sync.CursorResourceWalkerDataService
import gropius.sync.github.generated.IssueReadQuery

class IssueWalker(
    imsProject: IMSProject,
    val config: GitHubResourceWalkerConfig,
    budget: GithubResourceWalkerBudget,
    val apolloClient: ApolloClient,
    val issuePileService: IssuePileService,
    cursorResourceWalkerDataService: CursorResourceWalkerDataService
) : CursorResourceWalker<GithubGithubResourceWalkerBudgetUsageType, GithubGithubResourceWalkerEstimatedBudgetUsageType, GithubResourceWalkerBudget>(
    imsProject, imsProject.rawId!!, config.resourceWalkerConfig, budget, cursorResourceWalkerDataService
) {
    override suspend fun execute(): GithubGithubResourceWalkerBudgetUsageType {
        try {
            val newestIssue = issuePileService.findFirstByImsProjectOrderByLastUpdateDesc(imsProject.rawId!!)
            val since = newestIssue?.lastUpdate
            var cursor: String? = null
            do {
                val query = IssueReadQuery(
                    repoOwner = config.remoteOwner,
                    repoName = config.remoteRepo,
                    since = since,
                    cursor = cursor,
                    issueCount = config.count
                )
                val response = apolloClient.query(query).execute()
                cursor =
                    if (response.data?.repository?.issues?.pageInfo?.hasNextPage == true) response.data?.repository?.issues?.pageInfo?.endCursor
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
                    response.data?.repository?.issues?.nodes?.forEach {
                        issuePileService.integrateIssue(
                            imsProject, it!!
                        )
                    }
                }
            } while (cursor != null);
        } catch (e: ApolloException) {
            e.printStackTrace()
        }
        return GithubGithubResourceWalkerBudgetUsageType();
    }
}

