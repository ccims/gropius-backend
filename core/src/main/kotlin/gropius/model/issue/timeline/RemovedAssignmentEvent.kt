package gropius.model.issue.timeline

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import io.github.graphglue.model.Direction
import io.github.graphglue.model.DomainNode
import io.github.graphglue.model.FilterProperty
import io.github.graphglue.model.NodeRelationship
import java.time.OffsetDateTime

@DomainNode
@GraphQLDescription(
    """Event representing that a User was unassigned from an Issue, 
    meaning an Assignment was removed from an Issue.
    READ is granted if READ is granted on `issue`.
    """
)
class RemovedAssignmentEvent(
    createdAt: OffsetDateTime, lastModifiedAt: OffsetDateTime
) : PublicTimelineItem(createdAt, lastModifiedAt) {

    companion object {
        const val REMOVED_ASSIGNMENT = "REMOVED_ASSIGNMENT"
    }

    @NodeRelationship(REMOVED_ASSIGNMENT, Direction.OUTGOING)
    @GraphQLDescription("The removed Assignment.")
    @FilterProperty
    val removedAssignment by NodeProperty<Assignment>()

}