package gropius.model.user

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import gropius.authorization.RELATED_TO_GLOBAL_PERMISSION_RULE
import gropius.model.user.permission.BasePermission
import gropius.model.user.permission.GlobalPermission
import io.github.graphglue.model.*
import org.springframework.data.annotation.Transient
import java.net.URI

@DomainNode
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
@Authorization(GlobalPermission.CAN_CREATE_TEMPLATES, allow = [Rule(RELATED_TO_GLOBAL_PERMISSION_RULE)])
class GropiusUser(
    displayName: String,
    email: String?,
    avatar: URI?,
    @property:GraphQLIgnore
    val username: String,
    @GraphQLDescription("True if the user is an admin")
    var isAdmin: Boolean
) : User(displayName, email, avatar) {

    companion object {
        const val PERMISSION = "PERMISSION"
    }

    @GraphQLDescription("A unique identifier for the GropiusUser. Note that this might not be unique across all Users.")
    override fun username(): String = username

    @NodeRelationship(IMSUser.GROPIUS_USER, Direction.INCOMING)
    @GraphQLDescription("The IMSUsers linked to this GropiusUser.")
    @FilterProperty
    val imsUsers by NodeSetProperty<IMSUser>()

    @NodeRelationship(PERMISSION, Direction.OUTGOING)
    @GraphQLDescription("Permissions the user has.")
    @FilterProperty
    val permissions by NodeSetProperty<BasePermission>()
}