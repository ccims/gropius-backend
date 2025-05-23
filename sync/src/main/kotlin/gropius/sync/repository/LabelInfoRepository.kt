package gropius.sync.repository

import gropius.sync.model.LabelInfo
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository

/**
 * Repository for mapping of a single label from neo4j to GitHub
 */
@Repository
interface LabelInfoRepository : ReactiveMongoRepository<LabelInfo, ObjectId> {
    /**
     * Lookup to find the mapping given a neo4j id
     * @param localName Name query param
     * @return result of database operation
     */
    suspend fun findByImsProjectAndLocalName(imsProject: String, localName: String): LabelInfo?

    /**
     * Lookup to find the mapping given a GitHub id
     * @param githubId Database query param
     * @param url API URL syncing currently
     * @return result of database operation
     */
    suspend fun findByImsProjectAndGithubId(imsProject: String, githubId: String): LabelInfo?
}