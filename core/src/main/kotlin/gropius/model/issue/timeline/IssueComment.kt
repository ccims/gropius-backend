package gropius.model.issue.timeline

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLName
import gropius.model.issue.Artefact
import io.github.graphglue.model.Direction
import io.github.graphglue.model.DomainNode
import io.github.graphglue.model.FilterProperty
import io.github.graphglue.model.NodeRelationship
import java.time.OffsetDateTime

@DomainNode
@GraphQLDescription(
    """Comment on an Issue.
    Can reference Artefacts.
    Can be deleted, if deleted, the body is set to an empty String and the referencedComments are cleared.
    Keeps track when it was last edited and by who, but does not keep track of the change history.
    READ is granted if READ is granted on `issue`.
    """
)
class IssueComment(
    createdAt: OffsetDateTime,
    lastModifiedAt: OffsetDateTime,
    body: String,
    bodyLastEditedAt: OffsetDateTime,
    @property:GraphQLDescription("If true, the IssueComment was deleted and the body is no longer visible.")
    @GraphQLName("isDeleted")
    @FilterProperty
    var isCommentDeleted: Boolean
) : Comment(createdAt, lastModifiedAt, body, bodyLastEditedAt) {

    companion object {
        const val ANSWERS = "ANSWERS"
        const val REFERENCED_ARTEFACT = "REFERENCED_ARTEFACT"
    }

    @GraphQLDescription(
        """The text of the Comment.
        Supports GFM (GitHub Flavored Markdown).
        Updates to the body cause lastEditedAt and lastEditedBy to change, while updates to referencedArtefacts
        do not.
        Empty String if IssueComment is deleted.
        """
    )
    @FilterProperty
    override var body: String
        get() {
            return if (isCommentDeleted) {
                ""
            } else {
                super.body
            }
        }
        set(value) {
            super.body = value
        }

    @NodeRelationship(ANSWERS, Direction.OUTGOING)
    @GraphQLDescription("The Comment this IssueComment is an answers to.")
    @FilterProperty
    val answers by NodeProperty<Comment?>()

    @NodeRelationship(REFERENCED_ARTEFACT, Direction.OUTGOING)
    @GraphQLDescription("Referenced Artefacts. Changes to not cause lastEditedAt/lastEditedBy to change.")
    @FilterProperty
    val referencedArtefacts by NodeSetProperty<Artefact>()

}