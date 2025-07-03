package gropius.model.issue

import gropius.model.common.BaseNode
import gropius.model.common.NamedNode
import gropius.model.user.permission.NodePermission
import gropius.model.user.permission.TrackablePermission
import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.common.AuditedNode
import io.github.graphglue.model.Authorization
import io.github.graphglue.model.DomainNode
import io.github.graphglue.model.*
import java.time.OffsetDateTime

@DomainNode()
@GraphQLDescription(
    """
    An item in an IssueBoardColumn pointing to an Issue.
    position determines its order in the column.
    READ is granted if READ is granted on the parent Trackable on the Issue Board.
    """
)
@Authorization(TrackablePermission.MANAGE_ISSUE_BOARDS, allowFromRelated = ["column"])
@Authorization(NodePermission.READ, allowFromRelated = ["column"])
class IssueBoardItem(
    @GraphQLDescription("Order of this item in its column")
    @FilterProperty
    var position: Double
):BaseNode() {

    companion object {
        const val ISSUE_BOARD = "ISSUE_BOARD"
        const val ISSUE_BOARD_COLUMN = "ISSUE_BOARD_COLUMN"
        const val ISSUE = "ISSUE"
    }

    @NodeRelationship(ISSUE_BOARD, Direction.OUTGOING)
    @GraphQLDescription("The Issue Board  this board item belongs to")
    @FilterProperty
   val issueBoard by NodeProperty<IssueBoard>()

    @NodeRelationship(Issue.ISSUE_BOARD_ITEM, Direction.INCOMING)
    @GraphQLDescription("The Issue represented by this board item")
    @FilterProperty
    val issue by NodeProperty<Issue>()
}