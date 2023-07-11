package gropius.sync.github

import com.apollographql.apollo3.ApolloClient
import gropius.sync.CursorResourceWalker
import gropius.sync.CursorResourceWalkerDataService
import gropius.sync.github.generated.IssueReadQuery

class IssueWalker(
    imsProject: String,
    val config: GitHubResourceWalkerConfig,
    budget: GithubResourceWalkerBudget,
    val apolloClient: ApolloClient,
    cursorResourceWalkerDataService: CursorResourceWalkerDataService
) : CursorResourceWalker<GithubGithubResourceWalkerBudgetUsageType, GithubGithubResourceWalkerEstimatedBudgetUsageType, GithubResourceWalkerBudget>(
    imsProject, imsProject, config.resourceWalkerConfig, budget, cursorResourceWalkerDataService
) {
    override suspend fun execute(): GithubGithubResourceWalkerBudgetUsageType {
        val query = IssueReadQuery(
            repoOwner = config.remoteOwner,
            repoName = config.remoteRepo,
            since = null,
            cursor = null,
            issueCount = config.count
        )
        val response = apolloClient.query(query).execute()
        println(response)

        return GithubGithubResourceWalkerBudgetUsageType();
    }
}