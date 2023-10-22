package gropius.model.template

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import io.github.graphglue.model.*

@DomainNode
@GraphQLDescription("Template for RelationPartners.")
abstract class RelationPartnerTemplate<T, S : RelationPartnerTemplate<T, S>>(
    name: String, description: String, templateFieldSpecifications: MutableMap<String, String>, isDeprecated: Boolean
) : Template<T, S>(name, description, templateFieldSpecifications, isDeprecated) where T : Node, T : TemplatedNode {

    @NodeRelationship(RelationCondition.FROM, Direction.INCOMING)
    @GraphQLDescription("RelationConditions which allow this template for the start of the relation.")
    @FilterProperty
    val possibleStartOfRelations by NodeSetProperty<RelationCondition>()

    @NodeRelationship(RelationCondition.TO, Direction.INCOMING)
    @GraphQLDescription("RelationConditions which allow this template for the end of the relation.")
    @FilterProperty
    val possibleEndOfRelations by NodeSetProperty<RelationCondition>()

}