package gropius.sync

import jakarta.transaction.Transactional
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service

abstract class CursorResourceWalker<BudgetUsageType, EstimatedBudgetUsageType, Budget : ResourceWalkerBudget<BudgetUsageType, EstimatedBudgetUsageType>>(
    val imsProject: String,
    val resource: String,
    val config: CursorResourceWalkerConfig<BudgetUsageType, EstimatedBudgetUsageType>,
    val budget: Budget,
    val cursorResourceWalkerDataService: CursorResourceWalkerDataService
) {
    suspend fun getPriority(): Double {
        val data = cursorResourceWalkerDataService.findByImsProjectAndResource(imsProject, resource)
        return data?.currentPriority ?: config.basePriority
    }

    abstract suspend fun execute(): BudgetUsageType;

    suspend fun process() {
        if (budget.mayExecute(config.estimatedBudget)) {
            var usage = config.failureBudget;
            try {
                usage = execute()
            } finally {
                budget.integrate(usage)
                cursorResourceWalkerDataService.changePriority(
                    imsProject, resource, { config.basePriority }, config.basePriority
                );
            }
        } else {
            cursorResourceWalkerDataService.changePriority(
                imsProject, resource, { it + config.priorityIncrease }, config.basePriority
            );
        }
    }
}

@Document
data class CursorResourceWalkerData(
    @Indexed
    val imsProject: String,
    @Indexed
    val resource: String, var currentPriority: Double
) {
    @Id
    var id: ObjectId? = null
}

@Repository
interface CursorResourceWalkerDataRepository : ReactiveMongoRepository<CursorResourceWalkerData, ObjectId> {
    suspend fun findByImsProjectAndResource(imsProject: String, resource: String): CursorResourceWalkerData?
}

@Service
class CursorResourceWalkerDataService(val cursorResourceWalkerDataRepository: CursorResourceWalkerDataRepository) :
    CursorResourceWalkerDataRepository by cursorResourceWalkerDataRepository {
    @Transactional
    suspend fun changePriority(
        imsProject: String, resource: String, operator: (Double) -> Double, basePriority: Double
    ) {
        val data = cursorResourceWalkerDataRepository.findByImsProjectAndResource(imsProject, resource)
            ?: CursorResourceWalkerData(imsProject, resource, basePriority)
        data.currentPriority = operator(data.currentPriority)
        cursorResourceWalkerDataRepository.save(data)
    }
}

data class CursorResourceWalkerConfig<BudgetUsageType, EstimatedBudgetUsageType>(
    val basePriority: Double,
    val priorityIncrease: Double,
    val estimatedBudget: EstimatedBudgetUsageType,
    val failureBudget: BudgetUsageType
) {}