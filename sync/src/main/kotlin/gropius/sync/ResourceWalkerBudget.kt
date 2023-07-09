package gropius.sync

interface ResourceWalkerBudget<BudgetUsageType, EstimatedBudgetUsageType> {
    suspend fun integrate(usage: BudgetUsageType);
    suspend fun mayExecute(expectedUsage: EstimatedBudgetUsageType): Boolean;
}