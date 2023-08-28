package gropius.sync

import gropius.model.architecture.IMSProject
import jakarta.transaction.Transactional
import kotlinx.coroutines.reactor.awaitSingle
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service

abstract class ResourceWalker {
    abstract suspend fun getPriority(): Double;
    abstract suspend fun process();
}

abstract class CursorResourceWalker<BudgetUsageType, EstimatedBudgetUsageType, Budget : ResourceWalkerBudget<BudgetUsageType, EstimatedBudgetUsageType>>(
    val imsProject: IMSProject,
    val resource: String,
    val resourceWalkerConfig: CursorResourceWalkerConfig<BudgetUsageType, EstimatedBudgetUsageType>,
    val budget: Budget,
    val cursorResourceWalkerDataService: CursorResourceWalkerDataService
) : ResourceWalker() {
    override suspend fun getPriority(): Double {
        val data = cursorResourceWalkerDataService.findByImsProjectAndResource(imsProject.rawId!!, resource)
        return data?.currentPriority ?: resourceWalkerConfig.basePriority
    }

    protected abstract suspend fun execute(): BudgetUsageType;

    override suspend fun process() {
        if (budget.mayExecute(resourceWalkerConfig.estimatedBudget)) {
            var usage = resourceWalkerConfig.failureBudget;
            try {
                usage = execute()
            } finally {
                budget.integrate(usage)
                cursorResourceWalkerDataService.changePriority(
                    imsProject, resource, { resourceWalkerConfig.basePriority }, resourceWalkerConfig.basePriority
                );
            }
        } else {
            cursorResourceWalkerDataService.changePriority(
                imsProject, resource, { it + resourceWalkerConfig.priorityIncrease }, resourceWalkerConfig.basePriority
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
    /**
     * MongoDB ID
     */
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
        imsProject: IMSProject, resource: String, operator: (Double) -> Double, basePriority: Double
    ) {
        val data = cursorResourceWalkerDataRepository.findByImsProjectAndResource(imsProject.rawId!!, resource)
            ?: CursorResourceWalkerData(imsProject.rawId!!, resource, basePriority)
        data.currentPriority = operator(data.currentPriority)
        cursorResourceWalkerDataRepository.save(data).awaitSingle()
    }
}

data class CursorResourceWalkerConfig<BudgetUsageType, EstimatedBudgetUsageType>(
    val basePriority: Double,
    val priorityIncrease: Double,
    val estimatedBudget: EstimatedBudgetUsageType,
    val failureBudget: BudgetUsageType
) {}