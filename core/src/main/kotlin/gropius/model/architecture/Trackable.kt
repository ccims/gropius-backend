package gropius.model.architecture

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.authorization.RELATED_TO_NODE_PERMISSION_RULE
import gropius.model.issue.Artefact
import gropius.model.issue.Issue
import gropius.model.issue.Label
import gropius.model.user.permission.NodePermission
import gropius.model.user.permission.TrackablePermission
import io.github.graphglue.model.*
import java.net.URI

@DomainNode("trackables", searchQueryName = "searchTrackables")
@GraphQLDescription(
    """An entity which can have Issues, Labels and Artefacts.
    Has pinned issues.
    Can be synced to an IMS by creating an IMSProject.
    Can be affected by Issues.
    """
)
@AggregatedNodeRelationship(
    "assignedUsers",
    "The set of Users assigned to any issue",
    "issues",
    [AggregationEntry("assignments"), AggregationEntry("user")]
)
@AggregatedNodeRelationship(
    "usedIssueTemplates",
    "The set of IssueTemplates used by any issue",
    "issues",
    [AggregationEntry("template")]
)
@AggregatedNodeRelationship(
    "usedIssueTypes",
    "The set of IssueTypes used by any issue",
    "issues",
    [AggregationEntry("type")]
)
@AggregatedNodeRelationship(
    "usedIssueStates",
    "The set of IssueStates used by any issue",
    "issues",
    [AggregationEntry("state")]
)
@AggregatedNodeRelationship(
    "usedIssuePriorities",
    "The set of IssuePriorities used by any issue",
    "issues",
    [AggregationEntry("priority")]
)
@AggregatedNodeRelationship(
    "usedLabels",
    "The set of Labels used by any issue (including Labels not on this Trackable)",
    "issues",
    [AggregationEntry("labels")]
)
@Authorization(NodePermission.READ, allow = [Rule(RELATED_TO_NODE_PERMISSION_RULE)])
@Authorization(NodePermission.ADMIN, allow = [Rule(RELATED_TO_NODE_PERMISSION_RULE)])
@Authorization(
    TrackablePermission.MANAGE_IMS, allow = [Rule(RELATED_TO_NODE_PERMISSION_RULE, options = [NodePermission.ADMIN])]
)
@Authorization(
    TrackablePermission.CREATE_ISSUES, allow = [Rule(
        RELATED_TO_NODE_PERMISSION_RULE,
        options = [NodePermission.ADMIN, TrackablePermission.MANAGE_ISSUES, TrackablePermission.MODERATOR]
    )]
)
@Authorization(
    TrackablePermission.MODERATOR, allow = [Rule(RELATED_TO_NODE_PERMISSION_RULE, options = [NodePermission.ADMIN])]
)
@Authorization(
    TrackablePermission.COMMENT, allow = [Rule(
        RELATED_TO_NODE_PERMISSION_RULE,
        options = [NodePermission.ADMIN, TrackablePermission.MANAGE_ISSUES, TrackablePermission.MODERATOR]
    )]
)
@Authorization(
    TrackablePermission.MANAGE_LABELS, allow = [Rule(RELATED_TO_NODE_PERMISSION_RULE, options = [NodePermission.ADMIN])]
)
@Authorization(
    TrackablePermission.MANAGE_ARTEFACTS,
    allow = [Rule(RELATED_TO_NODE_PERMISSION_RULE, options = [NodePermission.ADMIN])]
)
@Authorization(
    TrackablePermission.MANAGE_ISSUES, allow = [Rule(RELATED_TO_NODE_PERMISSION_RULE, options = [NodePermission.ADMIN])]
)
@Authorization(
    TrackablePermission.EXPORT_ISSUES, allow = [Rule(RELATED_TO_NODE_PERMISSION_RULE, options = [NodePermission.ADMIN])]
)
@Authorization(
    TrackablePermission.EXPORT_LABELS, allow = [Rule(RELATED_TO_NODE_PERMISSION_RULE, options = [NodePermission.ADMIN])]
)
@Authorization(
    TrackablePermission.AFFECT_ENTITIES_WITH_ISSUES,
    allow = [Rule(RELATED_TO_NODE_PERMISSION_RULE, options = [NodePermission.ADMIN])]
)
abstract class Trackable(
    name: String,
    description: String,
    @property:GraphQLDescription("If existing, the URL of the repository (e.g. a GitHub repository).")
    @FilterProperty
    var repositoryURL: URI?
) : NamedAffectedByIssue(name, description) {

    companion object {
        const val ISSUE = "ISSUE"
        const val LABEL = "LABEL"
        const val ARTEFACT = "ARTEFACT"
        const val SYNCS_TO = "SYNCS_TO"
    }

    @NodeRelationship(ISSUE, Direction.OUTGOING)
    @GraphQLDescription(
        """The set of Issues which are part of this Trackable.
        An Issue has to be part of a Trackable to use the Labels and Artefacts defined by the Trackable.
        """
    )
    @FilterProperty
    val issues by NodeSetProperty<Issue>()

    @NodeRelationship(LABEL, Direction.OUTGOING)
    @GraphQLDescription("The set of Labels which can be added to issues of this trackable.")
    @FilterProperty
    val labels by NodeSetProperty<Label>()

    @NodeRelationship(ARTEFACT, Direction.OUTGOING)
    @GraphQLDescription("Artefacts of this trackable, typically some kind of file.")
    @FilterProperty
    val artefacts by NodeSetProperty<Artefact>()

    @NodeRelationship(SYNCS_TO, Direction.OUTGOING)
    @GraphQLDescription("IMSProjects this Trackable is synced to and from.")
    @FilterProperty
    val syncsTo by NodeSetProperty<IMSProject>()

    @NodeRelationship(Issue.PINNED_ON, Direction.INCOMING)
    @GraphQLDescription("Issues which are pinned to this trackable, subset of `issues`.")
    @FilterProperty
    val pinnedIssues by NodeSetProperty<Issue>()

}