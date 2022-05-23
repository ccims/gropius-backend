package gropius.model.issue

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.architecture.Trackable
import gropius.model.common.AuditedNode
import gropius.model.issue.timeline.IssueComment
import gropius.model.user.permission.NodePermission
import io.github.graphglue.model.*
import org.springframework.data.annotation.Transient
import java.net.URI
import java.time.OffsetDateTime

@DomainNode
@GraphQLDescription(
    """An Artefact referencing a file defined via a URL.
    Can optionally specify a line range (from - to), and a version.
    Is part of exactly one Trackable.
    Can be referenced by Comments and Issues.
    Artefacts are synced to all IMSProjects of the Trackable they are part of.
    READ is granted if READ is granted on `trackable`.
    """
)
@Authorization(NodePermission.READ, allowFromRelated = ["trackable"])
class Artefact(
    createdAt: OffsetDateTime,
    lastModifiedAt: OffsetDateTime,
    @property:GraphQLDescription("A URL to the file this Artefact references")
    @FilterProperty
    @OrderProperty
    var file: URI,
    @property:GraphQLDescription("If present, the first line of the file this Artefact references, inclusive")
    @FilterProperty
    @OrderProperty
    var from: Int?,
    @property:GraphQLDescription("If present, the last line of the file this Artefact references, inclusive")
    @FilterProperty
    @OrderProperty
    var to: Int?,
    @property:GraphQLDescription("If present, the current version of this Artefact")
    @FilterProperty
    @OrderProperty
    var version: String?
) : AuditedNode(createdAt, lastModifiedAt) {

    @NodeRelationship(Trackable.ARTEFACT, Direction.INCOMING)
    @GraphQLDescription("The Trackable this Artefact is part of.")
    @FilterProperty
    @delegate:Transient
    val trackable by NodeProperty<Trackable>()

    @NodeRelationship(Issue.ARTEFACT, Direction.INCOMING)
    @GraphQLDescription("Issues which currently have this Artefact.")
    @FilterProperty
    @delegate:Transient
    val issues by NodeSetProperty<Issue>()

    @NodeRelationship(IssueComment.REFERENCED_ARTEFACT, Direction.INCOMING)
    @GraphQLDescription("IssueComments which currently reference this Artefact.")
    @FilterProperty
    @delegate:Transient
    val referencingComments by NodeSetProperty<IssueComment>()

}