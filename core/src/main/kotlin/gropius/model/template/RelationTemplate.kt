package gropius.model.template

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.architecture.Relation
import io.github.graphglue.model.Direction
import io.github.graphglue.model.DomainNode
import io.github.graphglue.model.FilterProperty
import io.github.graphglue.model.NodeRelationship

@DomainNode("relationTemplates")
@GraphQLDescription(
    """Template for Relations.
    Defines templated fields with specific types (defined using JSON schema).
    Defines which Relations can use this Template.
    At least one RelationCondition has to match.
    """
)
class RelationTemplate(
    name: String, description: String, templateFieldSpecifications: MutableMap<String, String>, isDeprecated: Boolean
) : Template<Relation, RelationTemplate>(name, description, templateFieldSpecifications, isDeprecated) {

    @NodeRelationship(RelationCondition.PART_OF, Direction.INCOMING)
    @GraphQLDescription("Defines which Relations can use this template, at least one RelationCondition has to match")
    @FilterProperty
    val relationConditions by NodeSetProperty<RelationCondition>()

}