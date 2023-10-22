package gropius.service.user.permission

import com.expediagroup.graphql.generator.execution.OptionalInput
import com.expediagroup.graphql.generator.scalars.ID
import gropius.authorization.GropiusAuthorizationContext
import gropius.dto.input.common.DeleteNodeInput
import gropius.dto.input.ifPresent
import gropius.dto.input.orElse
import gropius.dto.input.user.permission.CreateNodePermissionInput
import gropius.dto.input.user.permission.UpdateBasePermissionInput
import gropius.model.user.GropiusUser
import gropius.model.user.permission.NodePermission
import gropius.model.user.permission.NodeWithPermissions
import gropius.repository.GropiusRepository
import gropius.repository.findAllById
import gropius.repository.findById
import io.github.graphglue.authorization.Permission
import io.github.graphglue.model.Node
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull

/**
 * Base class for services for subclasses of [NodePermission]
 *
 * @param repository the associated repository used for CRUD functionality
 * @param T the type of Node this service is used for
 * @param R Repository type associated with [T]
 */
abstract class NodePermissionService<T : NodePermission<V>, V, R : GropiusRepository<T, String>>(
    repository: R
) : BasePermissionService<T, R>(repository) where V : Node, V : NodeWithPermissions<T> {

    /**
     * Creates the default permission on node creation
     * The created permission grants ADMIN and READ only to the provided [user]
     * Does not check permissions or save the created permission
     *
     * @param user the user who is granted the permission
     * @param constructor used to create the Permission instance
     * @return the created permission
     */
    protected suspend fun createDefaultPermission(
        user: GropiusUser, constructor: (String, String, MutableList<String>, Boolean) -> T
    ): T {
        val permission = constructor(
            "Admin permission",
            "The admin permission generated on node creation",
            mutableListOf(NodePermission.READ, NodePermission.ADMIN),
            false
        )
        permission.users() += user
        return permission
    }

    /**
     * Adds/Removes permissions to/from a [NodeWithPermissions]
     * Checks permissions on [addedPermissions], but not on [node]
     * Checks that at least one user has still [NodePermission.ADMIN] on [node]
     *
     * @param node the node of which to update the permissions
     * @param addedPermissions ids of permissions to add
     * @param removedPermissions ids of permissions to remove
     * @param authorizationContext used to check for permissions
     * @throws IllegalStateException if the [node] has no valid [NodePermission.ADMIN] permission left
     */
    suspend fun updatePermissionsOfNode(
        node: V,
        addedPermissions: OptionalInput<List<ID>>,
        removedPermissions: OptionalInput<List<ID>>,
        authorizationContext: GropiusAuthorizationContext
    ) {
        val permissionsToAdd = repository.findAllById(addedPermissions.orElse(emptyList()))
        val permissionsToRemove = repository.findAllById(removedPermissions.orElse(emptyList()))
        val nodesToCheck = permissionsToAdd.flatMap { it.nodesWithPermission() }.toSet()
        for (toCheck in nodesToCheck) {
            checkPermission(
                toCheck,
                Permission(NodePermission.ADMIN, authorizationContext),
                "add one of the permission ot other Nodes due to missing not having ADMIN on a Node associated with the permission"
            )
        }
        node.permissions() += permissionsToAdd
        val nodesToDelete = mutableListOf<T>()
        for (toRemove in permissionsToRemove) {
            if (node.permissions().remove(toRemove) && toRemove.nodesWithPermission().size == 1) {
                nodesToDelete += toRemove
            }
        }
        removedPermissions.ifPresent {
            ensureAdminPermissionExistsForNode(node)
        }
        repository.deleteAll(nodesToDelete).awaitSingleOrNull()
    }

    /**
     * Ensures that at least one permission on [node] grants [NodePermission.ADMIN] to at least one user
     *
     * @param node the node on which permissions where removed
     * @throws IllegalStateException if the [node] has no valid [NodePermission.ADMIN] permission left
     */
    private suspend fun ensureAdminPermissionExistsForNode(
        node: V
    ) {
        if (node.permissions()
                .none { NodePermission.ADMIN in it.entries && (it.allUsers || it.users().isNotEmpty()) }
        ) {
            throw IllegalStateException("No permission granting ADMIN left")
        }
    }

    /**
     * Creates a subtype of [NodePermission]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines the created permission
     * @param nodesWithPermission resolved nodes of `input.nodesWithPermission`
     * @param constructor used to create the permission
     * @return the created permission
     */
    protected suspend fun createNodePermission(
        authorizationContext: GropiusAuthorizationContext,
        input: CreateNodePermissionInput,
        nodesWithPermission: Collection<V>,
        constructor: (String, String, MutableList<String>, Boolean) -> T
    ): T {
        input.validate()
        nodesWithPermission.forEach {
            checkPermission(
                it,
                Permission(NodePermission.ADMIN, authorizationContext),
                "create permissions for a node in nodesWithPermission"
            )
        }
        val permission = constructor(
            input.name, input.description, input.entries.distinct().toMutableList(), input.allUsers
        )
        permission.nodesWithPermission() += nodesWithPermission
        createdBasePermissionNode(permission, input)
        return repository.save(permission).awaitSingle()
    }

    /**
     * Updates a [NodePermission] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [NodePermission] to update and how
     * @return the updated [NodePermission]
     */
    suspend fun updateNodePermission(
        authorizationContext: GropiusAuthorizationContext, input: UpdateBasePermissionInput
    ): T {
        input.validate()
        val permission = repository.findById(input.id)
        permission.nodesWithPermission().forEach {
            checkPermission(
                it,
                Permission(NodePermission.ADMIN, authorizationContext),
                "update the permission due to missing ADMIN permission on an associated node"
            )
        }
        updateBasePermission(permission, input)
        permission.nodesWithPermission().forEach {
            ensureAdminPermissionExistsForNode(it)
        }
        return repository.save(permission).awaitSingle()
    }

    /**
     * Deletes a [NodePermission] by id
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [NodePermission] to delete
     */
    suspend fun deleteNodePermission(
        authorizationContext: GropiusAuthorizationContext, input: DeleteNodeInput
    ) {
        input.validate()
        val permission = repository.findById(input.id)
        permission.nodesWithPermission().forEach {
            checkPermission(
                it,
                Permission(NodePermission.ADMIN, authorizationContext),
                "delete the permission due to missing ADMIN permission on an associated node"
            )
        }
        permission.nodesWithPermission().forEach {
            it.permissions() -= permission
            ensureAdminPermissionExistsForNode(it)
        }
        repository.delete(permission).awaitSingleOrNull()
    }

}