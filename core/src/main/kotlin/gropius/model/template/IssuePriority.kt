package gropius.model.template

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.common.NamedNode
import gropius.model.issue.Issue
import gropius.model.user.permission.NodePermission
import io.github.graphglue.model.*

@DomainNode(searchQueryName = "searchIssuePriorities")
@GraphQLDescription(
    """Priority of an Issue like HIGH or LOW. Part of an IssueTemplate.
    READ is always granted.
    """
)
@Authorization(NodePermission.READ, allowAll = true)
class IssuePriority(
    name: String,
    description: String,
    @property:GraphQLDescription("The value of the IssuePriority, used to compare/order different IssuePriorities.")
    @FilterProperty
    @OrderProperty
    var value: Double
) : NamedNode(name, description) {

    companion object {
        const val PART_OF = "PART_OF"
    }

    @NodeRelationship(PART_OF, Direction.OUTGOING)
    @FilterProperty
    val partOf by NodeSetProperty<IssueTemplate>()

    @NodeRelationship(Issue.PRIORITY, Direction.INCOMING)
    @FilterProperty
    val prioritizedIssues by NodeSetProperty<Issue>()
}