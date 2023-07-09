package gropius.model.issue.timeline

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.architecture.AffectedByIssue
import io.github.graphglue.model.*
import org.springframework.data.annotation.Transient
import java.time.OffsetDateTime

@DomainNode
@GraphQLDescription(
    """Event representing that an entity is affected by an Issue.
    READ is granted if READ is granted on `issue`.
    """
)
class AddedAffectedEntityEvent(
    createdAt: OffsetDateTime, lastModifiedAt: OffsetDateTime
) : PublicTimelineItem(createdAt, lastModifiedAt) {

    companion object {
        const val ADDED_AFFECTED = "ADDED_AFFECTED"
    }

    @NodeRelationship(ADDED_AFFECTED, Direction.OUTGOING)
    @GraphQLDescription("The entity affected by the Issue, null if deleted.")
    @FilterProperty
    val addedAffectedEntity by NodeProperty<AffectedByIssue?>()

}