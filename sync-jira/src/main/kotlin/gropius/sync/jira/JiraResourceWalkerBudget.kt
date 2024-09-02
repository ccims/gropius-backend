package gropius.sync.jira

import gropius.sync.ResourceWalkerBudget

/**
 * A budget for the Jira resource walker.
 */
class JiraGithubResourceWalkerBudgetUsageType {}

/**
 * An estimated budget for the Jira resource walker.

 */
class JiraGithubResourceWalkerEstimatedBudgetUsageType {}

/**
 * A budget for the Jira resource walker.
 */
class JiraResourceWalkerBudget :
    ResourceWalkerBudget<JiraGithubResourceWalkerBudgetUsageType, JiraGithubResourceWalkerEstimatedBudgetUsageType> {
    override suspend fun integrate(usage: JiraGithubResourceWalkerBudgetUsageType) {}
    override suspend fun mayExecute(expectedUsage: JiraGithubResourceWalkerEstimatedBudgetUsageType): Boolean {
        return true;
    }
}