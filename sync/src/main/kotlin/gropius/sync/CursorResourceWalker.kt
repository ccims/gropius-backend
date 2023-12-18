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

/**
 * In terface for implementing a resource walker
 */
abstract class ResourceWalker {
    abstract suspend fun getPriority(): Double;
    abstract suspend fun process();
}

/**
 * Cursor Resource Walker#
 * @param imsProject IMS project to sync
 * @param resource Resource to sync
 * @param resourceWalkerConfig Configuration for the resource walker
 * @param budget Budget to use for the resource walker
 * @param cursorResourceWalkerDataService Data service for the resource walker
 */
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

    /**
     * Execute the resource walker
     * @return the budget usage
     */
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

/**
 * Database data for a cursor resource walker
 * @param imsProject IMS project to sync
 * @param resource Resource to sync
 * @param currentPriority Current priority of the resource
 */
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

/**
 * Repository for cursor resource walker data
 */
@Repository
interface CursorResourceWalkerDataRepository : ReactiveMongoRepository<CursorResourceWalkerData, ObjectId> {

    /**
     * Check the current data for a given imsProject/resource combo
     * @param imsProject Database query param
     * @param resource Database query param
     * @return result of database operation
     */
    suspend fun findByImsProjectAndResource(imsProject: String, resource: String): CursorResourceWalkerData?
}

/**
 * Data service for cursor resource walker data
 * @param cursorResourceWalkerDataRepository Repository for cursor resource walker data
 */
@Service
class CursorResourceWalkerDataService(val cursorResourceWalkerDataRepository: CursorResourceWalkerDataRepository) :
    CursorResourceWalkerDataRepository by cursorResourceWalkerDataRepository {

    /**
     * Change the priority of an element
     * @param imsProject IMS project to sync
     * @param resource Resource to sync
     * @param operator Operator to apply to the priority
     * @param basePriority Base priority to use if the element is not yet in the database
     */
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

/**
 * Config for a cursor resource walker
 * @param basePriority Base priority of the resource
 * @param priorityIncrease Priority increase of the resource
 * @param estimatedBudget Estimated budget of the resource
 * @param failureBudget Failure budget of the resource
 */
data class CursorResourceWalkerConfig<BudgetUsageType, EstimatedBudgetUsageType>(
    val basePriority: Double,
    val priorityIncrease: Double,
    val estimatedBudget: EstimatedBudgetUsageType,
    val failureBudget: BudgetUsageType
) {}