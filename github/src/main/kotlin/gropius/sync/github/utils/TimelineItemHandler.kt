package gropius.sync.github.utils

import gropius.model.issue.timeline.*
import gropius.sync.github.NodeSourcerer
import gropius.sync.github.config.IMSProjectConfig
import gropius.sync.github.generated.fragment.*
import gropius.sync.github.model.IssueInfo
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.neo4j.core.ReactiveNeo4jOperations
import org.springframework.data.neo4j.core.findById
import org.springframework.stereotype.Component
import java.time.OffsetDateTime

/**
 * Save a GitHub timeline item into a Gropius timeline item
 * @param neoOperations Reference for the spring instance of ReactiveNeo4jOperations
 * @param nodeSourcerer Reference for the spring instance of NodeSourcerer
 */
@Component
class TimelineItemHandler(
    private val nodeSourcerer: NodeSourcerer,
    @Qualifier("graphglueNeo4jOperations")
    private val neoOperations: ReactiveNeo4jOperations,
) {
    /**
     * Logger used to print notifications
     */
    private val logger = LoggerFactory.getLogger(TimelineItemHandler::class.java)

    /**
     * Save timeline item to database
     * @param issue Affected issue
     * @param event raw GitHub timeline item
     * @param imsProjectConfig Config of the active project
     * @param neo4jID ID of the comment if only modified
     * @return the neo4j-id for the created item (if created) and the last DateTime concerning this item
     */
    suspend fun handleIssueComment(
        imsProjectConfig: IMSProjectConfig, issue: IssueInfo, event: IssueCommentData, neo4jID: String?
    ): Pair<String?, OffsetDateTime?> {
        var commentEvent: IssueComment
        if (neo4jID == null) {
            commentEvent = IssueComment(event.createdAt, OffsetDateTime.now(), "", event.createdAt, false)
            commentEvent.issue().value = issue.load(neoOperations)
            commentEvent.createdBy().value = nodeSourcerer.ensureUser(imsProjectConfig, event.author!!)
        } else {
            commentEvent = neoOperations.findById(neo4jID)!!
            if (commentEvent.bodyLastEditedAt > event.updatedAt) {
                return Pair(commentEvent.rawId, event.updatedAt)
            }
        }
        commentEvent.body = event.body
        commentEvent.bodyLastEditedAt = event.updatedAt
        commentEvent.bodyLastEditedBy().value =
            nodeSourcerer.ensureUser(imsProjectConfig, event.editor ?: event.author!!)
        commentEvent.lastModifiedAt = event.updatedAt
        commentEvent.lastModifiedBy().value = nodeSourcerer.ensureUser(imsProjectConfig, event.editor ?: event.author!!)
        commentEvent.issue().value.issueComments() += commentEvent
        commentEvent = neoOperations.save(commentEvent).awaitSingle()
        return Pair(commentEvent.rawId, event.updatedAt)
    }

    /**
     * Save timeline item to database
     * Only changes the state if a fitting state exists
     * @param imsProjectConfig Config to use
     * @param issue Affected issue
     * @param event raw GitHub timeline item
     * @param imsProjectConfig Config of the active project
     * @return the neo4j-id for the created item (if created) and the last DateTime concerning this item
     */
    private suspend fun handleIssueClosed(
        imsProjectConfig: IMSProjectConfig, issue: IssueInfo, event: ClosedEventTimelineItemData
    ): Pair<String?, OffsetDateTime?> {
        val loadedIssue = issue.load(neoOperations)
        // TODO better state detection
        val newState = loadedIssue.template().value.issueStates().firstOrNull { !it.isOpen }
        return newState?.let {
            var stateChangedEvent = StateChangedEvent(event.createdAt, OffsetDateTime.now())
            stateChangedEvent.oldState().value = issue.load(neoOperations).state().value
            stateChangedEvent.newState().value = newState
            stateChangedEvent.issue().value = loadedIssue
            stateChangedEvent.createdBy().value = nodeSourcerer.ensureUser(imsProjectConfig, event.actor!!)
            stateChangedEvent.lastModifiedBy().value = nodeSourcerer.ensureUser(imsProjectConfig, event.actor!!)
            stateChangedEvent = neoOperations.save(stateChangedEvent).awaitSingle()
            Pair(stateChangedEvent.rawId, event.createdAt)
        } ?: Pair(null, null)
    }

    /**
     * Save timeline item to database
     * Only changes the state if a fitting state exists
     * @param imsProjectConfig Config to use
     * @param issue Affected issue
     * @param event raw GitHub timeline item
     * @param imsProjectConfig Config of the active project
     * @return the neo4j-id for the created item (if created) and the last DateTime concerning this item
     */
    private suspend fun handleIssueReopen(
        imsProjectConfig: IMSProjectConfig, issue: IssueInfo, event: ReopenedEventTimelineItemData
    ): Pair<String?, OffsetDateTime?> {
        val loadedIssue = issue.load(neoOperations)
        // TODO better state detection
        val newState = loadedIssue.template().value.issueStates().firstOrNull { it.isOpen }
        return newState?.let {
            var stateChangedEvent = StateChangedEvent(event.createdAt, OffsetDateTime.now())
            stateChangedEvent.oldState().value = issue.load(neoOperations).state().value
            stateChangedEvent.newState().value = newState
            stateChangedEvent.issue().value = loadedIssue
            stateChangedEvent.createdBy().value = nodeSourcerer.ensureUser(imsProjectConfig, event.actor!!)
            stateChangedEvent.lastModifiedBy().value = nodeSourcerer.ensureUser(imsProjectConfig, event.actor!!)
            stateChangedEvent = neoOperations.save(stateChangedEvent).awaitSingle()
            Pair(stateChangedEvent.rawId, event.createdAt)
        } ?: Pair(null, null)
    }

    /**
     * Save timeline item to database
     * @param imsProjectConfig Config to use
     * @param issue Affected issue
     * @param event raw GitHub timeline item
     * @param imsProjectConfig Config of the active project
     * @return the neo4j-id for the created item (if created) and the last DateTime concerning this item
     */
    private suspend fun handleIssueLabeled(
        imsProjectConfig: IMSProjectConfig, issue: IssueInfo, event: LabeledEventTimelineItemData
    ): Pair<String?, OffsetDateTime?> {
        var addedLabelEvent = AddedLabelEvent(event.createdAt, OffsetDateTime.now())
        addedLabelEvent.issue().value = issue.load(neoOperations)
        addedLabelEvent.createdBy().value = nodeSourcerer.ensureUser(imsProjectConfig, event.actor!!)
        addedLabelEvent.lastModifiedBy().value = nodeSourcerer.ensureUser(imsProjectConfig, event.actor!!)
        //addedLabelEvent.addedLabel().value = nodeSourcerer.ensureLabel(imsProjectConfig, event.label)
        addedLabelEvent = neoOperations.save(addedLabelEvent).awaitSingle()
        return Pair(addedLabelEvent.rawId, event.createdAt)
    }

    /**
     * Save timeline item to database
     * @param imsProjectConfig Config to use
     * @param issue Affected issue
     * @param event raw GitHub timeline item
     * @param imsProjectConfig Config of the active project
     * @return the neo4j-id for the created item (if created) and the last DateTime concerning this item
     */
    private suspend fun handleIssueUnlabeled(
        imsProjectConfig: IMSProjectConfig, issue: IssueInfo, event: UnlabeledEventTimelineItemData
    ): Pair<String?, OffsetDateTime?> {
        var removedLabelEvent = RemovedLabelEvent(event.createdAt, OffsetDateTime.now())
        removedLabelEvent.issue().value = issue.load(neoOperations)
        removedLabelEvent.createdBy().value = nodeSourcerer.ensureUser(imsProjectConfig, event.actor!!)
        removedLabelEvent.lastModifiedBy().value = nodeSourcerer.ensureUser(imsProjectConfig, event.actor!!)
        //removedLabelEvent.removedLabel().value = nodeSourcerer.ensureLabel(imsProjectConfig, event.label)
        removedLabelEvent = neoOperations.save(removedLabelEvent).awaitSingle()
        return Pair(removedLabelEvent.rawId, event.createdAt)
    }

    /**
     * Save timeline item to database
     * @param imsProjectConfig Config to use
     * @param issue Affected issue
     * @param event raw GitHub timeline item
     * @param imsProjectConfig Config of the active project
     * @return the neo4j-id for the created item (if created) and the last DateTime concerning this item
     */
    private suspend fun handleIssueRenamedTitle(
        imsProjectConfig: IMSProjectConfig, issue: IssueInfo, event: RenamedTitleEventTimelineItemData
    ): Pair<String?, OffsetDateTime?> {
        var titleChangedEvent =
            TitleChangedEvent(event.createdAt, OffsetDateTime.now(), event.previousTitle, event.currentTitle)
        titleChangedEvent.issue().value = issue.load(neoOperations)
        titleChangedEvent.createdBy().value = nodeSourcerer.ensureUser(imsProjectConfig, event.actor!!)
        titleChangedEvent.lastModifiedBy().value = nodeSourcerer.ensureUser(imsProjectConfig, event.actor!!)
        titleChangedEvent = neoOperations.save(titleChangedEvent).awaitSingle()
        return Pair(titleChangedEvent.rawId, event.createdAt)
    }

    /**
     * Save a non-comment timeline item to the database
     * @param imsProjectConfig Config to use
     * @param issue The mongo info for the the issue
     * @param event a GrqphQL timeline issue
     * @param imsProjectConfig Config of the active project
     * @return the neo4j-id for the created item (if created) and the last DateTime concerning this item
     */
    suspend fun handleIssueModifiedItem(
        imsProjectConfig: IMSProjectConfig, issue: IssueInfo, event: TimelineItemData
    ): Pair<String?, OffsetDateTime?> {
        logger.trace(event.toString())
        return when (event) {
            is ClosedEventTimelineItemData -> handleIssueClosed(imsProjectConfig, issue, event)
            is ReopenedEventTimelineItemData -> handleIssueReopen(imsProjectConfig, issue, event)
            is LabeledEventTimelineItemData -> handleIssueLabeled(imsProjectConfig, issue, event)
            is UnlabeledEventTimelineItemData -> handleIssueUnlabeled(imsProjectConfig, issue, event)
            is RenamedTitleEventTimelineItemData -> handleIssueRenamedTitle(imsProjectConfig, issue, event)
            is AssignedEventTimelineItemData -> Pair(null, event.createdAt)
            is CommentDeletedEventTimelineItemData -> Pair(null, event.createdAt)
            is DemilestonedEventTimelineItemData -> Pair(null, event.createdAt)
            is MarkedAsDuplicateEventTimelineItemData -> Pair(null, event.createdAt)
            is MentionedEventTimelineItemData -> Pair(null, event.createdAt)
            is MilestonedEventTimelineItemData -> Pair(null, event.createdAt)
            is PinnedEventTimelineItemData -> Pair(null, event.createdAt)
            is UnassignedEventTimelineItemData -> Pair(null, event.createdAt)
            is UnmarkedAsDuplicateEventTimelineItemData -> Pair(null, event.createdAt)
            is UnpinnedEventTimelineItemData -> Pair(null, event.createdAt)

            // Handle all events separately, as GitHub does not inherit the createdAt time anywhere
            is AddedToProjectEventTimelineItemData -> Pair(null, event.createdAt)
            is ConnectedEventTimelineItemData -> Pair(null, event.createdAt)
            is ConvertedNoteToIssueEventTimelineItemData -> Pair(null, event.createdAt)
            is ConvertedToDiscussionEventTimelineItemData -> Pair(null, event.createdAt)
            is CrossReferencedEventTimelineItemData -> Pair(null, event.createdAt)
            is DisconnectedEventTimelineItemData -> Pair(null, event.createdAt)
            is LockedEventTimelineItemData -> Pair(null, event.createdAt)
            is MovedColumnsInProjectEventTimelineItemData -> Pair(null, event.createdAt)
            is ReferencedEventTimelineItemData -> Pair(null, event.createdAt)
            is RemovedFromProjectEventTimelineItemData -> Pair(null, event.createdAt)
            is TransferredEventTimelineItemData -> Pair(null, event.createdAt)
            is UnlockedEventTimelineItemData -> Pair(null, event.createdAt)
            is SubscribedEventTimelineItemData -> Pair(null, event.createdAt)
            is UnsubscribedEventTimelineItemData -> Pair(null, event.createdAt)
            else -> {
                throw IllegalArgumentException("Invalid GraphQL query response in timeline")
            }
        }
    }
}
