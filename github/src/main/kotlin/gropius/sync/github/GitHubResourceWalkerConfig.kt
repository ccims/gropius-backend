package gropius.sync.github

import gropius.sync.CursorResourceWalkerConfig

data class GitHubResourceWalkerConfig(
    val resourceWalkerConfig: CursorResourceWalkerConfig<GithubGithubResourceWalkerBudgetUsageType, GithubGithubResourceWalkerEstimatedBudgetUsageType>,
    val remoteOwner: String,
    val remoteRepo: String,
    val count: Int
) {}