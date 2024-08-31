package gropius.dto.input.architecture.layout

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.dto.input.common.Input
import gropius.model.architecture.layout.Point
import gropius.model.architecture.layout.SegmentLayout

@GraphQLDescription("Input which defines the layout of a Relation")
class RelationLayoutInput(
    @GraphQLDescription("List of intermediate points of the relation")
    val points: List<Point>,
    @GraphQLDescription("The layout of the segments of the relation, must contain exactly one more element than points")
    val segments: List<SegmentLayout>
) : Input() {

    override fun validate() {
        super.validate()
        if (points.size != segments.size - 1) {
            throw IllegalArgumentException("The segments list must contain exactly one more element than the points list")
        }
    }

}