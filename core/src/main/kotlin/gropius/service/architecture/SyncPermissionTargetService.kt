package gropius.service.architecture

import gropius.authorization.GropiusAuthorizationContext
import gropius.dto.input.architecture.UpdateSyncPermissionsInput
import gropius.model.architecture.SyncPermissionTarget
import gropius.model.user.permission.NodePermission
import gropius.repository.architecture.SyncPermissionTargetRepository
import gropius.repository.findById
import gropius.service.common.NamedNodeService
import io.github.graphglue.authorization.Permission
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service

/**
 * Service for [SyncPermissionTarget]s.
 *
 * @param repository the associated repository used for CRUD functionality
 */
@Service
class SyncPermissionTargetService(
    repository: SyncPermissionTargetRepository
) : NamedNodeService<SyncPermissionTarget, SyncPermissionTargetRepository>(repository) {

    /**
     * Updates the sync permissions of the current user on the [SyncPermissionTarget] based on the provided [input]
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines the updated sync permissions
     * @return the updated [SyncPermissionTarget]
     */
    suspend fun updateSyncPermissions(
        authorizationContext: GropiusAuthorizationContext, input: UpdateSyncPermissionsInput
    ): SyncPermissionTarget {
        input.validate()
        val target = repository.findById(input.id)
        checkPermission(target, Permission(NodePermission.READ, authorizationContext), "use the SyncPermissionTarget")
        val user = getUser(authorizationContext)
        if (input.canSyncSelf != null) {
            if (input.canSyncSelf) {
                target.syncSelfAllowedBy() += user
            } else {
                target.syncSelfAllowedBy() -= user
            }
        }
        if (input.canSyncOthers != null) {
            if (input.canSyncOthers) {
                target.syncOthersAllowedBy() += user
            } else {
                target.syncOthersAllowedBy() -= user
            }
        }
        return repository.save(target).awaitSingle()
    }

}