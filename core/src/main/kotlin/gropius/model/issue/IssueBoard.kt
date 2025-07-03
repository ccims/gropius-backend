package gropius.model.issue

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.architecture.Trackable
import gropius.model.common.NamedNode
import gropius.model.user.permission.NodePermission
import gropius.model.user.permission.TrackablePermission
import io.github.graphglue.model.*

@DomainNode(searchQueryName = "searchIssueBoards")
@GraphQLDescription(
    """Issue Boards used to assign Issues to.
    An Issue Board consists of a name and a description.
    Issue Boards may be synced to  IMSProjects of the Trackable they are part of.
    READ is granted if READ is granted on any Trackable in `trackable`.
    """
)
@Authorization(TrackablePermission.MANAGE_ISSUE_BOARDS, allowFromRelated = ["trackable"])
@Authorization(NodePermission.READ, allowFromRelated = ["trackable"])
class IssueBoard(name:String, description:String):NamedNode(name,description) {
    companion object {
        const val ISSUE_BOARD_COLUMN = "ISSUE_BOARD_COLUMN"
    }

    @NodeRelationship(Trackable.ISSUE_BOARD, Direction.INCOMING)
    @GraphQLDescription("Trackable this Issue Board is part of.")
    @FilterProperty
    val trackable by NodeProperty<Trackable>()


    @NodeRelationship(IssueBoardColumn.ISSUE_BOARD, Direction.INCOMING)
    @GraphQLDescription("Columns on this IssueBoard")
    @FilterProperty
    val issueBoardColumns by NodeSetProperty<IssueBoardColumn>()

    @NodeRelationship(IssueBoardItem.ISSUE_BOARD, Direction.INCOMING)
    @GraphQLDescription("Items on this IssueBoard")
    @FilterProperty
    val issueBoardItems by NodeSetProperty<IssueBoardItem>()


}