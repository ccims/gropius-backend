package gropius.model.architecture.layout

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import gropius.model.architecture.Project
import gropius.model.architecture.Relation
import gropius.model.common.BaseNode
import gropius.model.user.permission.NodePermission
import io.github.graphglue.model.*

@DomainNode
@GraphQLDescription("Layout for a Relation")
@Authorization(
    NodePermission.READ,
    allowFromRelated = ["project", "view"]
)
class RelationLayout(
    @property:GraphQLIgnore
    var xCoordinates: IntArray,
    @property:GraphQLIgnore
    var yCoordinates: IntArray
) : BaseNode() {

    @NodeRelationship(Relation.LAYOUT, Direction.INCOMING)
    @GraphQLDescription("The Relation this layout is for.")
    @FilterProperty
    val relation by NodeProperty<Relation>()

    @NodeRelationship(Project.RELATION, Direction.OUTGOING)
    @GraphQLDescription("The project this layout is for, mutually exclusive with view.")
    @FilterProperty
    val project by NodeProperty<Project?>()

    @NodeRelationship(View.RELATION, Direction.OUTGOING)
    @GraphQLDescription("The view this layout is for, mutually exclusive with project.")
    @FilterProperty
    val view by NodeProperty<View?>()

    @GraphQLDescription("The intermediate points of the Relation in the layout.")
    val points get() = xCoordinates.zip(yCoordinates).map { Point(it.first, it.second) }

}