package gropius.model.issue.timeline

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import io.github.graphglue.model.DomainNode
import io.github.graphglue.model.FilterProperty
import java.time.Duration
import java.time.OffsetDateTime

@DomainNode
@GraphQLDescription(
    """Event representing that the estimated time of an Issue changed.
    READ is granted if READ is granted on `issue`.
    """
)
class EstimatedTimeChangedEvent(
    createdAt: OffsetDateTime,
    lastModifiedAt: OffsetDateTime,
    @property:GraphQLDescription("The old estimated time of the Issue.")
    @FilterProperty
    val oldEstimatedTime: Duration?,
    @property:GraphQLDescription("The new estimated time of the Issue.")
    @FilterProperty
    val newEstimatedTime: Duration?
) : PublicTimelineItem(createdAt, lastModifiedAt)