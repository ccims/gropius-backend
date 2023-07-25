package gropius.sync

interface GeneralResourceWalkerBudget {}
interface ResourceWalkerBudget<BudgetUsageType, EstimatedBudgetUsageType> : GeneralResourceWalkerBudget {
    suspend fun integrate(usage: BudgetUsageType);
    suspend fun mayExecute(expectedUsage: EstimatedBudgetUsageType): Boolean;
}