package gropius.model.architecture

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import gropius.authorization.RELATED_TO_ADMIN_NODE_PERMISSION_RULE
import gropius.model.common.ExtensibleNode
import gropius.model.template.BaseTemplate
import gropius.model.template.IMSProjectTemplate
import gropius.model.template.MutableTemplatedNode
import gropius.model.user.permission.NodePermission
import gropius.model.user.permission.TrackablePermission
import io.github.graphglue.model.*
import org.springframework.data.neo4j.core.schema.CompositeProperty

@DomainNode
@GraphQLDescription(
    """Project on an IMS, represents a Trackable synced to an IMS.
    The representation on the IMS depends on the type of IMS, e.g. for GitHub, a project is a repository.
    READ is granted if READ is granted on `trackable` or `ims`.
    """
)
@Authorization(
    NodePermission.READ,
    allow = [Rule(RELATED_TO_ADMIN_NODE_PERMISSION_RULE, "2")],
    allowFromRelated = ["trackable"]
)
@Authorization(TrackablePermission.MANAGE_IMS, allowFromRelated = ["trackable"])
@Authorization(NodePermission.RELATED_TO_NODE_PERMISSION, allowFromRelated = ["ims"])
class IMSProject(
    @property:GraphQLIgnore
    @CompositeProperty
    override val templatedFields: MutableMap<String, String>
) : ExtensibleNode(), MutableTemplatedNode {

    @NodeRelationship(BaseTemplate.USED_IN, Direction.INCOMING)
    @GraphQLDescription("The Template of this Component.")
    @FilterProperty
    override val template by NodeProperty<IMSProjectTemplate>()

    @NodeRelationship(Trackable.SYNCS_TO, Direction.INCOMING)
    @GraphQLDescription("The trackable which is synced.")
    @FilterProperty
    val trackable by NodeProperty<Trackable>()

    @NodeRelationship(IMS.PROJECT, Direction.INCOMING)
    @GraphQLDescription("The IMS this project is a part of.")
    @GraphQLNullable
    @FilterProperty
    val ims by NodeProperty<IMS>()

    @NodeRelationship(IMSIssue.PROJECT, Direction.INCOMING)
    @GraphQLDescription("The IMSIssues synced to by this project.")
    @FilterProperty
    val imsIssues by NodeSetProperty<IMSIssue>()
}