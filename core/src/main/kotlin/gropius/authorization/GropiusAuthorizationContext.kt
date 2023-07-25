package gropius.authorization

import graphql.schema.DataFetchingEnvironment
import gropius.model.user.GropiusUser
import io.github.graphglue.authorization.AuthorizationContext
import io.github.graphglue.graphql.extensions.authorizationContext
import org.neo4j.cypherdsl.core.Cypher

/**
 * Default authorization context
 *
 * @param userId the id of the authenticated [GropiusUser]
 * @param checkPermission if `false`, no authorization checks should be performed
 */
class GropiusAuthorizationContext(val userId: String, val checkPermission: Boolean = true) : AuthorizationContext {

    /**
     * A Cypher DSL parameter with the userId
     */
    val useridParameter = Cypher.anonParameter(userId)

}

/**
 * Gets the [GropiusAuthorizationContext] from the [DataFetchingEnvironment]
 * Assumes that [DataFetchingEnvironment.authorizationContext] is a [GropiusAuthorizationContext]
 */
val DataFetchingEnvironment.gropiusAuthorizationContext: GropiusAuthorizationContext get() {
    val tempAuthorizationContext = this.authorizationContext
    if (tempAuthorizationContext !is GropiusAuthorizationContext) {
        throw IllegalArgumentException("No GropiusAuthorizationContext available")
    }
    return tempAuthorizationContext
}

/**
 * Gets checkPermission of a possibly set [GropiusAuthorizationContext] and evaluates to false otherwise
 */
val DataFetchingEnvironment.checkPermission: Boolean get() {
    return if (this.authorizationContext is GropiusAuthorizationContext) {
        this.gropiusAuthorizationContext.checkPermission
    } else {
        false
    }
}