package gropius.model.architecture

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.authorization.RELATED_TO_NODE_PERMISSION_RULE
import gropius.model.user.permission.COMPONENT_PERMISSION_ENTRY_NAME
import gropius.model.user.permission.ComponentPermission
import gropius.model.user.permission.NodePermission
import io.github.graphglue.model.*
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import gropius.model.template.BaseTemplate
import gropius.model.template.ComponentTemplate
import gropius.model.template.MutableTemplatedNode
import org.springframework.data.annotation.Transient
import org.springframework.data.neo4j.core.schema.CompositeProperty
import java.net.URI

@DomainNode("components")
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
@Authorization(NodePermission.READ, allow = [Rule(RELATED_TO_NODE_PERMISSION_RULE)], allowFromRelated = ["versions"])
@Authorization(
    ComponentPermission.RELATE_TO_COMPONENT,
    allow = [Rule(COMPONENT_PERMISSION_ENTRY_NAME, options = [NodePermission.ADMIN])]
)
@Authorization(
    ComponentPermission.RELATE_FROM_COMPONENT,
    allow = [Rule(COMPONENT_PERMISSION_ENTRY_NAME, options = [NodePermission.ADMIN])]
)
class Component(
    name: String,
    description: String,
    repositoryURL: URI?,
    @property:GraphQLIgnore
    @CompositeProperty
    override val templatedFields: MutableMap<String, String>
) : Trackable(name, description, repositoryURL), MutableTemplatedNode {

    companion object {
        const val VERSION = "VERSION"
    }

    @NodeRelationship(BaseTemplate.USED_IN, Direction.INCOMING)
    @GraphQLDescription("The Template of this Component.")
    @FilterProperty
    @delegate:Transient
    val template by NodeProperty<ComponentTemplate>()

    @NodeRelationship(InterfaceSpecification.COMPONENT, Direction.INCOMING)
    @GraphQLDescription(
        """List of interfaces this component specifies.
        Note that visible/invisible InterfaceSpecifications are defined by a specific version of this component
        """
    )
    @FilterProperty
    @delegate:Transient
    val interfaceSpecifications by NodeSetProperty<InterfaceSpecification>()

    @NodeRelationship(VERSION, Direction.OUTGOING)
    @GraphQLDescription("Versions of this components.")
    @FilterProperty
    @delegate:Transient
    val versions by NodeSetProperty<ComponentVersion>()

    @NodeRelationship(NodePermission.NODE, Direction.INCOMING)
    @GraphQLDescription("Permissions for this Component.")
    @FilterProperty
    @delegate:Transient
    val permissions by NodeSetProperty<ComponentPermission>()

}