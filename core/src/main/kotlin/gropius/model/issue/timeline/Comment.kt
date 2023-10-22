package gropius.model.issue.timeline

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.user.User
import io.github.graphglue.model.*
import java.time.OffsetDateTime

@DomainNode
@GraphQLDescription(
    """Supertype for IssueComment and Body.
    Represents a text block in the Timeline.
    Keeps track when it was last edited and by who, but does not keep track of the change history.
    READ is granted if READ is granted on `issue`.
    """
)
abstract class Comment(
    createdAt: OffsetDateTime,
    lastModifiedAt: OffsetDateTime,
    @property:GraphQLDescription(
        """The text of the Comment.
        Supports GFM (GitHub Flavored Markdown).
        Updates to the body cause lastEditedAt and lastEditedBy to change, while updates to referencedArtefacts
        do not.
        """
    )
    @FilterProperty
    open var body: String,
    @property:GraphQLDescription(
        """Keep track when the body of the Comment was last updated.
        If not updated yet, the DateTime of creation.
        """
    )
    @FilterProperty
    @OrderProperty
    var bodyLastEditedAt: OffsetDateTime
) : PublicTimelineItem(createdAt, lastModifiedAt) {

    companion object {
        const val BODY_LAST_EDITED_BY = "BODY_LAST_EDITED_BY"
    }

    @NodeRelationship(IssueComment.ANSWERS, Direction.INCOMING)
    @GraphQLDescription("IssueComments which answer this Comment.")
    @FilterProperty
    val answeredBy by NodeSetProperty<IssueComment>()

    @NodeRelationship(BODY_LAST_EDITED_BY, Direction.OUTGOING)
    @GraphQLDescription(
        """The User who last edited the body of this Comment.
        If not edited yet, the creator of the Comment.
        """
    )
    @FilterProperty
    val bodyLastEditedBy by NodeProperty<User>()

}