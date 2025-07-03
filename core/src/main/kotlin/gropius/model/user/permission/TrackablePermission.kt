package gropius.model.user.permission

import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import gropius.model.architecture.AffectedByIssue
import gropius.model.architecture.IMSProject
import gropius.model.architecture.InterfaceSpecification
import gropius.model.architecture.Trackable
import gropius.model.issue.Artefact
import gropius.model.issue.Issue
import gropius.model.issue.IssueBoard
import gropius.model.issue.IssueBoardColumn
import gropius.model.issue.Label
import gropius.model.issue.timeline.Comment
import io.github.graphglue.model.DomainNode

/**
 * Common base class for [ComponentPermission] and [ProjectPermission]
 *
 * @param T the type of Node which is affected by this permission
 * @param entries the granted permission entries as Strings
 * @param allUsers if true, the permission is granted to all users
 */
@DomainNode
@GraphQLIgnore
abstract class TrackablePermission<T : Trackable>(
    name: String, description: String, entries: MutableList<String>, allUsers: Boolean
) : NodePermission<T>(name, description, entries, allUsers) {

    companion object {
        /**
         * Permission to check if the user can edit the [IMSProject]s of the [Trackable]
         */
        const val MANAGE_IMS = "MANAGE_IMS"

        /**
         * Permission to check if the user can create [Issue]s on the [Trackable]
         * Includes adding already existing [Issue]s
         */
        const val CREATE_ISSUES = "CREATE_ISSUES"

        /**
         * Permission to check if the user can affect entities part of this [Trackable],
         * e.g. the [Trackable] itself or [InterfaceSpecification]s with [Issue]s
         */
        const val AFFECT_ENTITIES_WITH_ISSUES = "AFFECT_ENTITIES_WITH_ISSUES"

        /**
         * Permission to check if the user can moderate [Issue]s on the [Trackable]
         * This allows everything [MANAGE_ISSUES] allows.
         * Additionally, it allows editing and deleting [Comment]s of other users
         */
        const val MODERATOR = "MODERATOR"

        /**
         * Permission to check if the user can comment on existing [Issue]s on the [Trackable]
         * Also allows editing of [Comment]s the user created.
         */
        const val COMMENT = "COMMENT"

        /**
         * Permission to check if the user can manage (add, remove, update) [Label]s on the [Trackable].
         * Note: to delete a [Label], the user has to be able to remove it from all [Trackable]s
         * which currently use the [Label]
         */
        const val MANAGE_LABELS = "MANAGE_LABELS"

        /**
         * Permission to check if the user can manage (add, remove, update) [IssueBoard]s on the [Trackable].
         * Note: to delete an [IssueBoard], the user has to be able to remove it from all [Trackable]s
         * which currently use the [IssueBoard]
         */
        const val MANAGE_ISSUE_BOARDS = "MANAGE_ISSUE_BOARDS"

        /**
         * Permission to check if the user can manage (add, remove, update) [Artefact]s on the [Trackable]
         */
        const val MANAGE_ARTEFACTS = "MANAGE_ARTEFACTS"

        /**
         * Permission to check if the user can manage [Issue]s.
         * This includes `CREATE_ISSUES` and `COMMENT`.
         * This does NOT include `LINK_TO_ISSUES` and `LINK_FROM_ISSUES`.
         * This includes
         * - change the Template
         * - add/remove [Label]s
         * - add/remove [Artefact]s
         * - change any fields on the [Issue] (startDate, dueDate, title, ...)
         * - change templated fields
         * In contrast to [MODERATOR], this does not allow editing/removing [Comment]s of other users
         */
        const val MANAGE_ISSUES = "MANAGE_ISSUES"

        /**
         * Permission to check if the user can add [Issue]s on the [Trackable] to other [Trackable]s
         */
        const val EXPORT_ISSUES = "EXPORT_ISSUES"

        /**
         * Permission to check if the user can add [Label]s on the [Trackable] to other [Trackable]s
         */
        const val EXPORT_LABELS = "EXPORT_LABELS"

        /**
         * Used to track [AffectedByIssue] entities relative to a trackable
         * Not used for permission checking, rather for filtering
         */
        const val RELATED_ISSUE_AFFECTED_ENTITY = "RELATED_ISSUE_AFFECTED_ENTITY"
    }

}