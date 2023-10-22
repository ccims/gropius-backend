package gropius.model.issue.timeline

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.issue.Artefact
import io.github.graphglue.model.Direction
import io.github.graphglue.model.DomainNode
import io.github.graphglue.model.FilterProperty
import io.github.graphglue.model.NodeRelationship
import java.time.OffsetDateTime

@DomainNode
@GraphQLDescription(
    """Event representing that an Artefact was removed from an Issue.
    READ is granted if READ is granted on `issue`.
    """
)
class RemovedArtefactEvent(
    createdAt: OffsetDateTime, lastModifiedAt: OffsetDateTime
) : PublicTimelineItem(createdAt, lastModifiedAt) {

    companion object {
        const val REMOVED_ARTEFACT = "REMOVED_ARTEFACT"
    }

    @NodeRelationship(REMOVED_ARTEFACT, Direction.OUTGOING)
    @GraphQLDescription("The Artefact which was removed from the Issue, null if deleted.")
    @FilterProperty
    val removedArtefact by NodeProperty<Artefact?>()

}