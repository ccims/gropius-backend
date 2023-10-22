package gropius.model.issue

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.expediagroup.graphql.generator.annotations.GraphQLType
import gropius.model.architecture.Trackable
import gropius.model.common.NamedAuditedNode
import gropius.model.issue.timeline.AddedLabelEvent
import gropius.model.issue.timeline.RemovedLabelEvent
import gropius.model.user.permission.NodePermission
import gropius.model.user.permission.TrackablePermission
import io.github.graphglue.model.*
import java.time.OffsetDateTime

@DomainNode(searchQueryName = "searchLabels")
@GraphQLDescription(
    """Label used to mark Issues with.
    A Label consists of a name, a description and a color.
    Issues may be synced to all IMSProjects of Trackables they are part of.
    READ is granted if READ is granted on any Trackable in `trackables`.
    """
)
@Authorization(NodePermission.READ, allowFromRelated = ["trackables", "addedEvents", "removedEvents"])
@Authorization(TrackablePermission.MANAGE_LABELS, allowFromRelated = ["trackables"])
@Authorization(TrackablePermission.EXPORT_LABELS, allowFromRelated = ["trackables"])
class Label(
    createdAt: OffsetDateTime,
    lastModifiedAt: OffsetDateTime,
    name: String,
    description: String,
    @property:GraphQLDescription("The color of the Label, used to display the Label.")
    @FilterProperty
    @OrderProperty
    @GraphQLType("Color")
    var color: String
) : NamedAuditedNode(createdAt, lastModifiedAt, name, description) {

    @NodeRelationship(Trackable.LABEL, Direction.INCOMING)
    @GraphQLDescription("Trackables this Label is part of.")
    @FilterProperty
    val trackables by NodeSetProperty<Trackable>()

    @NodeRelationship(Issue.LABEL, Direction.INCOMING)
    @GraphQLDescription("Issues which currently have this Label.")
    @FilterProperty
    val issues by NodeSetProperty<Issue>()

    @NodeRelationship(AddedLabelEvent.ADDED_LABEL, Direction.INCOMING)
    @GraphQLIgnore
    val addedEvents by NodeSetProperty<AddedLabelEvent>()

    @NodeRelationship(RemovedLabelEvent.REMOVED_LABEL, Direction.INCOMING)
    @GraphQLIgnore
    val removedEvents by NodeSetProperty<RemovedLabelEvent>()

}