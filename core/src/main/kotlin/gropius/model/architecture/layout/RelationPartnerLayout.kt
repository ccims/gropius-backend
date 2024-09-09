package gropius.model.architecture.layout

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import gropius.model.architecture.Project
import gropius.model.architecture.RelationPartner
import gropius.model.common.BaseNode
import gropius.model.user.permission.NodePermission
import io.github.graphglue.model.*

@DomainNode
@GraphQLDescription("Layout for a RelationPartner (ComponentVersion or Interface)")
@Authorization(
    NodePermission.READ,
    allowFromRelated = ["project", "view"]
)
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

    @NodeRelationship(Project.RELATION_PARTNER, Direction.OUTGOING)
    @GraphQLDescription("The project this layout is for, mutually exclusive with view.")
    @FilterProperty
    val project by NodeProperty<Project?>()

    @NodeRelationship(View.RELATION_PARTNER, Direction.OUTGOING)
    @GraphQLDescription("The view this layout is for, mutually exclusive with project.")
    @FilterProperty
    val view by NodeProperty<View?>()

    @GraphQLDescription("The position of the RelationPartner in the layout.")
    val pos get() = Point(x, y)

}
