package gropius.sync

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service

/**
 * Base class for storing information about the conversion of a timeline item
 * @param imsProject IMS project ID
 * @param githubId GitHub ID of the timeline item
 * @param gropiusId Gropius ID of the timeline item
 */
@Document
abstract class TimelineItemConversionInformation(
    @Indexed
    val imsProject: String,
    @Indexed
    val githubId: String?,
    @Indexed
    var gropiusId: String?
) {
    /**
     * MongoDB ID
     */
    @Id
    var id: ObjectId? = null
}

/**
 * Repository for accessing the timeline item conversion information
 */
@Repository
interface TimelineItemConversionInformationRepository :
    ReactiveMongoRepository<TimelineItemConversionInformation, ObjectId> {

    /**
     * Find using the IMS-side ID
     */
    suspend fun findByImsProjectAndGithubId(
        imsProject: String, githubId: String
    ): TimelineItemConversionInformation?

    /**
     * Find using the Gropius side ID
     */
    suspend fun findByImsProjectAndGropiusId(
        imsProject: String, gropiusId: String
    ): TimelineItemConversionInformation?
}

/**
 * Service for modifying the timeline item conversion information
 */
@Service
class TimelineItemConversionInformationService(val timelineItemConversionInformationRepository: TimelineItemConversionInformationRepository) :
    TimelineItemConversionInformationRepository by timelineItemConversionInformationRepository {}
