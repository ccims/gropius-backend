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

@Repository
interface NUserInfoRepository : ReactiveMongoRepository<UserInfoData, ObjectId> {
    suspend fun findByImsProjectAndGithubId(
        imsProject: String, githubId: String
    ): UserInfoData?

    suspend fun findByImsProjectAndGropiusId(
        imsProject: String, gropiusId: String
    ): UserInfoData?
}

@Service
class UserMapper(
    @Qualifier("graphglueNeo4jOperations")
    private val neoOperations: ReactiveNeo4jOperations, val nuserInfoRepository: NUserInfoRepository
) : NUserInfoRepository by nuserInfoRepository {
    @Transactional
    suspend fun saveUser(
        imsProject: IMSProject, githubId: String, gropiusId: String
    ) {
        val pile = nuserInfoRepository.findByImsProjectAndGithubId(imsProject.rawId!!, githubId) ?: UserInfoData(
            imsProject.rawId!!, githubId, gropiusId
        )
        nuserInfoRepository.save(pile).awaitSingle()
    }

    @Transactional
    suspend fun mapUser(imsProject: IMSProject, name: String): User {
        val pile = nuserInfoRepository.findByImsProjectAndGithubId(imsProject.rawId!!, name)
        return if (pile != null) {
            neoOperations.findById<User>(pile.gropiusId)!!
        } else {
            val gropiusUser = neoOperations.save(GropiusUser(name, null, null, name, false)).awaitSingle()
            nuserInfoRepository.save(
                UserInfoData(
                    imsProject.rawId!!, name, gropiusUser.rawId!!
                )
            ).awaitSingle()
            gropiusUser
        }
    }
}