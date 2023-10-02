package gropius.sync.github.repository

import gropius.sync.github.model.LabelInfo
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
     * @param neo4jId Database query param
     * @return result of database operation
     */
    suspend fun findByImsProjectAndNeo4jId(imsProject: String, neo4jId: String): LabelInfo?

    /**
     * Lookup to find the mapping given a GitHub id
     * @param githubId Database query param
     * @param url API URL syncing currently
     * @return result of database operation
     */
    suspend fun findByImsProjectAndGithubId(imsProject: String, githubId: String): LabelInfo?
}