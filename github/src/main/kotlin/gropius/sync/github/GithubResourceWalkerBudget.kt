package gropius.sync.github

import gropius.sync.ResourceWalkerBudget

class GithubGithubResourceWalkerBudgetUsageType {}
class GithubGithubResourceWalkerEstimatedBudgetUsageType {}

class GithubResourceWalkerBudget :
    ResourceWalkerBudget<GithubGithubResourceWalkerBudgetUsageType, GithubGithubResourceWalkerEstimatedBudgetUsageType> {
    override suspend fun integrate(usage: GithubGithubResourceWalkerBudgetUsageType) {}
    override suspend fun mayExecute(expectedUsage: GithubGithubResourceWalkerEstimatedBudgetUsageType): Boolean {
        return true;
    }
}