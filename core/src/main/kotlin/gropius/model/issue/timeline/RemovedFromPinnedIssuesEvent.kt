package gropius.model.issue.timeline

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.architecture.Trackable
import io.github.graphglue.model.*
import org.springframework.data.annotation.Transient
import java.time.OffsetDateTime

@DomainNode
@GraphQLDescription(
    """Event representing that an Issue was unpinned on a Trackable.
    READ is granted if READ is granted on `issue`.
    """
)
class RemovedFromPinnedIssuesEvent(
    createdAt: OffsetDateTime, lastModifiedAt: OffsetDateTime
) : PublicTimelineItem(createdAt, lastModifiedAt) {

    companion object {
        const val UNPINNED_ON = "UNPINNED_ON"
    }

    @NodeRelationship(UNPINNED_ON, Direction.OUTGOING)
    @GraphQLDescription("The Trackable the Issue is no longer pinned on, null if deleted.")
    @FilterProperty
    val unpinnedOn by NodeProperty<Trackable?>()

}