package gropius.model.issue.timeline

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.architecture.AffectedByIssue
import io.github.graphglue.model.*
import org.springframework.data.annotation.Transient
import java.time.OffsetDateTime

@DomainNode
@GraphQLDescription(
    """Event representing that an entity is no longer affected by an Issue.
    READ is granted if READ is granted on `issue`.
    """
)
class RemovedAffectedEntityEvent(
    createdAt: OffsetDateTime, lastModifiedAt: OffsetDateTime
) : PublicTimelineItem(createdAt, lastModifiedAt) {

    companion object {
        const val REMOVED_AFFECTED = "REMOVED_AFFECTED"
    }

    @NodeRelationship(REMOVED_AFFECTED, Direction.OUTGOING)
    @GraphQLDescription("The entity which is no longer affected by the Issue, null if deleted.")
    @FilterProperty
    val removedAffectedEntity by NodeProperty<AffectedByIssue?>()

}