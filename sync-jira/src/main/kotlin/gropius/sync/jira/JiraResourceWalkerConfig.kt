package gropius.sync.jira

import gropius.sync.CursorResourceWalkerConfig

/**
 * Condig for Jira Resozrce Walkers
 * @param resourceWalkerConfig the config for the resource walker
 * @param remoteOwner the owner of the remote repository
 * @param remoteRepo the name of the remote repository
 * @param count the number of issues to fetch
 */
data class JiraResourceWalkerConfig(
    val resourceWalkerConfig: CursorResourceWalkerConfig<JiraGithubResourceWalkerBudgetUsageType, JiraGithubResourceWalkerEstimatedBudgetUsageType>,
    val remoteOwner: String,
    val remoteRepo: String,
    val count: Int
) {}