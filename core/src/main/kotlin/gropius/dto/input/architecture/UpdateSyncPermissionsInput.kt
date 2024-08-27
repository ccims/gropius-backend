package gropius.dto.input.architecture

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.scalars.ID
import gropius.dto.input.common.Input

@GraphQLDescription("Input for the updateSyncPermissions mutation")
class UpdateSyncPermissionsInput(
    @GraphQLDescription("The SyncPermissionTarget to update the sync permissions for the current user")
    val id: ID,
    @GraphQLDescription("Whether the sync service is allowed to sync content of the user")
    val canSyncSelf: Boolean?,
    @GraphQLDescription("Whether the sync service is allowed to sync content of other users")
    val canSyncOthers: Boolean?
) : Input()