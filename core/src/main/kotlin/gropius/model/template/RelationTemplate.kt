package gropius.model.template

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.architecture.Relation
import gropius.model.template.style.MarkerType
import gropius.model.template.style.StrokeStyle
import io.github.graphglue.model.Direction
import io.github.graphglue.model.DomainNode
import io.github.graphglue.model.FilterProperty
import io.github.graphglue.model.NodeRelationship

@DomainNode("relationTemplates", searchQueryName = "searchRelationTemplates")
@GraphQLDescription(
    """Template for Relations.
    Defines templated fields with specific types (defined using JSON schema).
    Defines which Relations can use this Template.
    At least one RelationCondition has to match.
    """
)
class RelationTemplate(
    name: String,
    description: String,
    templateFieldSpecifications: MutableMap<String, String>,
    isDeprecated: Boolean,
    @GraphQLDescription("The type of the marker at the end of the relation.")
    var markerType: MarkerType,
) : Template<Relation, RelationTemplate>(name, description, templateFieldSpecifications, isDeprecated) {

    companion object {
        const val STROKE_STYLE = "STROKE_STYLE"
    }

    @NodeRelationship(RelationCondition.PART_OF, Direction.INCOMING)
    @GraphQLDescription("Defines which Relations can use this template, at least one RelationCondition has to match")
    @FilterProperty
    val relationConditions by NodeSetProperty<RelationCondition>()

    @NodeRelationship(STROKE_STYLE, Direction.OUTGOING)
    @GraphQLDescription("Style of the stroke")
    val stroke by NodeProperty<StrokeStyle?>()

}