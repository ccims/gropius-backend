package gropius.model.issue

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.common.BaseNode
import gropius.model.user.permission.NodePermission
import gropius.model.user.permission.TrackablePermission
import io.github.graphglue.model.*

@DomainNode()
@GraphQLDescription(
    """
    An item in an IssueBoardColumn pointing to an Issue.
    position determines its order in the column.
    READ is granted if READ is granted on the parent Trackable on the Issue Board.
    """
)
@Authorization(TrackablePermission.MANAGE_ISSUE_BOARDS, allowFromRelated = ["issueBoard"])
@Authorization(NodePermission.READ, allowFromRelated = ["issueBoard"])
class IssueBoardItem(
    @GraphQLDescription("Order of this item in its column")
    @FilterProperty
    var position: Double
) : BaseNode() {


    @NodeRelationship(IssueBoard.ISSUE_BOARD_ITEM, Direction.INCOMING)
    @GraphQLDescription("The Issue Board this board item belongs to")
    @FilterProperty
    val issueBoard by NodeProperty<IssueBoard>()

    @NodeRelationship(Issue.ISSUE_BOARD_ITEM, Direction.INCOMING)
    @GraphQLDescription("The Issue represented by this board item")
    @FilterProperty
    val issue by NodeProperty<Issue>()
}