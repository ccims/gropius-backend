package gropius.sync.github

import gropius.sync.CursorResourceWalker
import gropius.sync.CursorResourceWalkerConfig
import gropius.sync.CursorResourceWalkerDataService

class IssueWalker(
    imsProject: String,
    config: CursorResourceWalkerConfig<GithubGithubResourceWalkerBudgetUsageType, GithubGithubResourceWalkerEstimatedBudgetUsageType>,
    budget: GithubResourceWalkerBudget,
    cursorResourceWalkerDataService: CursorResourceWalkerDataService
) : CursorResourceWalker<GithubGithubResourceWalkerBudgetUsageType, GithubGithubResourceWalkerEstimatedBudgetUsageType, GithubResourceWalkerBudget>(
    imsProject, imsProject, config, budget, cursorResourceWalkerDataService
) {
    override suspend fun execute(): GithubGithubResourceWalkerBudgetUsageType {
        return GithubGithubResourceWalkerBudgetUsageType();
    }
}