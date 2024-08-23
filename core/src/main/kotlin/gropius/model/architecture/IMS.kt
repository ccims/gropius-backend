package gropius.model.architecture

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import gropius.authorization.RELATED_TO_NODE_PERMISSION_RULE
import gropius.model.template.BaseTemplate
import gropius.model.template.IMSTemplate
import gropius.model.template.MutableTemplatedNode
import gropius.model.user.IMSUser
import gropius.model.user.permission.IMSPermission
import gropius.model.user.permission.NodePermission
import gropius.model.user.permission.NodeWithPermissions
import io.github.graphglue.model.*
import org.springframework.data.neo4j.core.schema.CompositeProperty

@DomainNode("imss", searchQueryName = "searchIMSs")
@GraphQLDescription(
    """Entity which represents an issue management system (like GitHub, Jira, Redmine, ...).
    Trackables can be added to this via an IMSProject, so that their issues are synced to this IMS.
    READ is granted via an associated IMSPermission.
    """
)
@Authorization(NodePermission.READ, allow = [Rule(RELATED_TO_NODE_PERMISSION_RULE)])
@Authorization(NodePermission.ADMIN, allow = [Rule(RELATED_TO_NODE_PERMISSION_RULE)])
@Authorization(IMSPermission.SYNC_TRACKABLES, allow = [Rule(RELATED_TO_NODE_PERMISSION_RULE, options = [NodePermission.ADMIN])])
class IMS(
    name: String,
    description: String,
    @property:GraphQLIgnore
    @CompositeProperty
    override val templatedFields: MutableMap<String, String>
) : SyncPermissionTarget(name, description), MutableTemplatedNode, NodeWithPermissions<IMSPermission> {

    companion object {
        const val PROJECT = "PROJECT"
        const val USER = "USER"
    }

    @NodeRelationship(BaseTemplate.USED_IN, Direction.INCOMING)
    @GraphQLDescription("The Template of this Component.")
    @FilterProperty
    @OrderProperty
    override val template by NodeProperty<IMSTemplate>()

    @NodeRelationship(PROJECT, Direction.OUTGOING)
    @GraphQLDescription("Projects which are synced to this IMS.")
    @FilterProperty
    val projects by NodeSetProperty<IMSProject>()

    @NodeRelationship(USER, Direction.OUTGOING)
    @GraphQLDescription("Users of this IMS.")
    @FilterProperty
    val users by NodeSetProperty<IMSUser>()

    @NodeRelationship(NodePermission.NODE, Direction.INCOMING)
    @GraphQLDescription("Permissions for this IMS.")
    @FilterProperty
    override val permissions by NodeSetProperty<IMSPermission>()

}