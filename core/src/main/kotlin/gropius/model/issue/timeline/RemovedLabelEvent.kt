package gropius.model.issue.timeline

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.issue.Label
import io.github.graphglue.model.*
import org.springframework.data.annotation.Transient
import java.time.OffsetDateTime

@DomainNode
@GraphQLDescription(
    """Event representing that a Label was removed from an Issue.
    READ is granted if READ is granted on `issue`.
    """
)
class RemovedLabelEvent(
    createdAt: OffsetDateTime, lastModifiedAt: OffsetDateTime
) : PublicTimelineItem(createdAt, lastModifiedAt) {

    companion object {
        const val REMOVED_LABEL = "REMOVED_LABEL"
    }

    @NodeRelationship(REMOVED_LABEL, Direction.OUTGOING)
    @GraphQLDescription("The Label removed from the Issue.")
    @GraphQLNullable
    @FilterProperty
    val removedLabel by NodeProperty<Label>()

}