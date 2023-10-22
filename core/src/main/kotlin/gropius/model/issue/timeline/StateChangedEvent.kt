package gropius.model.issue.timeline

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.template.IssueState
import io.github.graphglue.model.Direction
import io.github.graphglue.model.DomainNode
import io.github.graphglue.model.FilterProperty
import io.github.graphglue.model.NodeRelationship
import java.time.OffsetDateTime

@DomainNode
@GraphQLDescription(
    """Event representing that the state of an Issue changed.
    READ is granted if READ is granted on `issue`.
    """
)
class StateChangedEvent(
    createdAt: OffsetDateTime, lastModifiedAt: OffsetDateTime,
) : PublicTimelineItem(createdAt, lastModifiedAt) {

    companion object {
        const val OLD_STATE = "OLD_STATE"
        const val NEW_STATE = "NEW_STATE"
    }

    @NodeRelationship(OLD_STATE, Direction.OUTGOING)
    @GraphQLDescription("The old state.")
    @FilterProperty
    val oldState by NodeProperty<IssueState>()

    @NodeRelationship(NEW_STATE, Direction.OUTGOING)
    @GraphQLDescription("The new state.")
    @FilterProperty
    val newState by NodeProperty<IssueState>()
}