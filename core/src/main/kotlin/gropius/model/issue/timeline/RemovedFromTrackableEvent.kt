package gropius.model.issue.timeline

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.architecture.Trackable
import io.github.graphglue.model.Direction
import io.github.graphglue.model.DomainNode
import io.github.graphglue.model.FilterProperty
import io.github.graphglue.model.NodeRelationship
import java.time.OffsetDateTime

@DomainNode
@GraphQLDescription(
    """Event representing that an Issue was removed from a Trackable.
    READ is granted if READ is granted on `issue`.
    """
)
class RemovedFromTrackableEvent(
    createdAt: OffsetDateTime, lastModifiedAt: OffsetDateTime
) : ParentTimelineItem(createdAt, lastModifiedAt) {

    companion object {
        const val REMOVED_FROM = "REMOVED_FROM"
    }

    @NodeRelationship(REMOVED_FROM, Direction.OUTGOING)
    @GraphQLDescription("The Trackable the Issue was removed from, null if deleted.")
    @FilterProperty
    val removedFromTrackable by NodeProperty<Trackable?>()

}