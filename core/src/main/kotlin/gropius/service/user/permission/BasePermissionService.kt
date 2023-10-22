package gropius.service.user.permission

import gropius.dto.input.ifPresent
import gropius.dto.input.isPresent
import gropius.dto.input.orElse
import gropius.dto.input.user.permission.CreateBasePermissionInput
import gropius.dto.input.user.permission.UpdateBasePermissionInput
import gropius.model.user.permission.BasePermission
import gropius.repository.GropiusRepository
import gropius.repository.findAllById
import gropius.service.common.NodeService

/**
 * Base class for services for subclasses of [BasePermission]
 *
 * @param repository the associated repository used for CRUD functionality
 * @param T the type of Node this service is used for
 * @param R Repository type associated with [T]
 */
abstract class BasePermissionService<T : BasePermission, R : GropiusRepository<T, String>>(
    repository: R
) : NodeService<T, R>(repository) {

    /**
     * Updates [node] based on [input]
     * Should be called after the node was constructed
     *
     * @param node the node to update
     * @param input defines how to update the provided [node]
     */
    suspend fun createdBasePermissionNode(node: T, input: CreateBasePermissionInput) {
        node.users() += gropiusUserRepository.findAllById(input.users)
    }

    /**
     * Updates [node] based on [input]
     * Updates name, description, entries, allUsers and users
     *
     * @param node the node to update
     * @param input defines how to update the provided [node]
     */
    suspend fun updateBasePermission(node: T, input: UpdateBasePermissionInput) {
        input.name.ifPresent {
            node.name = it
        }
        input.description.ifPresent {
            node.description = it
        }
        input.allUsers.ifPresent {
            node.allUsers = it
        }
        input.addedUsers.ifPresent {
            node.users() += gropiusUserRepository.findAllById(it)
        }
        input.removedUsers.ifPresent {
            node.users() -= gropiusUserRepository.findAllById(it).toSet()
        }
        if (input.addedEntries.isPresent || input.removedEntries.isPresent) {
            val newEntries = node.entries.toMutableSet()
            newEntries += input.addedEntries.orElse(emptyList())
            newEntries -=input.removedEntries.orElse(emptyList()).toSet()
            node.entries.clear()
            node.entries += newEntries
        }
    }

}