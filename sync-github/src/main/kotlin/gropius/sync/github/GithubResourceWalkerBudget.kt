package gropius.sync.github

import gropius.sync.ResourceWalkerBudget

/**
 * GitHub placeholder usage type.
 */
class GithubGithubResourceWalkerBudgetUsageType {}

/**
 * GitHub placeholder estimated usage type.
 */
class GithubGithubResourceWalkerEstimatedBudgetUsageType {}

/**
 * GitHub resource walker budget.
 */
class GithubResourceWalkerBudget :
    ResourceWalkerBudget<GithubGithubResourceWalkerBudgetUsageType, GithubGithubResourceWalkerEstimatedBudgetUsageType> {
    override suspend fun integrate(usage: GithubGithubResourceWalkerBudgetUsageType) {}
    override suspend fun mayExecute(expectedUsage: GithubGithubResourceWalkerEstimatedBudgetUsageType): Boolean {
        return true;
    }
}