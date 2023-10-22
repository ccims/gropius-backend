package gropius.model.issue.timeline

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.user.permission.NodePermission
import io.github.graphglue.model.Authorization
import io.github.graphglue.model.DomainNode
import java.time.OffsetDateTime

@DomainNode
@GraphQLDescription(
    """Event representing that an incoming IssueRelation was removed.
    READ is granted if READ is granted on `removedRelation`.
    """
)
@Authorization(NodePermission.READ, allowFromRelated = ["removedRelation"])
class RemovedIncomingRelationEvent(
    createdAt: OffsetDateTime,
    lastModifiedAt: OffsetDateTime,
) : RemovedRelationEvent(createdAt, lastModifiedAt)