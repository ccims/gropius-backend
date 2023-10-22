package gropius.model.issue.timeline

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import io.github.graphglue.model.Direction
import io.github.graphglue.model.DomainNode
import io.github.graphglue.model.FilterProperty
import io.github.graphglue.model.NodeRelationship
import java.time.OffsetDateTime

@DomainNode
@GraphQLDescription(
    """TimelineItem which can aggregate other TimelineItems.
    READ is granted if READ is granted on `issue`.
    """
)
abstract class ParentTimelineItem(
    createdAt: OffsetDateTime, lastModifiedAt: OffsetDateTime
) : TimelineItem(createdAt, lastModifiedAt) {

    companion object {
        const val CHILD_ITEMS = "CHILD_ITEMS"
    }

    @NodeRelationship(CHILD_ITEMS, Direction.OUTGOING)
    @GraphQLDescription("Child TimelineItems. These are still part of the main timeline.")
    @FilterProperty
    val childItems by NodeSetProperty<TimelineItem>()


}