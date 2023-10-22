package gropius.authorization

import io.github.graphglue.authorization.AuthorizationContext
import org.neo4j.cypherdsl.core.Node

/**
 * Base gropius authorization context
 * Provides flexibility how to define the [userNode]
 *
 * @param checkPermission if `false`, no authorization checks should be performed
 */
abstract class GropiusAuthorizationContextBase(
    val checkPermission: Boolean
) : AuthorizationContext {

    /**
     * Should be used in authorization check expressions
     */
    abstract val userNode: Node

}