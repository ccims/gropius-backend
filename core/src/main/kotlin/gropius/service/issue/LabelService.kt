package gropius.service.issue

import gropius.authorization.GropiusAuthorizationContext
import gropius.dto.input.common.DeleteNodeInput
import gropius.dto.input.ifPresent
import gropius.dto.input.issue.AddLabelToTrackableInput
import gropius.dto.input.issue.CreateLabelInput
import gropius.dto.input.issue.RemoveLabelFromTrackableInput
import gropius.dto.input.issue.UpdateLabelInput
import gropius.model.architecture.Trackable
import gropius.model.issue.Issue
import gropius.model.issue.Label
import gropius.model.user.User
import gropius.model.user.permission.TrackablePermission
import gropius.repository.architecture.TrackableRepository
import gropius.repository.findAllById
import gropius.repository.findById
import gropius.repository.issue.LabelRepository
import gropius.service.common.NamedAuditedNodeService
import io.github.graphglue.authorization.Permission
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.*

/**
 * Service for [Label]s. Provides functions to create, update and delete
 *
 * @param repository the associated repository used for CRUD functionality
 * @param trackableRepository used to find [Trackable]s by id
 * @param issueService used to remove [Label]s from [Issue]s
 */
@Service
class LabelService(
    repository: LabelRepository,
    private val trackableRepository: TrackableRepository,
    private val issueService: IssueService
) : NamedAuditedNodeService<Label, LabelRepository>(repository) {

    /**
     * Creates a new [Label] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines the [Label]
     * @return the saved created [Label]
     */
    suspend fun createLabel(authorizationContext: GropiusAuthorizationContext, input: CreateLabelInput): Label {
        input.validate()
        val trackables = trackableRepository.findAllById(input.trackables)
        for (trackable in trackables) {
            checkPermission(
                trackable,
                Permission(TrackablePermission.MANAGE_LABELS, authorizationContext),
                "create Labels on ${trackable.rawId}"
            )
        }
        val now = OffsetDateTime.now()
        val label = Label(now, now, input.name, input.description, input.color)
        label.trackables() += trackables
        createdAuditedNode(label, input, getUser(authorizationContext))
        return repository.save(label).awaitSingle()
    }

    /**
     * Updates a [Label] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [Label] to update and how
     * @return the updated [Label]
     */
    suspend fun updateLabel(authorizationContext: GropiusAuthorizationContext, input: UpdateLabelInput): Label {
        input.validate()
        val label = repository.findById(input.id)
        checkPermission(
            label,
            Permission(TrackablePermission.MANAGE_LABELS, authorizationContext),
            "manage Labels on any of its Trackables"
        )
        input.color.ifPresent {
            label.color = it
        }
        updateNamedAuditedNode(label, input, getUser(authorizationContext), OffsetDateTime.now())
        return repository.save(label).awaitSingle()
    }

    /**
     * Adds a [Label] to a [Trackable]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [Label] to add to which [Trackable]
     * @return the saved updated [Trackable]
     */
    suspend fun addLabelToTrackable(
        authorizationContext: GropiusAuthorizationContext, input: AddLabelToTrackableInput
    ): Trackable {
        input.validate()
        val label = repository.findById(input.label)
        val trackable = trackableRepository.findById(input.trackable)
        checkPermission(
            label,
            Permission(TrackablePermission.EXPORT_LABELS, authorizationContext),
            "export Labels on any of its Trackables"
        )
        checkPermission(
            trackable, Permission(TrackablePermission.MANAGE_LABELS, authorizationContext), "manage Labels"
        )
        trackable.labels() += label
        return trackableRepository.save(trackable).awaitSingle()
    }

    /**
     * Removes a [Label] from a [Trackable]
     * Deletes the [Label] if it's the last [Trackable] the [Label] was on
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [Label] to remove from which [Trackable]
     * @return the saved updated [Trackable]
     */
    suspend fun removeLabelFromTrackable(
        authorizationContext: GropiusAuthorizationContext, input: RemoveLabelFromTrackableInput
    ): Trackable {
        input.validate()
        val label = repository.findById(input.label)
        val trackable = trackableRepository.findById(input.trackable)
        checkPermission(trackable, Permission(TrackablePermission.MANAGE_LABELS, authorizationContext), "manage Labels")
        trackable.labels() -= label
        label.trackables() -= trackable
        val toRemove = label.issues().filter {
            Collections.disjoint(it.trackables(), label.trackables())
        }
        removeLabelFromIssues(label, toRemove, getUser(authorizationContext))
        return trackableRepository.save(trackable).awaitSingle()
    }

    /**
     * Removes a [Label] from all [Trackable]s by id
     * Does not actually delete it to keep it visible in issue histories
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [Label] to delete
     */
    suspend fun deleteLabel(authorizationContext: GropiusAuthorizationContext, input: DeleteNodeInput) {
        input.validate()
        val label = repository.findById(input.id)
        for (trackable in label.trackables()) {
            checkPermission(
                trackable,
                Permission(TrackablePermission.MANAGE_LABELS, authorizationContext),
                "manage Labels on a Trackable it is on"
            )
        }
        removeLabelFromIssues(label, label.issues(), getUser(authorizationContext))
        label.trackables().clear()
        repository.save(label).awaitSingle()
    }

    /**
     * Removes [label] for each [Issue] in [issues] now [byUser].
     * Saves [issues]
     *
     * @param label the [Label] to remove
     * @param issues contains [Issue]s from which [label] is removed
     * @param byUser the [User] which remove the [label]
     */
    private suspend fun removeLabelFromIssues(label: Label, issues: Collection<Issue>, byUser: User) {
        val now = OffsetDateTime.now()
        for (issue in issues.toSet()) {
            issueService.removeLabelFromIssue(issue, label, now, byUser)
        }
        issueService.repository.saveAll(issues).collectList().awaitSingle()
    }

}