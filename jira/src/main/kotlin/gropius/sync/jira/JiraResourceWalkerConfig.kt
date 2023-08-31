package gropius.sync.jira

import gropius.sync.CursorResourceWalkerConfig

data class JiraResourceWalkerConfig(
    val resourceWalkerConfig: CursorResourceWalkerConfig<JiraGithubResourceWalkerBudgetUsageType, JiraGithubResourceWalkerEstimatedBudgetUsageType>,
    val remoteOwner: String,
    val remoteRepo: String,
    val count: Int
) {}