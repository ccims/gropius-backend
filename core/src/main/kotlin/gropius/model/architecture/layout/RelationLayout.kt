package gropius.model.architecture.layout

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import gropius.model.architecture.Relation
import gropius.model.common.BaseNode
import io.github.graphglue.model.Direction
import io.github.graphglue.model.DomainNode
import io.github.graphglue.model.FilterProperty
import io.github.graphglue.model.NodeRelationship

@DomainNode
@GraphQLDescription("Layout for a Relation")
class RelationLayout(
    @property:GraphQLIgnore
    var xCoordinates: IntArray,
    @property:GraphQLIgnore
    var yCoordinates: IntArray,
    @property:GraphQLIgnore
    var segmentLayouts: BooleanArray
) : BaseNode() {

    @NodeRelationship(Relation.LAYOUT, Direction.INCOMING)
    @GraphQLDescription("The Relation this layout is for.")
    @FilterProperty
    val relation by NodeProperty<Relation>()

    @GraphQLDescription("The intermediate points of the Relation in the layout.")
    val points get() = xCoordinates.zip(yCoordinates).map { Point(it.first, it.second) }

    @GraphQLDescription("The layout of the segments of the Relation, always contains one more element than points.")
    val segments get() = segmentLayouts.map { SegmentLayout.from(it) }

}