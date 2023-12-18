package gropius.sync.github

import gropius.sync.CursorResourceWalkerConfig

/**
 * Configuration for GitHub load balancer
 * @param resourceWalkerConfig Resource walker configuration
 * @param remoteOwner Remote owner
 * @param remoteRepo Remote repository
 * @param count Number of resources to load
 */
data class GitHubResourceWalkerConfig(
    val resourceWalkerConfig: CursorResourceWalkerConfig<GithubGithubResourceWalkerBudgetUsageType, GithubGithubResourceWalkerEstimatedBudgetUsageType>,
    val remoteOwner: String,
    val remoteRepo: String,
    val count: Int
) {}