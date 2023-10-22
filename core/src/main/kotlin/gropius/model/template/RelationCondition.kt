package gropius.model.template

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.common.ExtensibleNode
import gropius.model.user.permission.NodePermission
import io.github.graphglue.model.*

@DomainNode
@GraphQLDescription(
    """Condition which defines if a Relation can use a RelationTemplate.
    A relation can only use the Template, if the start of the Relation has a template in from,
    and the end of the Relation has a template in to.
    Also defines which InterfaceSpecifications are derived via the Relation.
    Part of a RelationTemplate.
    READ is always granted.
    """
)
@Authorization(NodePermission.READ, allowAll = true)
class RelationCondition : ExtensibleNode() {

    companion object {
        const val PART_OF = "PART_OF"
        const val FROM = "FROM"
        const val TO = "TO"
    }

    @NodeRelationship(InterfaceSpecificationDerivationCondition.PART_OF, Direction.INCOMING)
    @GraphQLDescription("Defines which InterfaceSpecifications are derived via the Relation.")
    @FilterProperty
    val interfaceSpecificationDerivationConditions by NodeSetProperty<InterfaceSpecificationDerivationCondition>()

    @NodeRelationship(PART_OF, Direction.OUTGOING)
    @GraphQLDescription("The RelationTemplates this is part of.")
    @FilterProperty
    val partOf by NodeSetProperty<RelationTemplate>()

    @NodeRelationship(FROM, Direction.OUTGOING)
    @GraphQLDescription("Templates of allowed start RelationPartners")
    @FilterProperty
    val from by NodeSetProperty<RelationPartnerTemplate<*, *>>()

    @NodeRelationship(TO, Direction.OUTGOING)
    @GraphQLDescription("Templates of allowed end RelationPartners")
    @FilterProperty
    val to by NodeSetProperty<RelationPartnerTemplate<*, *>>()

}