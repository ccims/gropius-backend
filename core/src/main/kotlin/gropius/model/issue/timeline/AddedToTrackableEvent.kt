package gropius.model.issue.timeline

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.architecture.Trackable
import io.github.graphglue.model.*
import org.springframework.data.annotation.Transient
import java.time.OffsetDateTime

@DomainNode
@GraphQLDescription(
    """Event representing that the Issue was added to a Trackable.
    READ is granted if READ is granted on `issue`.
    """
)
class AddedToTrackableEvent(
    createdAt: OffsetDateTime, lastModifiedAt: OffsetDateTime
) : PublicTimelineItem(createdAt, lastModifiedAt) {

    companion object {
        const val ADDED_TO = "ADDED_TO"
    }

    @NodeRelationship(ADDED_TO, Direction.OUTGOING)
    @GraphQLDescription("The Trackable the Issue was added to, null if deleted.")
    @FilterProperty
    val addedToTrackable by NodeProperty<Trackable?>()

}