package gropius.model.issue.timeline

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.template.IssuePriority
import io.github.graphglue.model.Direction
import io.github.graphglue.model.DomainNode
import io.github.graphglue.model.FilterProperty
import io.github.graphglue.model.NodeRelationship
import java.time.OffsetDateTime

@DomainNode
@GraphQLDescription(
    """Event representing that the priority of an Issue changed.
    READ is granted if READ is granted on `issue`.
    """
)
class PriorityChangedEvent(
    createdAt: OffsetDateTime,
    lastModifiedAt: OffsetDateTime,
) : PublicTimelineItem(createdAt, lastModifiedAt) {

    companion object {
        const val OLD_PRIORITY = "OLD_PRIORITY"
        const val NEW_PRIORITY = "NEW_PRIORITY"
    }

    @NodeRelationship(OLD_PRIORITY, Direction.OUTGOING)
    @GraphQLDescription("The old priority.")
    @FilterProperty
    val oldPriority by NodeProperty<IssuePriority?>()

    @NodeRelationship(NEW_PRIORITY, Direction.OUTGOING)
    @GraphQLDescription("The new priority.")
    @FilterProperty
    val newPriority by NodeProperty<IssuePriority?>()
}