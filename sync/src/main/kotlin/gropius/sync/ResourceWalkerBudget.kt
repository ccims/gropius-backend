package gropius.sync

/**
 * A budget for a [ResourceWalker].
 */
interface GeneralResourceWalkerBudget {}

/**
 * A budget for a [ResourceWalker] that uses a [BudgetUsageType] to track the usage of the budget.
 * @param BudgetUsageType The type of the budget usage.
 * @param EstimatedBudgetUsageType The type of the estimated budget usage.
 */
interface ResourceWalkerBudget<BudgetUsageType, EstimatedBudgetUsageType> : GeneralResourceWalkerBudget {

    /**
     * Integrate a given usage into the budget
     */
    suspend fun integrate(usage: BudgetUsageType);

    /**
     * Check if a given budget should still be executed
     */
    suspend fun mayExecute(expectedUsage: EstimatedBudgetUsageType): Boolean;
}