package gropius.sync.github.repository

import gropius.sync.github.model.UserInfo
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository

/**
 * Mapping of a single user from neo4j to github
 */
@Repository
interface UserInfoRepository : ReactiveMongoRepository<UserInfo, ObjectId> {
    /**
     * Lookup to find the mapping given a login
     * @param login Database query param
     * @return result of database operation
     */
    suspend fun findByLogin(login: String): UserInfo?
    /**
     * Lookup to find the mapping given a neo4j id
     * @param neo4jId Database query param
     * @return result of database operation
     */
    suspend fun findByNeo4jId(neo4jId: String): UserInfo?
}
