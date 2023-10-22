package gropius.model.issue.timeline

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.user.permission.NodePermission
import io.github.graphglue.model.Authorization
import io.github.graphglue.model.DomainNode
import java.time.OffsetDateTime

@DomainNode
@GraphQLDescription(
    """Event representing that the type of an incoming IssueRelation changed.
    READ is granted if READ is granted on `issue`.
    """
)
@Authorization(NodePermission.READ, allowFromRelated = ["issue"])
class OutgoingRelationTypeChangedEvent(
    createdAt: OffsetDateTime, lastModifiedAt: OffsetDateTime,
) : RelationTypeChangedEvent(createdAt, lastModifiedAt)