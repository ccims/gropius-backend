package gropius.sync

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service

/**
 * This class is used to store information about the conversion of a issue
 * @param imsProject The project the issue belongs to.
 * @param githubId The id of the issue on github.
 * @param gropiusId The id of the issue on gropius.
 */
@Document
class IssueConversionInformation(
    @Indexed
    val imsProject: String,
    @Indexed
    val githubId: String,
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
 * This interface is used to acess stored information about the conversion of a issue
 */
@Repository
interface IssueConversionInformationRepository : ReactiveMongoRepository<IssueConversionInformation, ObjectId> {

    /**
     * Find issues using ims id
     * @param imsProject The project the issue belongs to.
     * @param githubId The id of the issue on github.
     * @return The gropius issue.
     */
    suspend fun findByImsProjectAndGithubId(
        imsProject: String, githubId: String
    ): IssueConversionInformation?

    /**
     * Find issues using gropius id
     * @param imsProject The project the issue belongs to.
     * @param gropiusId The id of the issue on gropius.
     * @return The gropius issue.
     */
    suspend fun findByImsProjectAndGropiusId(
        imsProject: String, gropiusId: String
    ): IssueConversionInformation?
}

/**
 * Service to access issue conversion information
 */
@Service
class IssueConversionInformationService(val issueConversionInformationRepository: IssueConversionInformationRepository) :
    IssueConversionInformationRepository by issueConversionInformationRepository {}
