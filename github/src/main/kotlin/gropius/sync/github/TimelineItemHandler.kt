package gropius.sync.github

import gropius.model.issue.timeline.AddedLabelEvent
import gropius.model.issue.timeline.RemovedLabelEvent
import gropius.model.issue.timeline.TitleChangedEvent
import gropius.sync.github.generated.fragment.*
import gropius.sync.github.model.IssueInfo
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.neo4j.core.ReactiveNeo4jOperations
import org.springframework.stereotype.Component
import java.time.OffsetDateTime

/**
 * Save a github timeline item into a gropius timeline item
 */
@Component
class TimelineItemHandler(
    /**
     * Reference for the spring instance of NodeSourcerer
     */
    private val nodeSourcerer: NodeSourcerer,
    /**
     * Reference for the spring instance of ReactiveNeo4jOperations
     */
    @Qualifier("graphglueNeo4jOperations")
    private val neoOperations: ReactiveNeo4jOperations
) {
    /**
     * Save timeline item to database
     * @param issue Affected issue
     * @param event raw github timeline item
     * @return the neo4j-id for the created item (if created) and the last DateTime concerning this item
     */
    private suspend fun handleIssueComment(
        issue: IssueInfo, event: IssueCommentTimelineItemData
    ): Pair<String?, OffsetDateTime?> {
        //TODO
        return Pair(null, event.updatedAt)
    }

    /**
     * Save timeline item to database
     * @param issue Affected issue
     * @param event raw github timeline item
     * @return the neo4j-id for the created item (if created) and the last DateTime concerning this item
     */
    private suspend fun handleIssueClosed(
        issue: IssueInfo, event: ClosedEventTimelineItemData
    ): Pair<String?, OffsetDateTime?> {
        //TODO: PR #5
        return Pair(null, event.createdAt)
    }

    /**
     * Save timeline item to database
     * @param issue Affected issue
     * @param event raw github timeline item
     * @return the neo4j-id for the created item (if created) and the last DateTime concerning this item
     */
    private suspend fun handleIssueReopen(
        issue: IssueInfo, event: ReopenedEventTimelineItemData
    ): Pair<String?, OffsetDateTime?> {
        //TODO: PR #5
        return Pair(null, event.createdAt)
    }

    /**
     * Save timeline item to database
     * @param issue Affected issue
     * @param event raw github timeline item
     * @return the neo4j-id for the created item (if created) and the last DateTime concerning this item
     */
    private suspend fun handleIssueLabeled(
        issue: IssueInfo, event: LabeledEventTimelineItemData
    ): Pair<String?, OffsetDateTime?> {
        var addedLabelEvent = AddedLabelEvent(event.createdAt, OffsetDateTime.now())
        addedLabelEvent.issue().value = issue.load(neoOperations)
        addedLabelEvent.createdBy().value = nodeSourcerer.ensureUser(event.actor!!)
        addedLabelEvent.lastModifiedBy().value = nodeSourcerer.ensureUser(event.actor!!)
        addedLabelEvent.addedLabel().value = nodeSourcerer.ensureLabel(event.label)
        addedLabelEvent = neoOperations.save(addedLabelEvent).awaitSingle()
        return Pair(addedLabelEvent.rawId, event.createdAt)
    }

    /**
     * Save timeline item to database
     * @param issue Affected issue
     * @param event raw github timeline item
     * @return the neo4j-id for the created item (if created) and the last DateTime concerning this item
     */
    private suspend fun handleIssueUnlabeled(
        issue: IssueInfo, event: UnlabeledEventTimelineItemData
    ): Pair<String?, OffsetDateTime?> {
        var removedLabelEvent = RemovedLabelEvent(event.createdAt, OffsetDateTime.now())
        removedLabelEvent.issue().value = issue.load(neoOperations)
        removedLabelEvent.createdBy().value = nodeSourcerer.ensureUser(event.actor!!)
        removedLabelEvent.lastModifiedBy().value = nodeSourcerer.ensureUser(event.actor!!)
        removedLabelEvent.removedLabel().value = nodeSourcerer.ensureLabel(event.label)
        removedLabelEvent = neoOperations.save(removedLabelEvent).awaitSingle()
        return Pair(removedLabelEvent.rawId, event.createdAt)
    }

    /**
     * Save timeline item to database
     * @param issue Affected issue
     * @param event raw github timeline item
     * @return the neo4j-id for the created item (if created) and the last DateTime concerning this item
     */
    private suspend fun handleIssueRenamedTitle(
        issue: IssueInfo, event: RenamedTitleEventTimelineItemData
    ): Pair<String?, OffsetDateTime?> {
        var titleChangedEvent =
            TitleChangedEvent(event.createdAt, OffsetDateTime.now(), event.previousTitle, event.currentTitle)
        titleChangedEvent.issue().value = issue.load(neoOperations)
        titleChangedEvent.createdBy().value = nodeSourcerer.ensureUser(event.actor!!)
        titleChangedEvent.lastModifiedBy().value = nodeSourcerer.ensureUser(event.actor!!)
        titleChangedEvent = neoOperations.save(titleChangedEvent).awaitSingle()
        return Pair(titleChangedEvent.rawId, event.createdAt)
    }

    /**
     * Save a non-comment timeline item to the database
     * @param issue The mongo info for the the issue
     * @param event a GrqphQL timeline issue
     * @return the neo4j-id for the created item (if created) and the last DateTime concerning this item
     */
    suspend fun handleIssueModifiedItem(issue: IssueInfo, event: TimelineItemData): Pair<String?, OffsetDateTime?> {
        return when (event) {
            is IssueCommentTimelineItemData -> handleIssueComment(issue, event)
            is ClosedEventTimelineItemData -> handleIssueClosed(issue, event)
            is ReopenedEventTimelineItemData -> handleIssueReopen(issue, event)
            is LabeledEventTimelineItemData -> handleIssueLabeled(issue, event)
            is UnlabeledEventTimelineItemData -> handleIssueUnlabeled(issue, event)
            is RenamedTitleEventTimelineItemData -> handleIssueRenamedTitle(issue, event)
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
                println(event)
                TODO("Throw nice exception for unhandled event: " + event.__typename)
            }
        }
    }
}