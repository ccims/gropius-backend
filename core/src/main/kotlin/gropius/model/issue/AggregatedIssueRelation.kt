package gropius.model.issue

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.common.BaseNode
import gropius.model.issue.timeline.IssueRelation
import gropius.model.template.IssueRelationType
import gropius.model.user.permission.NodePermission
import io.github.graphglue.model.*

@DomainNode
@GraphQLDescription(
    """An aggregated IssueRelation.
    IssueRelations are aggregated by both start and end Issue.
    """
)
@Authorization(NodePermission.READ, allowFromRelated = ["start"])
class AggregatedIssueRelation(var count: Int) : BaseNode() {

    companion object {
        const val ISSUE_RELATION = "ISSUE_RELATION"
        const val TYPE = "TYPE"
    }

    @NodeRelationship(AggregatedIssue.OUTGOING_RELATION, Direction.INCOMING)
    @GraphQLDescription("The start of this AggregatedIssueRelation.")
    @FilterProperty
    val start by NodeProperty<AggregatedIssue>()

    @NodeRelationship(AggregatedIssue.INCOMING_RELATION, Direction.INCOMING)
    @GraphQLDescription("The end of this AggregatedIssueRelation.")
    @FilterProperty
    val end by NodeProperty<AggregatedIssue>()

    @NodeRelationship(TYPE, Direction.OUTGOING)
    @GraphQLDescription("The IssueType of this AggregatedIssue.")
    @FilterProperty
    val type by NodeProperty<IssueRelationType?>()

    @NodeRelationship(ISSUE_RELATION, Direction.OUTGOING)
    @GraphQLDescription("The IssueRelations aggregated by this AggregatedIssueRelation.")
    @FilterProperty
    val issueRelations by NodeSetProperty<IssueRelation>()

}