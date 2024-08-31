package gropius.model.architecture.layout

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import gropius.model.architecture.RelationPartner
import gropius.model.common.BaseNode
import io.github.graphglue.model.Direction
import io.github.graphglue.model.DomainNode
import io.github.graphglue.model.FilterProperty
import io.github.graphglue.model.NodeRelationship

@DomainNode
@GraphQLDescription("Layout for a RelationPartner (ComponentVersion or Interface)")
class RelationPartnerLayout(
    @property:GraphQLIgnore
    var x: Int,
    @property:GraphQLIgnore
    var y: Int
) : BaseNode() {

    @NodeRelationship(RelationPartner.LAYOUT, Direction.INCOMING)
    @GraphQLDescription("The RelationPartner this layout is for.")
    @FilterProperty
    val relationPartner by NodeProperty<RelationPartner>()

    @GraphQLDescription("The position of the RelationPartner in the layout, for an Interface, this is relative to the owning ComponentVersion.")
    val pos get() = Point(x, y)

}