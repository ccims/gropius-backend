package gropius.model.issue.timeline

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.template.IssueType
import io.github.graphglue.model.DomainNode
import java.time.OffsetDateTime

@DomainNode
@GraphQLDescription(
    """Event representing that the type of an Issue changed.
    READ is granted if READ is granted on `issue`.
    """
)
class TypeChangedEvent(
    createdAt: OffsetDateTime, lastModifiedAt: OffsetDateTime
) : AbstractTypeChangedEvent<IssueType>(createdAt, lastModifiedAt)