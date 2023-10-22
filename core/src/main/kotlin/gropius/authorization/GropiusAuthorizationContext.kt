package gropius.authorization

import graphql.schema.DataFetchingEnvironment
import gropius.model.user.GropiusUser
import io.github.graphglue.graphql.extensions.authorizationContext
import org.neo4j.cypherdsl.core.Cypher
import org.neo4j.cypherdsl.core.Node

/**
 * Default authorization context
 *
 * @param userId the id of the authenticated [GropiusUser]
 * @param checkPermission if `false`, no authorization checks should be performed
 */
class GropiusAuthorizationContext(
    val userId: String, checkPermission: Boolean = true
) : GropiusAuthorizationContextBase(
    checkPermission
) {

    override val userNode: Node
        get() = Cypher.node(GropiusUser::class.simpleName!!).withProperties(mapOf("id" to Cypher.anonParameter(userId)))

}

/**
 * Gets the [GropiusAuthorizationContext] from the [DataFetchingEnvironment]
 * Assumes that [DataFetchingEnvironment.authorizationContext] is a [GropiusAuthorizationContext]
 */
val DataFetchingEnvironment.gropiusAuthorizationContext: GropiusAuthorizationContext
    get() {
        val tempAuthorizationContext = this.authorizationContext
        if (tempAuthorizationContext !is GropiusAuthorizationContext) {
            throw IllegalArgumentException("No GropiusAuthorizationContext available")
        }
        return tempAuthorizationContext
    }

/**
 * Gets checkPermission of a possibly set [GropiusAuthorizationContext] and evaluates to false otherwise
 */
val DataFetchingEnvironment.checkPermission: Boolean
    get() {
        return this.authorizationContext is GropiusAuthorizationContext
    }