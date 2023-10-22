package gropius.model.issue.timeline

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.user.permission.NodePermission
import io.github.graphglue.model.Authorization
import io.github.graphglue.model.DomainNode
import java.time.OffsetDateTime

@DomainNode
@GraphQLDescription(
    """Event representing that an outgoing IssueRelation was removed.
    READ is granted if READ is granted on `issue`.
    """
)
@Authorization(NodePermission.READ, allowFromRelated = ["issue"])
class RemovedOutgoingRelationEvent(
    createdAt: OffsetDateTime,
    lastModifiedAt: OffsetDateTime,
) : RemovedRelationEvent(createdAt, lastModifiedAt)