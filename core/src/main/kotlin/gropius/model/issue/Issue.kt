package gropius.model.issue

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import gropius.model.architecture.AffectedByIssue
import gropius.model.architecture.IMSIssue
import gropius.model.architecture.IMSProject
import gropius.model.architecture.Trackable
import gropius.model.common.AuditedNode
import gropius.model.issue.timeline.*
import gropius.model.template.*
import gropius.model.user.User
import gropius.model.user.permission.NodePermission
import gropius.model.user.permission.TrackablePermission
import io.github.graphglue.model.*
import org.springframework.data.annotation.Transient
import org.springframework.data.neo4j.core.schema.CompositeProperty
import java.time.Duration
import java.time.OffsetDateTime

@DomainNode
@GraphQLDescription(
    """An Issue in the Gropius system.
    Issues can be used to report bugs, request features, ask questions, ...
    Issues are synced to all IMSProjects of Trackables they are part of.
    All changes to the Issue are reflected by the timeline.
    READ is granted if READ is granted on any Trackable in `trackables`.
    """
)
@Authorization(NodePermission.READ, allowFromRelated = ["trackables"])
@Authorization(TrackablePermission.LINK_TO_ISSUES, allowFromRelated = ["trackables"])
@Authorization(TrackablePermission.LINK_FROM_ISSUES, allowFromRelated = ["trackables"])
@Authorization(TrackablePermission.MODERATOR, allowFromRelated = ["trackables"])
@Authorization(TrackablePermission.COMMENT, allowFromRelated = ["trackables"])
@Authorization(TrackablePermission.MANAGE_ISSUES, allowFromRelated = ["trackables"])
@Authorization(TrackablePermission.EXPORT_ISSUES, allowFromRelated = ["trackables"])
class Issue(
    createdAt: OffsetDateTime,
    lastModifiedAt: OffsetDateTime,
    @property:GraphQLIgnore
    @CompositeProperty
    override val templatedFields: MutableMap<String, String>,
    @property:GraphQLDescription("Title of the Issue, usually a short description of the Issue.")
    @FilterProperty
    @OrderProperty
    var title: String,
    @property:GraphQLDescription("The DateTime when the Issue was last updated, this includes a changed timeline.")
    @FilterProperty
    @OrderProperty
    var lastUpdatedAt: OffsetDateTime,
    @property:GraphQLDescription("DateTime when working on this Issue started / will start.")
    @FilterProperty
    @OrderProperty
    var startDate: OffsetDateTime?,
    @property:GraphQLDescription("DateTime when working on this Issue should be finished.")
    @FilterProperty
    @OrderProperty
    var dueDate: OffsetDateTime?,
    @property:GraphQLDescription("Estimated amount of time necessary for this Issue.")
    @FilterProperty
    @OrderProperty
    var estimatedTime: Duration?,
    @property:GraphQLDescription("Time spent working on this Issue.")
    @FilterProperty
    @OrderProperty
    var spentTime: Duration?
) : AuditedNode(createdAt, lastModifiedAt), MutableTemplatedNode {

    companion object {
        const val TIMELINE = "TIMELINE"
        const val ISSUE_COMMENT = "ISSUE_COMMENT"
        const val BODY = "BODY"
        const val TYPE = "TYPE"
        const val STATE = "STATE"
        const val PRIORITY = "PRIORITY"
        const val LABEL = "LABEL"
        const val ARTEFACT = "ARTEFACT"
        const val PARTICIPANT = "PARTICIPANT"
        const val INCOMING_RELATION = "INCOMING_RELATION"
        const val OUTGOING_RELATION = "OUTGOING_RELATION"
        const val ASSIGNMENT = "ASSIGNMENT"
        const val PINNED_ON = "PINNED_ON"
        const val AFFECTS = "AFFECTS"
    }

    @NodeRelationship(BaseTemplate.USED_IN, Direction.INCOMING)
    @GraphQLDescription("The Template of this Issue.")
    @FilterProperty
    @delegate:Transient
    override val template by NodeProperty<IssueTemplate>()

    @NodeRelationship(AFFECTS, Direction.OUTGOING)
    @GraphQLDescription("Entities which are in some regard affected by this Issue.")
    @FilterProperty
    @delegate:Transient
    val affects by NodeSetProperty<AffectedByIssue>()

    @NodeRelationship(Trackable.ISSUE, Direction.INCOMING)
    @GraphQLDescription("Trackables this Issue is part of.")
    @FilterProperty
    @delegate:Transient
    val trackables by NodeSetProperty<Trackable>()

    @NodeRelationship(TIMELINE, Direction.OUTGOING)
    @GraphQLDescription("Timeline of the Issue, shows how the Issue changed over time.")
    @FilterProperty
    @delegate:Transient
    val timelineItems by NodeSetProperty<TimelineItem>()

    @NodeRelationship(ISSUE_COMMENT, Direction.OUTGOING)
    @GraphQLDescription("Comments on the Issue, subset of the timeline.")
    @FilterProperty
    @delegate:Transient
    val issueComments by NodeSetProperty<IssueComment>()

    @NodeRelationship(BODY, Direction.OUTGOING)
    @GraphQLDescription("The Body of the Issue, a Comment directly associated with the Issue.")
    @FilterProperty
    @delegate:Transient
    val body by NodeProperty<Body>()

    @NodeRelationship(TYPE, Direction.OUTGOING)
    @GraphQLDescription("The type of the Issue, e.g. BUG. Allowed IssueTypes are defined by the template.")
    @FilterProperty
    @delegate:Transient
    val type by NodeProperty<IssueType>()

    @NodeRelationship(STATE, Direction.OUTGOING)
    @GraphQLDescription(
        """The state of the Issue, e.g. OPEN. Allowed IssueStates are defined by the template.
        The state also defines if this Issue is considered open or closed.
        """
    )
    @FilterProperty
    @delegate:Transient
    val state by NodeProperty<IssueState>()

    @NodeRelationship(PRIORITY, Direction.OUTGOING)
    @GraphQLDescription("The priority of the Issue, e.g. HIGH. Allowed IssuePriorities are defined by the template.")
    @FilterProperty
    @delegate:Transient
    val priority by NodeProperty<IssuePriority?>()

    @NodeRelationship(LABEL, Direction.OUTGOING)
    @GraphQLDescription("Labels currently assigned to the Issue. For the history, see timelineItems.")
    @FilterProperty
    @delegate:Transient
    val labels by NodeSetProperty<Label>()

    @NodeRelationship(ARTEFACT, Direction.OUTGOING)
    @GraphQLDescription("Artefacts currently assigned to the Issue. For the history, see timelineItems.")
    @FilterProperty
    @delegate:Transient
    val artefacts by NodeSetProperty<Artefact>()

    @NodeRelationship(PARTICIPANT, Direction.OUTGOING)
    @GraphQLDescription("Users who participated on the Issue, e.g. commented, added Labels, ...")
    @FilterProperty
    @delegate:Transient
    val participants by NodeSetProperty<User>()

    @NodeRelationship(INCOMING_RELATION, Direction.OUTGOING)
    @GraphQLDescription("Current IssueRelations which have this Issue as end point.")
    @FilterProperty
    @delegate:Transient
    val incomingRelations by NodeSetProperty<IssueRelation>()

    @NodeRelationship(OUTGOING_RELATION, Direction.OUTGOING)
    @GraphQLDescription("Current IssueRelations which have this Issue as start point.")
    @FilterProperty
    @delegate:Transient
    val outgoingRelations by NodeSetProperty<IssueRelation>()

    @NodeRelationship(ASSIGNMENT, Direction.OUTGOING)
    @GraphQLDescription("Current Assignments to this Issue. For the history, see timelineItems.")
    @FilterProperty
    @delegate:Transient
    val assignments by NodeSetProperty<Assignment>()

    @NodeRelationship(PINNED_ON, Direction.OUTGOING)
    @GraphQLDescription("Trackables this Issue is currently pinned on. For the history, see timelineItems.")
    @FilterProperty
    @delegate:Transient
    val pinnedOn by NodeSetProperty<Trackable>()

    @NodeRelationship(IMSIssue.ISSUE, Direction.INCOMING)
    @GraphQLDescription(
        """Descriptions of each IMSProject this issue is synced to containing information specified by the sync
        """
    )
    @FilterProperty
    @delegate:Transient
    val imsIssues by NodeSetProperty<IMSIssue>()
}