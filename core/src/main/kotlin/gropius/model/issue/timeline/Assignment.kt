package gropius.model.issue.timeline

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.template.AssignmentType
import gropius.model.user.User
import io.github.graphglue.model.Direction
import io.github.graphglue.model.DomainNode
import io.github.graphglue.model.FilterProperty
import io.github.graphglue.model.NodeRelationship
import java.time.OffsetDateTime

@DomainNode
@GraphQLDescription(
    """Event representing that a User is assigned to an Issue.
    An Assignment is only active if it is still in `assignments` on Issue.
    READ is granted if READ is granted on `issue`.
    """
)
class Assignment(createdAt: OffsetDateTime, lastModifiedAt: OffsetDateTime) : PublicTimelineItem(createdAt, lastModifiedAt) {

    companion object {
        const val TYPE = "TYPE"
        const val INITIAL_TYPE = "INITIAL_TYPE"
        const val USER = "USER"
    }

    @NodeRelationship(TYPE, Direction.OUTGOING)
    @GraphQLDescription("The type of Assignment, e.g. REVIEWER. Allowed types are defined by the IssueTemplate.")
    @FilterProperty
    val type by NodeProperty<AssignmentType?>()

    @NodeRelationship(INITIAL_TYPE, Direction.OUTGOING)
    @GraphQLDescription("The initial type of Assignment, e.g. REVIEWER. Allowed types are defined by the IssueTemplate.")
    @FilterProperty
    val initialType by NodeProperty<AssignmentType?>()

    @NodeRelationship(USER, Direction.OUTGOING)
    @GraphQLDescription("The User assigned to the Issue.")
    @FilterProperty
    val user by NodeProperty<User>()

}