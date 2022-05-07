package gropius.model.template

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.common.ExtensibleNode
import io.github.graphglue.model.Direction
import io.github.graphglue.model.DomainNode
import io.github.graphglue.model.FilterProperty
import io.github.graphglue.model.NodeRelationship
import org.springframework.data.annotation.Transient

@DomainNode
@GraphQLDescription(
    """Condition which defines if a Relation can use a RelationTemplate.
    A relation can only use the Template, if the start of the Relation has a template in from,
    and the end of the Relation has a template in to.
    Also defines which InterfaceSpecifications are inherited via the Relation.
    Part of a RelationTemplate.
    """
)
class RelationCondition : ExtensibleNode() {

    companion object {
        const val PART_OF = "PART_OF"
        const val FROM = "FROM"
        const val TO = "TO"
    }

    @NodeRelationship(InterfaceSpecificationInheritanceCondition.PART_OF, Direction.INCOMING)
    @GraphQLDescription("Defines which InterfaceSpecifications are inherited via the Relation.")
    @FilterProperty
    @delegate:Transient
    val interfaceSpecificationInheritanceConditions by NodeSetProperty<InterfaceSpecificationInheritanceCondition>()

    @NodeRelationship(PART_OF, Direction.OUTGOING)
    @GraphQLDescription("The RelationTemplates this is part of.")
    @FilterProperty
    @delegate:Transient
    val partOf by NodeSetProperty<RelationTemplate>()

    @NodeRelationship(FROM, Direction.OUTGOING)
    @GraphQLDescription("Templates of allowed start RelationPartners")
    @FilterProperty
    @delegate:Transient
    val from by NodeSetProperty<RelationPartnerTemplate<*, *>>()

    @NodeRelationship(TO, Direction.OUTGOING)
    @GraphQLDescription("Templates of allowed end RelationPartners")
    @FilterProperty
    @delegate:Transient
    val to by NodeSetProperty<RelationPartnerTemplate<*, *>>()

}