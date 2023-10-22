package gropius.model.template

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.common.ExtensibleNode
import gropius.model.user.permission.NodePermission
import io.github.graphglue.model.*

@DomainNode
@GraphQLDescription(
    """Defines which InterfaceSpecifications are derived under which conditions by a Relation.
    Part of a RelationCondition, which is part of RelationTemplates.
    READ is always granted.
    """
)
@Authorization(NodePermission.READ, allowAll = true)
class InterfaceSpecificationDerivationCondition(
    @property:GraphQLDescription("If true, visible self-defined InterfaceSpecifications are derived")
    @FilterProperty
    val derivesVisibleSelfDefined: Boolean,
    @property:GraphQLDescription("If true, invisible self-defined InterfaceSpecifications are derived")
    @FilterProperty
    val derivesInvisibleSelfDefined: Boolean,
    @property:GraphQLDescription("If true, visible derived InterfaceSpecifications are derived")
    @FilterProperty
    val derivesVisibleDerived: Boolean,
    @property:GraphQLDescription("If true, invisible derived InterfaceSpecifications are derived")
    @FilterProperty
    val derivesInvisibleDerived: Boolean,
    @property:GraphQLDescription("If true InterfaceSpecifications are visible derived")
    @FilterProperty
    val isVisibleDerived: Boolean,
    @property:GraphQLDescription("If true InterfaceSpecifications are invisible derived")
    @FilterProperty
    val isInvisibleDerived: Boolean
) : ExtensibleNode() {

    companion object {
        const val PART_OF = "PART_OF"
        const val DERIVABLE_INTERFACE_SPECIFICATION = "DERIVABLE_INTERFACE_SPECIFICATION"
    }

    @NodeRelationship(PART_OF, Direction.OUTGOING)
    @GraphQLDescription("The RelationCondition this is part of.")
    @FilterProperty
    val partOf by NodeProperty<RelationCondition>()

    @NodeRelationship(DERIVABLE_INTERFACE_SPECIFICATION, Direction.OUTGOING)
    @GraphQLDescription("Templates of InterfaceSpecifications which are derived.")
    @FilterProperty
    val derivableInterfaceSpecifications by NodeSetProperty<InterfaceSpecificationTemplate>()
}