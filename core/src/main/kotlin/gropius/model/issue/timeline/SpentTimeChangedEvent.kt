package gropius.model.issue.timeline

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import io.github.graphglue.model.DomainNode
import io.github.graphglue.model.FilterProperty
import java.time.Duration
import java.time.OffsetDateTime

@DomainNode
@GraphQLDescription(
    """Event representing that the spent time of an Issue changed.
    READ is granted if READ is granted on `issue`.
    """
)
class SpentTimeChangedEvent(
    createdAt: OffsetDateTime,
    lastModifiedAt: OffsetDateTime,
    @property:GraphQLDescription("The old spent time.")
    @FilterProperty
    val oldSpentTime: Duration?,
    @property:GraphQLDescription("The mew spent time.")
    @FilterProperty
    val newSpentTime: Duration?
) : PublicTimelineItem(createdAt, lastModifiedAt)