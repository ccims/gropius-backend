package gropius.model.template

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.common.NamedNode
import gropius.model.issue.Issue
import gropius.model.user.permission.NodePermission
import io.github.graphglue.model.*
import org.springframework.data.annotation.Transient

@DomainNode
@GraphQLDescription(
    """State of an Issue like BUG or FEATURE_REQUEST. Part of an IssueTemplate.
    READ is always granted.
    """
)
@Authorization(NodePermission.READ, allowAll = true)
class IssueState(
    name: String,
    description: String,
    @property:GraphQLDescription("If true and the issue has this state, the issue is considered open, otherwise closed")
    @FilterProperty
    @OrderProperty
    val isOpen: Boolean
) : NamedNode(name, description) {

    companion object {
        const val PART_OF = "PART_OF"
    }

    @NodeRelationship(Issue.STATE, Direction.INCOMING)
    @GraphQLDescription("Issues with this state.")
    @FilterProperty
    @delegate:Transient
    val issuesWithState by NodeSetProperty<Issue>()

    @NodeRelationship(PART_OF, Direction.OUTGOING)
    @GraphQLDescription("IssueTemplates this is a part of.")
    @FilterProperty
    @delegate:Transient
    val partOf by NodeSetProperty<IssueTemplate>()

}