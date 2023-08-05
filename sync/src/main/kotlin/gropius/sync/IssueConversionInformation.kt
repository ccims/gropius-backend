package gropius.sync

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service

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

@Repository
interface IssueConversionInformationRepository : ReactiveMongoRepository<IssueConversionInformation, ObjectId> {
    suspend fun findByImsProjectAndGithubId(
        imsProject: String, githubId: String
    ): IssueConversionInformation?

    suspend fun findByImsProjectAndGropiusId(
        imsProject: String, gropiusId: String
    ): IssueConversionInformation?
}

@Service
class IssueConversionInformationService(val issueConversionInformationRepository: IssueConversionInformationRepository) :
    IssueConversionInformationRepository by issueConversionInformationRepository {}
