package gropius.sync.jira

import gropius.sync.ResourceWalkerBudget

class JiraGithubResourceWalkerBudgetUsageType {}
class JiraGithubResourceWalkerEstimatedBudgetUsageType {}

class JiraResourceWalkerBudget :
    ResourceWalkerBudget<JiraGithubResourceWalkerBudgetUsageType, JiraGithubResourceWalkerEstimatedBudgetUsageType> {
    override suspend fun integrate(usage: JiraGithubResourceWalkerBudgetUsageType) {}
    override suspend fun mayExecute(expectedUsage: JiraGithubResourceWalkerEstimatedBudgetUsageType): Boolean {
        return true;
    }
}