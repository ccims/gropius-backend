package gropius.model.issue

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.common.NamedNode
import gropius.model.template.IssueState
import gropius.model.user.permission.NodePermission
import gropius.model.user.permission.TrackablePermission
import io.github.graphglue.model.Authorization
import io.github.graphglue.model.DomainNode
import io.github.graphglue.model.*

@DomainNode(searchQueryName = "searchIssueBoardColumns")
@GraphQLDescription(
    """A column on an Issue Board.
    An Issue Board Column consists of a name and a description.
    An Issue Board Column is assigned at least 1 Issue State.
    READ is granted if READ is granted on any Trackable in `trackable`.
    """
)
@Authorization(TrackablePermission.MANAGE_ISSUE_BOARDS, allowFromRelated = ["issueBoard"])
@Authorization(NodePermission.READ, allowFromRelated = ["issueBoard"])
class IssueBoardColumn(name:String,description:String): NamedNode(name,description){

    companion object {
        const val ISSUE_BOARD = "ISSUE_BOARD"
        const val ISSUE_STATE= "ISSUE_STATE"
        const val ISSUE_BOARD_ITEM = "ISSUE_BOARD_ITEM"

    }

    @NodeRelationship(IssueBoard.ISSUE_BOARD_COLUMN, Direction.INCOMING)
    @GraphQLDescription("The Issue Board this column belongs to")
    @FilterProperty
    val issueBoard by NodeProperty<IssueBoard>()

    @NodeRelationship(IssueState.ISSUE_BOARD_COLUMN, Direction.INCOMING)
    @GraphQLDescription("The Issue States assigned to this column")
    @FilterProperty
    val issueStates by NodeSetProperty<IssueState>()

    @NodeRelationship(IssueBoardItem.ISSUE_BOARD_COLUMN, Direction.INCOMING)
    @GraphQLDescription("The Issue Board Items assigned to this column")
    @FilterProperty
    val issueBoardItems by NodeSetProperty<IssueBoardItem>()

}