package gropius.service.common

import gropius.authorization.GropiusAuthorizationContext
import gropius.model.user.GropiusUser
import gropius.repository.GropiusRepository
import gropius.repository.user.GropiusUserRepository
import io.github.graphglue.authorization.AuthorizationChecker
import io.github.graphglue.authorization.Permission
import io.github.graphglue.model.Node
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.beans.factory.annotation.Autowired

/**
 * Base class for services for subclasses of [Node]
 *
 * @param repository the associated repository used for CRUD functionality
 * @param T the type of Node this service is used for
 * @param R Repository type associated with [T]
 */
abstract class NodeService<T : Node, R : GropiusRepository<T, String>>(val repository: R) {

    /**
     * Injected, used for the [checkPermission] function
     */
    @Autowired
    lateinit var authorizationChecker: AuthorizationChecker

    /**
     * Injected, used to get a user based on a [GropiusAuthorizationContext]
     */
    @Autowired
    lateinit var gropiusUserRepository: GropiusUserRepository

    /**
     * Checks if the [permission] is granted on [node]
     * If checkPermission on the `permission.context` is `false`, no permission is evaluated
     * Does not handle the case that the user is an admin
     *
     * @param node the node where the permission must be granted
     * @param permission the permission to check for, none is checked if `null`
     * @param errorMessage the message to throw in case the permission is not granted,
     *   appended to "User does not have permission to"
     * @throws IllegalArgumentException with the provided [errorMessage] in case the permission is not granted
     */
    suspend fun checkPermission(node: Node, permission: Permission, errorMessage: String) {
        if (!evaluatePermission(node, permission)) {
            throw IllegalArgumentException("User does not have permission to $errorMessage")
        }
    }

    /**
     * Checks if the user defined by [authorizationContext] is a global admin
     *
     * @param authorizationContext defines the user to check for isAdmin
     * @throws IllegalArgumentException if checkPermission is `true` and the user is not an admin
     */
    suspend fun checkIsAdmin(authorizationContext: GropiusAuthorizationContext, errorMessage: String) {
        if (authorizationContext.checkPermission && !getUser(authorizationContext).isAdmin) {
            throw IllegalArgumentException("Uses does not have permission to $errorMessage")
        }
    }

    /**
     * Evaluates if the [permission] is granted on [node]
     * If checkPermission on the `permission.context` is `false`, no permission is evaluated
     * Does not handle the case that the user is an admin
     *
     * @param node the node where the permission must be granted
     * @param permission the permission to check for, none is checked if `null`
     * @return `true` if the permission is granted
     */
    suspend fun evaluatePermission(node: Node, permission: Permission): Boolean {
        val checkPermission = (permission.context as GropiusAuthorizationContext).checkPermission
        return !checkPermission || authorizationChecker.hasAuthorization(node, permission).awaitSingle()
    }

    /**
     * Gets a [GropiusUser] based on the userId from the [authorizationContext]
     *
     * @param authorizationContext used to get the user id, must be a [GropiusAuthorizationContext]
     * @return the found user
     */
    suspend fun getUser(authorizationContext: GropiusAuthorizationContext): GropiusUser {
        return gropiusUserRepository.findById(authorizationContext.userId).awaitSingle()
    }
}