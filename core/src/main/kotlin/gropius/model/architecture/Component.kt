package gropius.model.architecture

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import gropius.authorization.RELATED_TO_NODE_PERMISSION_RULE
import gropius.model.template.BaseTemplate
import gropius.model.template.ComponentTemplate
import gropius.model.template.MutableTemplatedNode
import gropius.model.user.permission.ComponentPermission
import gropius.model.user.permission.NodePermission
import gropius.model.user.permission.NodeWithPermissions
import io.github.graphglue.model.*
import org.springframework.data.neo4j.core.schema.CompositeProperty
import java.net.URI

@DomainNode("components", searchQueryName = "searchComponents")
@GraphQLDescription(
    """Entity which represents a software component, e.g. a library, a microservice, or a deployment platform, ....
    The type of software component is defined by the template.
    Can have issues, labels and artefacts as this is a Trackable.
    Defines InterfaceSpecifications, but visible/invisible InterfaceSpecificationVersions depend on the ComponentVersion.
    Can be affected by Issues.
    READ is granted via an associated ComponentPermission or if READ is granted on any Project including any 
    ComponentVersion in `versions` of this Component.
    """
)
@Authorization(NodePermission.READ, allowFromRelated = ["versions"])
@Authorization(
    ComponentPermission.RELATE_FROM_COMPONENT,
    allow = [Rule(RELATED_TO_NODE_PERMISSION_RULE, options = [NodePermission.ADMIN])]
)
@Authorization(
    ComponentPermission.ADD_TO_PROJECTS,
    allow = [Rule(RELATED_TO_NODE_PERMISSION_RULE, options = [NodePermission.ADMIN])]
)
class Component(
    name: String,
    description: String,
    repositoryURL: URI?,
    @property:GraphQLIgnore
    @CompositeProperty
    override val templatedFields: MutableMap<String, String>
) : Trackable(name, description, repositoryURL), MutableTemplatedNode, NodeWithPermissions<ComponentPermission> {

    companion object {
        const val VERSION = "VERSION"
    }

    @NodeRelationship(BaseTemplate.USED_IN, Direction.INCOMING)
    @GraphQLDescription("The Template of this Component.")
    @FilterProperty
    @OrderProperty
    override val template by NodeProperty<ComponentTemplate>()

    @NodeRelationship(InterfaceSpecification.COMPONENT, Direction.INCOMING)
    @GraphQLDescription(
        """List of interfaces this component specifies.
        Note that visible/invisible InterfaceSpecifications are defined by a specific version of this component
        """
    )
    @FilterProperty
    val interfaceSpecifications by NodeSetProperty<InterfaceSpecification>()

    @NodeRelationship(VERSION, Direction.OUTGOING)
    @GraphQLDescription("Versions of this components.")
    @FilterProperty
    val versions by NodeSetProperty<ComponentVersion>()

    @NodeRelationship(NodePermission.NODE, Direction.INCOMING)
    @GraphQLDescription("Permissions for this Component.")
    @FilterProperty
    override val permissions by NodeSetProperty<ComponentPermission>()

}