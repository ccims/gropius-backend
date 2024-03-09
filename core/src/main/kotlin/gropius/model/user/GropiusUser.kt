package gropius.model.user

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.authorization.RELATED_TO_GLOBAL_PERMISSION_RULE
import gropius.model.architecture.SyncPermissionTarget
import gropius.model.user.permission.BasePermission
import gropius.model.user.permission.GlobalPermission
import io.github.graphglue.model.*
import java.net.URI

/**
 * Name of the bean defining the nodePermission filter
 */
const val NODE_PERMISSION_FILTER_BEAN = "nodePermissionFilter"

@DomainNode(searchQueryName = "searchGropiusUsers")
@GraphQLDescription(
    """A user of the Gropius System.
    The username can be used as unique identifier for GropiusUsers.
    IMSUsers can be linked to a GropiusUser.
    Note however that this link does not affect relationships, e.g. if an IMSUser is part of an Assignment,
    after the IMSUser was linked to a GropiusUser, the GropiusUser does not link directly to the Assignment.
    Therefore, to collect all Assignments, Issue participations etc. it is necessary to also request all 
    linked IMSUsers and their Assignments etc.
    """
)
@Authorization(GlobalPermission.CAN_CREATE_COMPONENTS, allow = [Rule(RELATED_TO_GLOBAL_PERMISSION_RULE)])
@Authorization(GlobalPermission.CAN_CREATE_PROJECTS, allow = [Rule(RELATED_TO_GLOBAL_PERMISSION_RULE)])
@Authorization(GlobalPermission.CAN_CREATE_IMSS, allow = [Rule(RELATED_TO_GLOBAL_PERMISSION_RULE)])
@Authorization(GlobalPermission.CAN_CREATE_TEMPLATES, allow = [Rule(RELATED_TO_GLOBAL_PERMISSION_RULE)])
@AdditionalFilter(NODE_PERMISSION_FILTER_BEAN)
class GropiusUser(
    displayName: String,
    email: String?,
    avatar: URI?,
    username: String,
    @GraphQLDescription("True if the user is an admin")
    var isAdmin: Boolean
) : User(displayName, email, avatar, username) {

    companion object {
        const val PERMISSION = "PERMISSION"
        const val CAN_SYNC_SELF = "CAN_SYNC_SELF"
        const val CAN_SYNC_OTHERS = "CAN_SYNC_OTHERS"
    }

    @GraphQLDescription("A unique identifier for the GropiusUser. Note that this might not be unique across all Users.")
    override fun username(): String = username!!

    @NodeRelationship(IMSUser.GROPIUS_USER, Direction.INCOMING)
    @GraphQLDescription("The IMSUsers linked to this GropiusUser.")
    @FilterProperty
    val imsUsers by NodeSetProperty<IMSUser>()

    @NodeRelationship(PERMISSION, Direction.OUTGOING)
    @GraphQLDescription("Permissions the user has.")
    @FilterProperty
    val permissions by NodeSetProperty<BasePermission>()

    @NodeRelationship(CAN_SYNC_SELF, Direction.OUTGOING)
    @GraphQLDescription("The UserSyncPermissionTarget this users allow to sync content of this user.")
    @FilterProperty
    val canSyncSelf by NodeSetProperty<SyncPermissionTarget>()

    @NodeRelationship(CAN_SYNC_OTHERS, Direction.OUTGOING)
    @GraphQLDescription("The IMSSyncPermissionTarget this users allow to sync content of other users.")
    @FilterProperty
    val canSyncOthers by NodeSetProperty<SyncPermissionTarget>()
}