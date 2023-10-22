package gropius.model.issue.timeline

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.issue.Label
import io.github.graphglue.model.Direction
import io.github.graphglue.model.DomainNode
import io.github.graphglue.model.FilterProperty
import io.github.graphglue.model.NodeRelationship
import java.time.OffsetDateTime

@DomainNode
@GraphQLDescription(
    """Event representing that a Label was added to an Issue.
    READ is granted if READ is granted on `issue`.
    """
)
class AddedLabelEvent(
    createdAt: OffsetDateTime, lastModifiedAt: OffsetDateTime
) : PublicTimelineItem(createdAt, lastModifiedAt) {

    companion object {
        const val ADDED_LABEL = "ADDED_LABEL"
    }

    @NodeRelationship(ADDED_LABEL, Direction.OUTGOING)
    @GraphQLDescription("The Label added to the Issue, null if deleted.")
    @FilterProperty
    val addedLabel by NodeProperty<Label?>()

}