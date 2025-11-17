package gropius.model.template

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.template.style.FillStyle
import gropius.model.template.style.ShapeType
import gropius.model.template.style.StrokeStyle
import io.github.graphglue.model.*

@DomainNode
@GraphQLDescription("Template for RelationPartners.")
abstract class RelationPartnerTemplate<T, S : RelationPartnerTemplate<T, S>>(
    name: String,
    description: String,
    templateFieldSpecifications: MutableMap<String, String>,
    isDeprecated: Boolean,
    @GraphQLDescription("The corner radius of the shape, ignored for circle/ellipse.")
    var shapeRadius: Double?,
    @GraphQLDescription("The type of the shape.")
    var shapeType: ShapeType,
) : Template<T, S>(name, description, templateFieldSpecifications, isDeprecated) where T : Node, T : TemplatedNode {

    companion object {
        const val FILL_STYLE = "FILL_STYLE"
        const val STROKE_STYLE = "STROKE_STYLE"
    }


    @NodeRelationship(RelationCondition.FROM, Direction.INCOMING)
    @GraphQLDescription("RelationConditions which allow this template for the start of the relation.")
    @FilterProperty
    val possibleStartOfRelations by NodeSetProperty<RelationCondition>()

    @NodeRelationship(RelationCondition.TO, Direction.INCOMING)
    @GraphQLDescription("RelationConditions which allow this template for the end of the relation.")
    @FilterProperty
    val possibleEndOfRelations by NodeSetProperty<RelationCondition>()

    @NodeRelationship(FILL_STYLE, Direction.OUTGOING)
    @GraphQLDescription("Style of the fill")
    val fill by NodeProperty<FillStyle?>()

    @NodeRelationship(STROKE_STYLE, Direction.OUTGOING)
    @GraphQLDescription("Style of the stroke")
    val stroke by NodeProperty<StrokeStyle?>()

}