package gropius.sync.user

import gropius.model.architecture.IMSProject
import gropius.model.user.GropiusUser
import gropius.model.user.User
import gropius.sync.*
import jakarta.transaction.Transactional
import kotlinx.coroutines.reactor.awaitSingle
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.data.neo4j.core.ReactiveNeo4jOperations
import org.springframework.data.neo4j.core.findById
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service

/**
 * Repository for mapping of a single user from neo4j to GitHub
 * @param imsProject IMS project to sync
 * @param githubId GitHub ID of the user
 * @param gropiusId Gropius ID of the user
 */
@Document
data class UserInfoData(
    @Indexed
    val imsProject: String,
    @Indexed
    val githubId: String,
    @Indexed
    val gropiusId: String
) {
    /**
     * MongoDB ID
     */
    @Id
    var id: ObjectId? = null
}

/**
 * Repository for mapping of a single user from neo4j to GitHub
 */
@Repository
interface NUserInfoRepository : ReactiveMongoRepository<UserInfoData, ObjectId> {

    /**
     * Lookup to find the mapping given a neo4j id
     * @param imsProject Database query param
     * @param gropiusId Database query param
     * @return result of database operation
     */
    suspend fun findByImsProjectAndGithubId(
        imsProject: String, githubId: String
    ): UserInfoData?

    /**
     * Lookup to find the mapping given a GitHub id
     * @param imsProject Database query param
     * @param gropiusId Database query param
     * @return result of database operation
     */
    suspend fun findByImsProjectAndGropiusId(
        imsProject: String, gropiusId: String
    ): UserInfoData?
}

/**
 * Repository for user mapping
 * @param neoOperations Neo4j operations
 * @param nuserInfoRepository MongoDB repository
 */
@Service
class UserMapper(
    @Qualifier("graphglueNeo4jOperations")
    private val neoOperations: ReactiveNeo4jOperations, val nuserInfoRepository: NUserInfoRepository
) : NUserInfoRepository by nuserInfoRepository {

    /**
     * Save a user to the database
     * @param imsProject IMS project to sync
     * @param githubId GitHub ID of the user
     * @param gropiusId Gropius ID of the user
     */
    @Transactional
    suspend fun saveUser(
        imsProject: IMSProject, githubId: String, gropiusId: String
    ) {
        val pile = nuserInfoRepository.findByImsProjectAndGithubId(imsProject.rawId!!, githubId) ?: UserInfoData(
            imsProject.rawId!!, githubId, gropiusId
        )
        nuserInfoRepository.save(pile).awaitSingle()
    }

    /**
     * Map a user from given parameters
     * @param imsProject IMS project to sync
     * @param name GitHub name of the user
     * @param displayName Display name of the user
     * @param email Email of the user
     * @return Mapped user
     */
    @Transactional
    suspend fun mapUser(
        imsProject: IMSProject, name: String, displayName: String? = null, email: String? = null
    ): User {
        val pile = nuserInfoRepository.findByImsProjectAndGithubId(imsProject.rawId!!, name)
        return if (pile != null) {
            neoOperations.findById<User>(pile.gropiusId)!!
        } else {
            val gropiusUser =
                neoOperations.save(GropiusUser(displayName ?: name, email, null, name, false)).awaitSingle()
            nuserInfoRepository.save(
                UserInfoData(
                    imsProject.rawId!!, name, gropiusUser.rawId!!
                )
            ).awaitSingle()
            gropiusUser
        }
    }
}