package gropius.model.architecture

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import gropius.model.common.ExtensibleNode
import gropius.model.template.BaseTemplate
import gropius.model.template.InterfaceDefinitionTemplate
import gropius.model.template.MutableTemplatedNode
import gropius.model.user.permission.ComponentPermission
import gropius.model.user.permission.NodePermission
import gropius.model.user.permission.TrackablePermission
import io.github.graphglue.model.*
import org.springframework.data.neo4j.core.schema.CompositeProperty

@DomainNode
@GraphQLDescription(
    """InterfaceDefinition on a ComponentVersion
    Specifies if it is visible/invisible self-defined.
    Specifies if it is visible/invisible derived (and by which Relations)
    READ is granted if READ is granted on `componentVersion`
    """
)
@Authorization(NodePermission.READ, allowFromRelated = ["componentVersion"])
@Authorization(NodePermission.ADMIN, allowFromRelated = ["componentVersion"])
@Authorization(ComponentPermission.RELATE_FROM_COMPONENT, allowFromRelated = ["componentVersion"])
@Authorization(TrackablePermission.AFFECT_ENTITIES_WITH_ISSUES, allowFromRelated = ["componentVersion"])
class InterfaceDefinition(
    @property:GraphQLDescription(
        """If true, `interfaceSpecificationVersion`is self-defined visible on the `componentVersion`"""
    )
    @FilterProperty
    @OrderProperty
    var visibleSelfDefined: Boolean,
    @property:GraphQLDescription(
        """If true, `interfaceSpecificationVersion`is self-defined invisible on the `componentVersion`"""
    )
    @FilterProperty
    @OrderProperty
    var invisibleSelfDefined: Boolean,
    @property:GraphQLIgnore
    @CompositeProperty
    override val templatedFields: MutableMap<String, String>
) : ExtensibleNode(), MutableTemplatedNode {

    companion object {
        const val VISIBLE_DERIVED_BY = "VISIBLE_DERIVED_BY"
        const val INVISIBLE_DERIVED_BY = "INVISIBLE_DERIVED_BY"
        const val INTERFACE_SPECIFICATION_VERSION = "INTERFACE_SPECIFICATION_VERSION"
        const val COMPONENT_VERSION = "COMPONENT_VERSION"
    }

    @NodeRelationship(BaseTemplate.USED_IN, Direction.INCOMING)
    @GraphQLDescription("The Template of this InterfaceDefinition.")
    @FilterProperty
    override val template by NodeProperty<InterfaceDefinitionTemplate>()

    @NodeRelationship(INTERFACE_SPECIFICATION_VERSION, Direction.OUTGOING)
    @GraphQLDescription("The InterfaceSpecificationVersion present on the ComponentVersion")
    @FilterProperty
    val interfaceSpecificationVersion by NodeProperty<InterfaceSpecificationVersion>()

    @NodeRelationship(COMPONENT_VERSION, Direction.OUTGOING)
    @GraphQLDescription("The ComponentVersion using the InterfaceSpecificationVersion")
    @FilterProperty
    val componentVersion by NodeProperty<ComponentVersion>()

    @NodeRelationship(VISIBLE_DERIVED_BY, Direction.OUTGOING)
    @GraphQLDescription(
        """Relations because of which `interfaceSpecificationVersion` is visible derived on `componentVersion`"""
    )
    @FilterProperty
    val visibleDerivedBy by NodeSetProperty<Relation>()

    @NodeRelationship(INVISIBLE_DERIVED_BY, Direction.OUTGOING)
    @GraphQLDescription(
        """Relations because of which `interfaceSpecificationVersion` is invisible derived on `componentVersion`"""
    )
    @FilterProperty
    val invisibleDerivedBy by NodeSetProperty<Relation>()

    @NodeRelationship(Interface.DEFINITION, Direction.INCOMING)
    @GraphQLDescription("If visible, the created Interface")
    @FilterProperty
    val visibleInterface by NodeProperty<Interface?>()
}