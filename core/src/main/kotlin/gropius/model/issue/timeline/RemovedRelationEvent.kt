package gropius.model.issue.timeline

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import io.github.graphglue.model.Direction
import io.github.graphglue.model.DomainNode
import io.github.graphglue.model.FilterProperty
import io.github.graphglue.model.NodeRelationship
import org.springframework.data.annotation.Transient
import java.time.OffsetDateTime

@DomainNode
@GraphQLDescription(
    """Event representing that an outgoing IssueRelation was removed.
    """
)
abstract class RemovedRelationEvent(
    createdAt: OffsetDateTime,
    lastModifiedAt: OffsetDateTime,
) : TimelineItem(createdAt, lastModifiedAt) {

    companion object {
        const val REMOVED_RELATION = "REMOVED_RELATION"
    }

    @NodeRelationship(REMOVED_RELATION, Direction.OUTGOING)
    @GraphQLDescription("The removed IssueRelation, null if deleted.")
    @FilterProperty
    val removedRelation by NodeProperty<IssueRelation?>()

}