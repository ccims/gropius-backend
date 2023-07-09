package gropius.model.issue.timeline

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.user.permission.NodePermission
import io.github.graphglue.model.*
import org.springframework.data.annotation.Transient
import java.time.OffsetDateTime

@DomainNode
@GraphQLDescription(
    """Event representing that the Issue was used in an IssueRelation as related issue.
    The IssueRelation may not be active any more.
    READ is granted if READ is granted on `relation`.
    """
)
@Authorization(NodePermission.READ, allowFromRelated = ["relation"])
class RelatedByIssueEvent(
    createdAt: OffsetDateTime,
    lastModifiedAt: OffsetDateTime,
) : TimelineItem(createdAt, lastModifiedAt) {

    companion object {
        const val RELATION = "RELATION"
    }

    @NodeRelationship(RELATION, Direction.OUTGOING)
    @GraphQLDescription("The IssueRelation the Issue is related at, null if deleted.")
    @FilterProperty
    val relation by NodeProperty<IssueRelation?>()

}