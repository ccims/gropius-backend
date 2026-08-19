package gropius.model.issue

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.common.NamedNode
import gropius.model.template.IssueState
import gropius.model.user.permission.NodePermission
import gropius.model.user.permission.TrackablePermission
import io.github.graphglue.model.*

@DomainNode(searchQueryName = "searchIssueBoardColumns")
@GraphQLDescription(
    """A column on an Issue Board.
    An Issue Board Column consists of a name, a description and a position.
    Issues are assigned to a column by their state: an Issue is shown in the column
    the Issue's state is assigned to.
    READ is granted if READ is granted on the Trackable of the Issue Board.
    """
)
@Authorization(TrackablePermission.MANAGE_ISSUE_BOARDS, allowFromRelated = ["issueBoard"])
@Authorization(NodePermission.READ, allowFromRelated = ["issueBoard"])
class IssueBoardColumn(
    name: String,
    description: String,
    @property:GraphQLDescription("Order of this column on its Issue Board")
    @FilterProperty
    @OrderProperty
    var position: Double
) : NamedNode(name, description) {
    companion object {
        const val ISSUE_STATE = "ISSUE_STATE"
    }

    @NodeRelationship(IssueBoard.ISSUE_BOARD_COLUMN, Direction.INCOMING)
    @GraphQLDescription("The Issue Board this column belongs to")
    @FilterProperty
    val issueBoard by NodeProperty<IssueBoard>()

    @NodeRelationship(ISSUE_STATE, Direction.OUTGOING)
    @GraphQLDescription("The Issue States assigned to this column")
    @FilterProperty
    val issueStates by NodeSetProperty<IssueState>()
}
