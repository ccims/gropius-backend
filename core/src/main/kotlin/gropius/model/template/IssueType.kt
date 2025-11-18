package gropius.model.template

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.common.NamedNode
import gropius.model.issue.Issue
import gropius.model.user.permission.NodePermission
import io.github.graphglue.model.*

@DomainNode(searchQueryName = "searchIssueTypes")
@GraphQLDescription(
    """Type of an Issue like BUG or FEATURE_REQUEST. Part of an IssueTemplate.
    READ is always granted.
    """
)
@Authorization(NodePermission.READ, allowAll = true)
class IssueType(
    name: String,
    description: String,
    @GraphQLDescription("A path that is used as the icon for issues. Used with a 0 0 100 100 viewBox. No stroke, only fill.")
    var iconPath: String
) : NamedNode(name, description) {

    companion object {
        const val PART_OF = "PART_OF"
    }

    @NodeRelationship(Issue.TYPE, Direction.INCOMING)
    @GraphQLDescription("Issues with this type.")
    @FilterProperty
    val issuesWithType by NodeSetProperty<Issue>()

    @NodeRelationship(PART_OF, Direction.OUTGOING)
    @GraphQLDescription("IssueTemplates this is a part of.")
    @FilterProperty
    val partOf by NodeSetProperty<IssueTemplate>()

}