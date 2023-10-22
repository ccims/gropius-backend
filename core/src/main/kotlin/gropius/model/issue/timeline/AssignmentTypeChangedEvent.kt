package gropius.model.issue.timeline

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.template.AssignmentType
import io.github.graphglue.model.Direction
import io.github.graphglue.model.DomainNode
import io.github.graphglue.model.FilterProperty
import io.github.graphglue.model.NodeRelationship
import java.time.OffsetDateTime

@DomainNode
@GraphQLDescription(
    """Event representing that the type of an Assignment changed.
    READ is granted if READ is granted on `issue`.
    """
)
class AssignmentTypeChangedEvent(
    createdAt: OffsetDateTime, lastModifiedAt: OffsetDateTime,
) : AbstractTypeChangedEvent<AssignmentType?>(createdAt, lastModifiedAt) {

    companion object {
        const val ASSIGNMENT = "ASSIGNMENT"
    }

    @NodeRelationship(ASSIGNMENT, Direction.OUTGOING)
    @GraphQLDescription("The Assignment of which the type was changed")
    @FilterProperty
    val assignment by NodeProperty<Assignment>()

}