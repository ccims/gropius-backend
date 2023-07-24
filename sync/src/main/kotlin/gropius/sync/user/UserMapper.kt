package gropius.sync.user

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
    @Id
    var id: ObjectId? = null
}

@Repository
interface UserInfoRepository : ReactiveMongoRepository<UserInfoData, ObjectId> {
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
    private val neoOperations: ReactiveNeo4jOperations, val userInfoRepository: UserInfoRepository
) : UserInfoRepository by userInfoRepository {
    @Transactional
    suspend fun saveUser(
        imsProject: String, githubId: String, gropiusId: String
    ) {
        val pile = userInfoRepository.findByImsProjectAndGithubId(imsProject, githubId) ?: UserInfoData(
            imsProject, githubId, gropiusId
        )
        userInfoRepository.save(pile).awaitSingle()
    }

    @Transactional
    suspend fun mapUser(imsProject: String, name: String): User {
        var pile = userInfoRepository.findByImsProjectAndGithubId(imsProject, name)
        if (pile != null) {
            return neoOperations.findById<User>(pile.gropiusId)!!
        } else {
            val gropiusUser = neoOperations.save(GropiusUser(name, null, null, name, false)).awaitSingle()
            userInfoRepository.save(
                UserInfoData(
                    imsProject, name, gropiusUser.rawId!!
                )
            ).awaitSingle()
            return gropiusUser
        }
    }
}