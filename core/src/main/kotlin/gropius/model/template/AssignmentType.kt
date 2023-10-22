package gropius.model.template

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.common.NamedNode
import gropius.model.issue.timeline.Assignment
import gropius.model.user.permission.NodePermission
import io.github.graphglue.model.*

@DomainNode(searchQueryName = "searchAssignmentTypes")
@GraphQLDescription(
    """Type for an Assignment, like REVIEWER. Part of an IssueTemplate.
    READ is always granted.
    """
)
@Authorization(NodePermission.READ, allowAll = true)
class AssignmentType(name: String, description: String) : NamedNode(name, description) {

    companion object {
        const val PART_OF = "PART_OF"
    }

    @NodeRelationship(Assignment.TYPE, Direction.INCOMING)
    @GraphQLDescription("Assignments which use this type.")
    @FilterProperty
    val assignmentsWithType by NodeSetProperty<Assignment>()

    @NodeRelationship(PART_OF, Direction.OUTGOING)
    @GraphQLDescription("IssueTemplates this is part of.")
    @FilterProperty
    val partOf by NodeSetProperty<IssueTemplate>()

}