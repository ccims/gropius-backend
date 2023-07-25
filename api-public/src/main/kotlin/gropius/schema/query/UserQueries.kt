package gropius.schema.query

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import graphql.schema.DataFetchingEnvironment
import gropius.authorization.GropiusAuthorizationContext
import gropius.authorization.gropiusAuthorizationContext
import gropius.model.user.GropiusUser
import gropius.repository.user.GropiusUserRepository
import io.github.graphglue.graphql.extensions.authorizationContext
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Component

/**
 * Contains all user-related queries
 *
 * @param gropiusUserRepository for getting [GropiusUser] by id
 */
@Component
class UserQueries(private val gropiusUserRepository: GropiusUserRepository) : Query {

    @GraphQLDescription("The current authenticated user")
    suspend fun currentUser(dfe: DataFetchingEnvironment): GropiusUser? {
        return if (dfe.authorizationContext is GropiusAuthorizationContext) {
            val userId = dfe.gropiusAuthorizationContext.userId
            gropiusUserRepository.findById(userId).awaitSingle()
        } else {
            null
        }
    }

}