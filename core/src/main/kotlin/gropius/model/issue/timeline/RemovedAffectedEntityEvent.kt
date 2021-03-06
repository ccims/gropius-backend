package gropius.model.issue.timeline

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.architecture.AffectedByIssue
import io.github.graphglue.model.*
import org.springframework.data.annotation.Transient
import java.time.OffsetDateTime

@DomainNode
@GraphQLDescription("Event representing that an entity is no longer affected by an Issue.")
class RemovedAffectedEntityEvent(
    createdAt: OffsetDateTime, lastModifiedAt: OffsetDateTime
) : TimelineItem(createdAt, lastModifiedAt) {

    companion object {
        const val REMOVED_AFFECTED = "REMOVED_AFFECTED"
    }

    @NodeRelationship(REMOVED_AFFECTED, Direction.OUTGOING)
    @GraphQLDescription("The entity which is no longer affected by the Issue.")
    @GraphQLNullable
    @FilterProperty
    @delegate:Transient
    val removedAffectedEntity by NodeProperty<AffectedByIssue>()

}