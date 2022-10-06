package gropius.service.issue

import gropius.authorization.GropiusAuthorizationContext
import gropius.dto.input.issue.*
import gropius.model.architecture.Trackable
import gropius.model.issue.Artefact
import gropius.model.issue.Issue
import gropius.model.issue.Label
import gropius.model.issue.timeline.*
import gropius.model.user.User
import gropius.model.user.permission.NodePermission
import gropius.model.user.permission.TrackablePermission
import gropius.repository.architecture.TrackableRepository
import gropius.repository.findById
import gropius.repository.issue.ArtefactRepository
import gropius.repository.issue.IssueRepository
import gropius.repository.issue.LabelRepository
import gropius.repository.issue.timeline.TimelineItemRepository
import gropius.service.common.AuditedNodeService
import io.github.graphglue.authorization.Permission
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.*

/**
 * Service for [Issue]s. Provides function to update the deprecation status
 *
 * @param repository the associated repository used for CRUD functionality
 * @param labelRepository used to find [Label]s by id
 * @param timelineItemRepository used to save [TimelineItem]s
 * @param artefactRepository used to find [Artefact]s by id
 * @param trackableRepository used to find [Trackable]s by id
 */
@Service
class IssueService(
    repository: IssueRepository,
    private val labelRepository: LabelRepository,
    private val timelineItemRepository: TimelineItemRepository,
    private val artefactRepository: ArtefactRepository,
    private val trackableRepository: TrackableRepository
) : AuditedNodeService<Issue, IssueRepository>(repository) {

    /**
     * Deletes an [Issue]
     * Does not check the authorization status
     *
     * @param node the Issue to delete
     */
    suspend fun deleteIssue(node: Issue) {
        node.isDeleted = true
        repository.save(node).awaitSingle()
    }

    /**
     * Adds an [Issue] to a [Trackable], returns the created [AddedToTrackableEvent],
     * or `null` if the [Issue] was already on the [Trackable].
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [Issue] to add to which [Trackable]
     * @return the saved created [AddedToTrackableEvent] or `null` if no event was created
     */
    suspend fun addIssueToTrackable(
        authorizationContext: GropiusAuthorizationContext, input: AddIssueToTrackableInput
    ): AddedToTrackableEvent? {
        val issue = repository.findById(input.issue)
        val trackable = trackableRepository.findById(input.trackable)
        checkPermission(
            trackable,
            Permission(TrackablePermission.MANAGE_ISSUES, authorizationContext),
            "manage Issues on the Trackable"
        )
        checkPermission(issue, Permission(TrackablePermission.EXPORT_ISSUES, authorizationContext), "export the Issue")
        return if (trackable !in issue.trackables()) {
            return timelineItemRepository.save(
                addIssueToTrackable(issue, trackable, OffsetDateTime.now(), getUser(authorizationContext))
            ).awaitSingle()
        } else {
            null
        }
    }

    /**
     * Adds an [issue] to a [trackable] at [atTime] as [byUser] and adds a [AddedToTrackableEvent] to the timeline.
     * Creates the event even if the [issue] was already on the [trackable].
     * Only adds the [issue] to the [trackable] if no newer timeline item exists which removes it again.
     * Does not check the authorization status.
     * Does neither save the created [AddedToTrackableEvent] nor the [issue].
     * It is necessary to save the [issue] or returned [AddedToTrackableEvent] afterwards.
     *
     * @param issue the [Issue] to add to [trackable]
     * @param trackable the [Trackable] where the [issue] should be added
     * @param atTime the point in time when the modification happened, updates [Issue.lastUpdatedAt] if necessary
     * @param byUser the [User] who caused the update, updates [Issue.participants] if necessary
     * @return the created [AddedToTrackableEvent]
     */
    suspend fun addIssueToTrackable(
        issue: Issue, trackable: Trackable, atTime: OffsetDateTime, byUser: User
    ): AddedToTrackableEvent {
        val event = AddedToTrackableEvent(atTime, atTime)
        event.addedToTrackable().value = trackable
        createdTimelineItem(issue, event, atTime, byUser)
        if (!existsNewerTimelineItem<RemovedFromTrackableEvent>(
                issue, atTime
            ) { it.removedFromTrackable().value == trackable }
        ) {
            issue.trackables() += trackable
        }
        return event
    }

    /**
     * Removes an [Issue] from a [Trackable], returns the created [RemovedFromPinnedIssuesEvent],
     * or `null` if the [Issue] was not pinned on the [Trackable].
     * Also removes [Label]s and [Artefact]s if necessary, and unpins it on the specified [Trackable].
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [Issue] to remove from which [Trackable]
     * @return the saved created [RemovedFromTrackableEvent] or `null` if no event was created
     */
    suspend fun removeIssueFromTrackable(
        authorizationContext: GropiusAuthorizationContext, input: RemoveIssueFromTrackableInput
    ): RemovedFromTrackableEvent? {
        val issue = repository.findById(input.issue)
        val trackable = trackableRepository.findById(input.trackable)
        checkPermission(
            trackable,
            Permission(TrackablePermission.MANAGE_ISSUES, authorizationContext),
            "manage Issues on the Trackable"
        )
        return if (trackable in issue.trackables()) {
            return timelineItemRepository.save(
                removeIssueFromTrackable(issue, trackable, OffsetDateTime.now(), getUser(authorizationContext))
            ).awaitSingle()
        } else {
            null
        }
    }

    /**
     * Removes an [issue] from a [trackable] at [atTime] as [byUser] and adds a [RemovedFromTrackableEvent]
     * to the timeline.
     * Also removes [Label]s and [Artefact]s if necessary, and unpins it on the specified [Trackable].
     * Creates the event even if the [issue] was not on the [trackable].
     * Only removes the [issue] from the [trackable] if no newer timeline item exists which adds it again.
     * Does not check the authorization status.
     * Does neither save the created [RemovedFromTrackableEvent] nor the [issue].
     * It is necessary to save the [issue] or returned [RemovedFromTrackableEvent] afterwards.
     *
     * @param issue the [Issue] to remove from [trackable]
     * @param trackable the [Trackable] where [issue] should be removed
     * @param atTime the point in time when the modification happened, updates [Issue.lastUpdatedAt] if necessary
     * @param byUser the [User] who caused the update, updates [Issue.participants] if necessary
     * @return the created [RemovedFromTrackableEvent]
     */
    suspend fun removeIssueFromTrackable(
        issue: Issue, trackable: Trackable, atTime: OffsetDateTime, byUser: User
    ): RemovedFromTrackableEvent {
        val event = RemovedFromTrackableEvent(atTime, atTime)
        event.removedFromTrackable().value = trackable
        createdTimelineItem(issue, event, atTime, byUser)
        if (!existsNewerTimelineItem<AddedToTrackableEvent>(
                issue, atTime
            ) { it.addedToTrackable().value == trackable }
        ) {
            issue.trackables() -= trackable
            if (trackable in issue.pinnedOn()) {
                event.childItems() += removeIssueFromPinnedIssues(issue, trackable, atTime, byUser)
            }
            event.childItems() += issue.artefacts().filter { it.trackable().value == trackable }
                .map { removeArtefactFromIssue(issue, it, atTime, byUser) }
            event.childItems() += issue.labels().filter { Collections.disjoint(issue.trackables(), it.trackables()) }
                .map { removeLabelFromIssue(issue, it, atTime, byUser) }
        }
        return event
    }

    /**
     * Pins an [Issue] on a [Trackable], returns the created [AddedToPinnedIssuesEvent],
     * or `null` if the [Issue] was already pinned on the [Trackable].
     * Checks the authorization status, checks that the [Issue] can be pinned on the [Trackable]
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [Issue] to pin on which [Trackable]
     * @return the saved created [AddedToPinnedIssuesEvent] or `null` if no event was created
     */
    suspend fun addIssueToPinnedIssues(
        authorizationContext: GropiusAuthorizationContext, input: AddIssueToPinnedIssuesInput
    ): AddedToPinnedIssuesEvent? {
        val issue = repository.findById(input.issue)
        val trackable = trackableRepository.findById(input.trackable)
        checkPermission(
            trackable,
            Permission(TrackablePermission.MANAGE_ISSUES, authorizationContext),
            "manage Issues on the Trackable where the Issue should be pinned"
        )
        return if (trackable !in issue.pinnedOn()) {
            return timelineItemRepository.save(
                addIssueToPinnedIssues(issue, trackable, OffsetDateTime.now(), getUser(authorizationContext))
            ).awaitSingle()
        } else {
            null
        }
    }

    /**
     * Pins an [issue] on [trackable] at [atTime] as [byUser] and adds a [AddedToPinnedIssuesEvent]
     * to the timeline.
     * Creates the event even if the [issue] was already pinned on the [trackable].
     * Only adds the [issue] to the `pinnedIssues` on  [trackable] if no newer timeline item exists which removes
     * it again.
     * Does not check the authorization status.
     * Checks if the [issue] can be pinned to this [trackable].
     * Does neither save the created [AddedToPinnedIssuesEvent] nor the [issue].
     * It is necessary to save the [issue] or returned [AddedToPinnedIssuesEvent] afterwards.
     *
     * @param issue the [Issue] to pin
     * @param trackable the [Trackable] where the [issue] should be pinned
     * @param atTime the point in time when the modification happened, updates [Issue.lastUpdatedAt] if necessary
     * @param byUser the [User] who caused the update, updates [Issue.participants] if necessary
     * @return the created [AddedToPinnedIssuesEvent]
     * @throws IllegalArgumentException if [issue] cannot be pinned on [trackable]
     */
    suspend fun addIssueToPinnedIssues(
        issue: Issue, trackable: Trackable, atTime: OffsetDateTime, byUser: User
    ): AddedToPinnedIssuesEvent {
        if (trackable !in issue.trackables()) {
            throw IllegalArgumentException("The Issue cannot be pinned on the Trackable as it is not on the Trackable")
        }
        val event = AddedToPinnedIssuesEvent(atTime, atTime)
        event.pinnedOn().value = trackable
        createdTimelineItem(issue, event, atTime, byUser)
        if (!existsNewerTimelineItem<RemovedFromPinnedIssuesEvent>(
                issue, atTime
            ) { it.unpinnedOn().value == trackable }
        ) {
            issue.pinnedOn() += trackable
        }
        return event
    }

    /**
     * Unpins an [Issue] on a [Trackable], returns the created [RemovedFromPinnedIssuesEvent],
     * or `null` if the [Issue] was not pinned on the [Trackable].
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [Issue] to unpin on which [Trackable]
     * @return the saved created [RemovedFromPinnedIssuesEvent] or `null` if no event was created
     */
    suspend fun removeIssueFromPinnedIssues(
        authorizationContext: GropiusAuthorizationContext, input: RemoveIssueFromPinnedIssuesInput
    ): RemovedFromPinnedIssuesEvent? {
        val issue = repository.findById(input.issue)
        val trackable = trackableRepository.findById(input.trackable)
        checkPermission(
            trackable,
            Permission(TrackablePermission.MANAGE_ISSUES, authorizationContext),
            "manage Issues on the Trackable where the Issue should be unpinned"
        )
        return if (trackable in issue.pinnedOn()) {
            return timelineItemRepository.save(
                removeIssueFromPinnedIssues(issue, trackable, OffsetDateTime.now(), getUser(authorizationContext))
            ).awaitSingle()
        } else {
            null
        }
    }

    /**
     * Unpins an [issue] on a [trackable] at [atTime] as [byUser] and adds a [RemovedFromPinnedIssuesEvent]
     * to the timeline.
     * Creates the event even if the [issue] was not pinned on the [trackable].
     * Only removes the [issue] from the `pinnedIssues` on [trackable] if no newer timeline item exists which
     * adds it again.
     * Does not check the authorization status.
     * Does neither save the created [RemovedFromPinnedIssuesEvent] nor the [issue].
     * It is necessary to save the [issue] or returned [RemovedFromPinnedIssuesEvent] afterwards.
     *
     * @param issue the [Issue] to unpin
     * @param trackable the [Trackable] where [issue] should be unpinned
     * @param atTime the point in time when the modification happened, updates [Issue.lastUpdatedAt] if necessary
     * @param byUser the [User] who caused the update, updates [Issue.participants] if necessary
     * @return the created [RemovedFromPinnedIssuesEvent]
     */
    suspend fun removeIssueFromPinnedIssues(
        issue: Issue, trackable: Trackable, atTime: OffsetDateTime, byUser: User
    ): RemovedFromPinnedIssuesEvent {
        val event = RemovedFromPinnedIssuesEvent(atTime, atTime)
        event.unpinnedOn().value = trackable
        createdTimelineItem(issue, event, atTime, byUser)
        if (!existsNewerTimelineItem<AddedToPinnedIssuesEvent>(issue, atTime) { it.pinnedOn().value == trackable }) {
            issue.pinnedOn() -= trackable
        }
        return event
    }

    /**
     * Adds a [Label] to an [Issue], returns the created [AddedLabelEvent], or `null` if the [Label] was already
     * present on the [Issue].
     * Checks the authorization status, checks that the [Label] can be added to the [Issue]
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [Label] to add to which [Issue]
     * @return the saved created [AddedLabelEvent] or `null` if no event was created
     */
    suspend fun addLabelToIssue(
        authorizationContext: GropiusAuthorizationContext, input: AddLabelToIssueInput
    ): AddedLabelEvent? {
        val issue = repository.findById(input.issue)
        val label = labelRepository.findById(input.label)
        checkPermission(issue, Permission(TrackablePermission.MANAGE_ISSUES, authorizationContext), "manage the Issue")
        checkPermission(label, Permission(NodePermission.READ, authorizationContext), "use the Label")
        return if (label !in issue.labels()) {
            return timelineItemRepository.save(
                addLabelToIssue(issue, label, OffsetDateTime.now(), getUser(authorizationContext))
            ).awaitSingle()
        } else {
            null
        }
    }

    /**
     * Adds a [label] to an [issue] at [atTime] as [byUser] and adds a [AddedLabelEvent] to the timeline.
     * Creates the event even if the [label] was already on the [issue].
     * Only adds the [label] to the [issue] if no newer timeline item exists which removes it again.
     * Does not check the authorization status.
     * Checks if the [label] can be added to this [issue].
     * Does neither save the created [AddedLabelEvent] nor the [issue].
     * It is necessary to save the [issue] or returned [AddedLabelEvent] afterwards.
     *
     * @param issue the [Issue] where [label] is added to
     * @param label the [Label] to add
     * @param atTime the point in time when the modification happened, updates [Issue.lastUpdatedAt] if necessary
     * @param byUser the [User] who caused the update, updates [Issue.participants] if necessary
     * @return the created [AddedLabelEvent]
     * @throws IllegalArgumentException if [label] cannot be added to [issue]
     */
    suspend fun addLabelToIssue(
        issue: Issue, label: Label, atTime: OffsetDateTime, byUser: User
    ): AddedLabelEvent {
        if (Collections.disjoint(issue.trackables(), label.trackables())) {
            throw IllegalArgumentException("The Label cannot be added to the Issue as no common Trackable exists")
        }
        val event = AddedLabelEvent(atTime, atTime)
        event.addedLabel().value = label
        createdTimelineItem(issue, event, atTime, byUser)
        if (!existsNewerTimelineItem<RemovedLabelEvent>(issue, atTime) { it.removedLabel().value == label }) {
            issue.labels() += label
        }
        return event
    }

    /**
     * Removes a [Label] from an [Issue], returns the created [RemovedLabelEvent], or `null` if the [Label] was not
     * present on the [Issue].
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [Label] to remove from which [Issue]
     * @return the saved created [RemovedLabelEvent] or `null` if no event was created
     */
    suspend fun removeLabelFromIssue(
        authorizationContext: GropiusAuthorizationContext, input: RemoveLabelFromIssueInput
    ): RemovedLabelEvent? {
        val issue = repository.findById(input.issue)
        val label = labelRepository.findById(input.label)
        checkPermission(issue, Permission(TrackablePermission.MANAGE_ISSUES, authorizationContext), "manage the Issue")
        return if (label in issue.labels()) {
            return timelineItemRepository.save(
                removeLabelFromIssue(issue, label, OffsetDateTime.now(), getUser(authorizationContext))
            ).awaitSingle()
        } else {
            null
        }
    }

    /**
     * Removes a [label] from an [issue] at [atTime] as [byUser] and adds a [RemovedLabelEvent] to the timeline.
     * Creates the event even if the [label] was not on the [issue].
     * Only removes the [label] from the [issue] if no newer timeline item exists which adds it again.
     * Does not check the authorization status.
     * Does neither save the created [RemovedLabelEvent] nor the [issue].
     * It is necessary to save the [issue] or returned [RemovedLabelEvent] afterwards.
     *
     * @param issue the [Issue] where [label] is removed from
     * @param label the [Label] to remove
     * @param atTime the point in time when the modification happened, updates [Issue.lastUpdatedAt] if necessary
     * @param byUser the [User] who caused the update, updates [Issue.participants] if necessary
     * @return the created [RemovedLabelEvent]
     */
    suspend fun removeLabelFromIssue(
        issue: Issue, label: Label, atTime: OffsetDateTime, byUser: User
    ): RemovedLabelEvent {
        val event = RemovedLabelEvent(atTime, atTime)
        event.removedLabel().value = label
        createdTimelineItem(issue, event, atTime, byUser)
        if (!existsNewerTimelineItem<AddedLabelEvent>(issue, atTime) { it.addedLabel().value == label }) {
            issue.labels() -= label
        }
        return event
    }

    /**
     * Adds a [Artefact] to an [Issue], returns the created [AddedArtefactEvent], or `null` if the [Artefact] was already
     * present on the [Issue].
     * Checks the authorization status, checks that the [Artefact] can be added to the [Issue]
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [Artefact] to add to which [Issue]
     * @return the saved created [AddedArtefactEvent] or `null` if no event was created
     */
    suspend fun addArtefactToIssue(
        authorizationContext: GropiusAuthorizationContext, input: AddArtefactToIssueInput
    ): AddedArtefactEvent? {
        val issue = repository.findById(input.issue)
        val artefact = artefactRepository.findById(input.artefact)
        checkPermission(issue, Permission(TrackablePermission.MANAGE_ISSUES, authorizationContext), "manage the Issue")
        checkPermission(artefact, Permission(NodePermission.READ, authorizationContext), "use the Artefact")
        return if (artefact !in issue.artefacts()) {
            return timelineItemRepository.save(
                addArtefactToIssue(issue, artefact, OffsetDateTime.now(), getUser(authorizationContext))
            ).awaitSingle()
        } else {
            null
        }
    }

    /**
     * Adds a [artefact] to an [issue] at [atTime] as [byUser] and adds a [AddedArtefactEvent] to the timeline.
     * Creates the event even if the [artefact] was already on the [issue].
     * Only adds the [artefact] to the [issue] if no newer timeline item exists which removes it again.
     * Does not check the authorization status.
     * Check if the [artefact] can be added to this [issue].
     * Does neither save the created [AddedArtefactEvent] nor the [issue].
     * It is necessary to save the [issue] or returned [AddedArtefactEvent] afterwards.
     *
     * @param issue the [Issue] where [artefact] is added to
     * @param artefact the [Artefact] to add
     * @param atTime the point in time when the modification happened, updates [Issue.lastUpdatedAt] if necessary
     * @param byUser the [User] who caused the update, updates [Issue.participants] if necessary
     * @return the crated [AddedArtefactEvent]
     * @throws IllegalArgumentException if the [artefact] cannot be added to the [issue]
     */
    suspend fun addArtefactToIssue(
        issue: Issue, artefact: Artefact, atTime: OffsetDateTime, byUser: User
    ): AddedArtefactEvent {
        if (artefact.trackable().value !in issue.trackables()) {
            throw IllegalArgumentException("The Artefact is not part of a Trackable the Issue is on")
        }
        val event = AddedArtefactEvent(atTime, atTime)
        event.addedArtefact().value = artefact
        createdTimelineItem(issue, event, atTime, byUser)
        if (!existsNewerTimelineItem<RemovedArtefactEvent>(issue, atTime) { it.removedArtefact().value == artefact }) {
            issue.artefacts() += artefact
        }
        return event
    }

    /**
     * Removes a [Artefact] from an [Issue], returns the created [RemovedArtefactEvent], or `null` if the [Artefact] was not
     * present on the [Issue].
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [Artefact] to remove from which [Issue]
     * @return the saved created [RemovedArtefactEvent] or `null` if no event was created
     */
    suspend fun removeArtefactFromIssue(
        authorizationContext: GropiusAuthorizationContext, input: RemoveArtefactFromIssueInput
    ): RemovedArtefactEvent? {
        val issue = repository.findById(input.issue)
        val artefact = artefactRepository.findById(input.artefact)
        checkPermission(issue, Permission(TrackablePermission.MANAGE_ISSUES, authorizationContext), "manage the Issue")
        return if (artefact in issue.artefacts()) {
            return timelineItemRepository.save(
                removeArtefactFromIssue(issue, artefact, OffsetDateTime.now(), getUser(authorizationContext))
            ).awaitSingle()
        } else {
            null
        }
    }

    /**
     * Removes a [artefact] from an [issue] at [atTime] as [byUser] and adds a [RemovedArtefactEvent] to the timeline.
     * Creates the event even if the [artefact] was not on the [issue].
     * Only removes the [artefact] from the [issue] if no newer timeline item exists which adds it again.
     * Does not check the authorization status.
     * Does neither save the created [RemovedArtefactEvent] nor the [issue].
     * It is necessary to save the [issue] or returned [RemovedArtefactEvent] afterwards.
     *
     * @param issue the [Issue] where [artefact] is removed from
     * @param artefact the [Artefact] to remove
     * @param atTime the point in time when the modification happened, updates [Issue.lastUpdatedAt] if necessary
     * @param byUser the [User] who caused the update, updates [Issue.participants] if necessary
     * @return the created [RemovedArtefactEvent]
     */
    suspend fun removeArtefactFromIssue(
        issue: Issue, artefact: Artefact, atTime: OffsetDateTime, byUser: User
    ): RemovedArtefactEvent {
        val event = RemovedArtefactEvent(atTime, atTime)
        event.removedArtefact().value = artefact
        createdTimelineItem(issue, event, atTime, byUser)
        if (!existsNewerTimelineItem<AddedArtefactEvent>(issue, atTime) { it.addedArtefact().value == artefact }) {
            issue.artefacts() -= artefact
        }
        return event
    }

    /**
     * Called after a [TimelineItem] was created
     * Adds it to the [issue], calls [createdAuditedNode] and updates [Issue.lastUpdatedAt] and [Issue.participants].
     * Also sets [TimelineItem.issue], to allow to save [timelineItem] instead of [issue].
     *
     * @param issue associated [Issue] to which [timelineItem] should be added
     * @param timelineItem the created [TimelineItem]
     * @param atTime point in time at which [timelineItem] was created
     * @param byUser the [User] who created [timelineItem]
     */
    private suspend fun createdTimelineItem(
        issue: Issue, timelineItem: TimelineItem, atTime: OffsetDateTime, byUser: User
    ) {
        createdAuditedNode(timelineItem, byUser)
        timelineItem.issue().value = issue
        issue.timelineItems() += timelineItem
        issue.participants() += byUser
        issue.lastUpdatedAt = maxOf(issue.lastUpdatedAt, atTime)
    }

    /**
     * Checks if a [TimelineItem] with type [T] exists on [issue] created after [time] matching [itemFilter]
     *
     * @param T the type of [TimelineItem] to look for
     * @param issue contains the timeline to check
     * @param time only consider items after this time
     * @param itemFilter used to further filter for acceptable [TimelineItem]s
     * @return the result of the check
     */
    private suspend inline fun <reified T : TimelineItem> existsNewerTimelineItem(
        issue: Issue, time: OffsetDateTime, itemFilter: (T) -> Boolean
    ): Boolean {
        return issue.timelineItems().any {
            (it is T) && (it.createdAt > time) && itemFilter(it)
        }
    }

}