package gropius.sync.github.repository

import gropius.sync.github.model.IssueInfo
import kotlinx.coroutines.flow.Flow
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository

/**
 * Mapping of a single issue from neo4j to github
 */
@Repository
interface IssueInfoRepository : ReactiveMongoRepository<IssueInfo, ObjectId> {
    /**
     * Find all issues that have outstanding changes
     * @return result of database operation
     */
    suspend fun findByDirtyIsTrue(): Flow<IssueInfo>

    /**
     * Lookup to find the mapping given a neo4j id
     * @param neo4jId Database query param
     * @return result of database operation
     */
    suspend fun findByNeo4jId(neo4jId: String): IssueInfo?

    /**
     * Lookup to find the mapping given a github id
     * @param githubId Database query param
     * @return result of database operation
     */
    suspend fun findByGithubId(githubId: String): IssueInfo?
}


