package gropius.sync.jira

import kotlinx.coroutines.reactor.awaitSingle
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

/**
 * Base class for storing information about general sync status
 * @param imsProject IMS project ID
 * @param lastSuccessfulSync Timestamp of the last successful sync. nullable for future use
 */
@Document
data class SyncStatus(
    @Indexed
    val imsProject: String,
    @Indexed
    var lastSuccessfulSync: OffsetDateTime?,
) {
    /**
     * MongoDB ID
     */
    @Id
    var id: ObjectId? = null
}

/**
 * Repository for accessing the sync status
 */
@Repository
interface SyncStatusRepository : ReactiveMongoRepository<SyncStatus, ObjectId> {
    /**
     * Find using the IMSProject ID
     */
    suspend fun findByImsProject(
        imsProject: String
    ): SyncStatus?
}

/**
 * Service for modifying the sync status
 */
@Service
class SyncStatusService(val syncStatusRepository: SyncStatusRepository) : SyncStatusRepository by syncStatusRepository {

    @Transactional
    suspend fun updateTime(imsProject: String, lastSuccessfulSync: OffsetDateTime) {
        val status = syncStatusRepository.findByImsProject(imsProject) ?: SyncStatus(imsProject, null)
        status.lastSuccessfulSync = lastSuccessfulSync
        syncStatusRepository.save(status).awaitSingle()
    }
}
