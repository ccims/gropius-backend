package gropius.sync.github

import com.fasterxml.jackson.databind.JsonNode
import gropius.model.architecture.IMSProject
import gropius.model.issue.Issue
import gropius.model.issue.timeline.*
import gropius.sync.*
import gropius.sync.github.generated.IssueReadQuery
import gropius.sync.github.generated.TimelineReadQuery
import gropius.sync.github.generated.fragment.*
import gropius.sync.github.generated.fragment.AssignedEventTimelineItemData.Assignee.Companion.userData
import gropius.sync.github.generated.fragment.TimelineItemData.Companion.asNode
import gropius.sync.github.generated.fragment.UnassignedEventTimelineItemData.Assignee.Companion.userData
import jakarta.transaction.Transactional
import kotlinx.coroutines.reactor.awaitSingle
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.data.neo4j.core.findById
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

/**
 * A timeline item that may be integrated into Gropius
 * @param githubId The GitHub ID of the timeline item
 * @param createdAt The creation date of the timeline item
 */
@Document
abstract class GithubTimelineItem(
    @Indexed
    val githubId: String,
    var createdAt: OffsetDateTime,
) : IncomingTimelineItem(), DereplicatorTimelineItemInfo {
    /**
     * MongoDB ID
     */
    @Id
    var id: ObjectId? = null
}

/**
 * GitHub Timeline Item with ID
 * @param githubId The GitHub ID of the timeline item
 * @param createdAt The creation date of the timeline item
 */
@Document
abstract class OwnedGithubTimelineItem(
    githubId: String, createdAt: OffsetDateTime
) : GithubTimelineItem(githubId, createdAt) {
    override suspend fun identification(): String {
        return githubId
    }
}

/**
 * A single issue that may or may not be integrated into Gropius
 * @param imsProject The IMS project ID
 * @param githubId The GitHub ID of the issue
 * @param initialTitle The initial title of the issue
 * @param initialDescription The initial description of the issue
 * @param lastUpdate The last update date of the issue
 * @param createdAt The creation date of the issue
 * @param timelineItems The timeline items of the issue
 * @param createdBy The GitHub user that created the issue
 * @param needsTimelineRequest Whether the timeline of the issue needs to be requested
 * @param needsCommentRequest Whether the comments of the issue need to be requested
 * @param hasUnsyncedData Whether the issue has unsynced data
 */
@Document
data class IssuePileData(
    @Indexed
    val imsProject: String,
    @Indexed
    val githubId: String,
    val initialTitle: String,
    val initialDescription: String,
    @Indexed
    var lastUpdate: OffsetDateTime,
    var createdAt: OffsetDateTime,
    val timelineItems: MutableList<GithubTimelineItem>,
    val createdBy: UserData?,
    var number: Int = 0,
    var url: String = "",
    @Indexed
    var needsTimelineRequest: Boolean = true,
    @Indexed
    var needsCommentRequest: Boolean = true,
    @Indexed
    var hasUnsyncedData: Boolean = true
) : IncomingIssue(), DereplicatorIssueInfo, DereplicatorTitleChangeIssueInfo {

    /**
     * MongoDB ID
     */
    @Id
    var id: ObjectId? = null

    override suspend fun identification(): String {
        return githubId
    }

    override suspend fun incomingTimelineItems(service: SyncDataService): List<IncomingTimelineItem> {
        return timelineItems
    }

    override suspend fun markDone(service: SyncDataService) {
        val githubService = service as GithubDataService
        githubService.issuePileService.markIssueSynced(id!!)
    }

    override suspend fun createIssue(imsProject: IMSProject, service: SyncDataService): Issue {
        val githubService = (service as GithubDataService)
        val issue = Issue(
            createdAt, lastUpdate, mutableMapOf(), initialTitle, initialDescription, lastUpdate
        )
        issue.body().value = Body(createdAt, lastUpdate, lastUpdate)
        issue.body().value.lastModifiedBy().value = githubService.mapUser(imsProject, createdBy)
        issue.body().value.bodyLastEditedBy().value = githubService.mapUser(imsProject, createdBy)
        issue.body().value.createdBy().value = githubService.mapUser(imsProject, createdBy)
        issue.createdBy().value = githubService.mapUser(imsProject, createdBy)
        issue.lastModifiedBy().value = githubService.mapUser(imsProject, createdBy)
        issue.body().value.issue().value = issue
        issue.state().value = githubService.issueState(imsProject, true)
        issue.template().value = githubService.issueTemplate(imsProject)
        issue.trackables() += imsProject.trackable().value
        issue.type().value = githubService.issueType(imsProject)
        return issue
    }

    override suspend fun fillImsIssueTemplatedFields(
        templatedFields: MutableMap<String, String>, service: SyncDataService
    ) {
        val githubService = (service as GithubDataService)
        val encodedNumber = githubService.jsonNodeMapper.jsonNodeToDeterministicString(
            githubService.objectMapper.valueToTree<JsonNode>(
                number
            )
        )
        templatedFields["number"] = encodedNumber

        val encodedUrl = githubService.jsonNodeMapper.jsonNodeToDeterministicString(
            githubService.objectMapper.valueToTree<JsonNode>(
                url
            )
        )
        templatedFields["url"] = encodedUrl

        val encodedId = githubService.jsonNodeMapper.jsonNodeToDeterministicString(
            githubService.objectMapper.valueToTree<JsonNode>(
                githubId
            )
        )
        templatedFields["id"] = encodedId
    }
}

/**
 * A repository for issue piles
 * @see IssuePileData
 */
@Repository
interface IssuePileRepository : ReactiveMongoRepository<IssuePileData, ObjectId> {

    /**
     * Find an issue pile by IMS project ID and GitHub ID
     * @param imsProject The IMS project ID
     * @param githubId The GitHub ID
     * @return The issue pile
     */
    suspend fun findByImsProjectAndGithubId(
        imsProject: String, githubId: String
    ): IssuePileData?

    /**
     * Find an issue pile by IMS project ID and GitHub ID
     * @param imsProject The IMS project ID
     * @param githubId The GitHub ID
     * @return The issue pile
     */
    suspend fun findFirstByImsProjectOrderByLastUpdateDesc(
        imsProject: String
    ): IssuePileData?

    /**
     * Find all issue piles by IMS project ID requiring a new Timeline request
     * @param imsProject The IMS project ID
     * @param needsTimelineRequest Whether the timeline needs to be requested
     */
    suspend fun findByImsProjectAndNeedsTimelineRequest(
        imsProject: String, needsTimelineRequest: Boolean
    ): List<IssuePileData>

    /**
     * Find all issue piles by IMS project ID requiring a new Comment request
     * @param imsProject The IMS project ID
     * @param needsCommentRequest Whether the comments need to be requested
     * @return The issue piles
     */
    suspend fun findByImsProjectAndNeedsCommentRequest(
        imsProject: String, needsCommentRequest: Boolean
    ): List<IssuePileData>

    /**
     * Find all issue piles by IMS project ID and whether they have unsynced data
     * @param imsProject The IMS project ID
     * @param hasUnsyncedData Whether the issue piles have unsynced data
     * @return The issue piles
     */
    suspend fun findByImsProjectAndHasUnsyncedData(
        imsProject: String, hasUnsyncedData: Boolean
    ): List<IssuePileData>
}

/**
 * A service for issue piles
 * @param issuePileRepository The issue pile repository
 */
@Service
class IssuePileService(val issuePileRepository: IssuePileRepository) : IssuePileRepository by issuePileRepository {

    /**
     * Integrate an issue into the database
     * @param imsProject The IMS project
     * @param data The issue data
     */
    @Transactional
    suspend fun integrateIssue(
        imsProject: IMSProject, data: IssueReadQuery.Data.Repository.Issues.Node
    ) {
        val pile = issuePileRepository.findByImsProjectAndGithubId(imsProject.rawId!!, data.id) ?: IssuePileData(
            imsProject.rawId!!,
            data.id,
            data.title,
            data.body,
            data.updatedAt,
            data.createdAt,
            mutableListOf(),
            data.author,
            data.number,
            data.url.toString()
        )
        pile.lastUpdate = data.updatedAt
        pile.needsTimelineRequest = true;
        pile.needsCommentRequest = true;
        pile.timelineItems.filter { (it as? IssueCommentTimelineItem) != null }
            .forEach { (it as IssueCommentTimelineItem).recheckDone = false }
        issuePileRepository.save(pile).awaitSingle()
    }

    /**
     * Mark an issue as needing no new timeline request
     * @param issue The issue
     */
    @Transactional
    suspend fun markIssueTimelineDone(
        issue: ObjectId
    ) {
        val pile = issuePileRepository.findById(issue).awaitSingle()
        pile.needsTimelineRequest = false
        issuePileRepository.save(pile).awaitSingle()
    }

    /**
     * Map a TimelineItem from the API to the Database
     * @param data The API data
     * @return The database data
     */
    fun mapTimelineItem(data: TimelineReadQuery.Data.IssueNode.TimelineItems.Node): GithubTimelineItem? {
        return when (data) {
            is IssueCommentTimelineItemData -> {
                return if (data.author != null) IssueCommentTimelineItem(data)
                else null
            }

            is ClosedEventTimelineItemData -> ClosedEventTimelineItem(data)
            is ReopenedEventTimelineItemData -> ReopenedEventTimelineItem(data)
            is LabeledEventTimelineItemData -> LabeledEventTimelineItem(data)
            is UnlabeledEventTimelineItemData -> UnlabeledEventTimelineItem(data)
            is RenamedTitleEventTimelineItemData -> RenamedTitleEventTimelineItem(data)
            is AssignedEventTimelineItemData -> AssignedTimelineItem(data)
            is UnassignedEventTimelineItemData -> UnassignedTimelineItem(data)

            // Handle all events separately, as GitHub does not inherit the createdAt time anywhere
            is CommentDeletedEventTimelineItemData -> UnknownTimelineItem(data.id, data.createdAt)
            is DemilestonedEventTimelineItemData -> UnknownTimelineItem(data.id, data.createdAt)
            is MarkedAsDuplicateEventTimelineItemData -> UnknownTimelineItem(data.id, data.createdAt)
            is MentionedEventTimelineItemData -> UnknownTimelineItem(data.id, data.createdAt)
            is MilestonedEventTimelineItemData -> UnknownTimelineItem(data.id, data.createdAt)
            is PinnedEventTimelineItemData -> UnknownTimelineItem(data.id, data.createdAt)
            is UnmarkedAsDuplicateEventTimelineItemData -> UnknownTimelineItem(data.id, data.createdAt)
            is UnpinnedEventTimelineItemData -> UnknownTimelineItem(data.id, data.createdAt)

            is AddedToProjectEventTimelineItemData -> UnknownTimelineItem(data.id, data.createdAt)
            is ConnectedEventTimelineItemData -> UnknownTimelineItem(data.id, data.createdAt)
            is ConvertedNoteToIssueEventTimelineItemData -> UnknownTimelineItem(data.id, data.createdAt)
            is ConvertedToDiscussionEventTimelineItemData -> UnknownTimelineItem(data.id, data.createdAt)
            is CrossReferencedEventTimelineItemData -> UnknownTimelineItem(data.id, data.createdAt)
            is DisconnectedEventTimelineItemData -> UnknownTimelineItem(data.id, data.createdAt)
            is LockedEventTimelineItemData -> UnknownTimelineItem(data.id, data.createdAt)
            is MovedColumnsInProjectEventTimelineItemData -> UnknownTimelineItem(data.id, data.createdAt)
            is ReferencedEventTimelineItemData -> UnknownTimelineItem(data.id, data.createdAt)
            is RemovedFromProjectEventTimelineItemData -> UnknownTimelineItem(data.id, data.createdAt)
            is TransferredEventTimelineItemData -> UnknownTimelineItem(data.id, data.createdAt)
            is UnlockedEventTimelineItemData -> UnknownTimelineItem(data.id, data.createdAt)
            is SubscribedEventTimelineItemData -> UnknownTimelineItem(data.id, data.createdAt)
            is UnsubscribedEventTimelineItemData -> UnknownTimelineItem(data.id, data.createdAt)
            else -> {
                //throw IllegalArgumentException("Invalid GraphQL query response in timeline") // TODO: Check if exception or null better
                null
            }
        }
    }

    /**
     * Integrate a timeline item into the database
     * @param issue The issue
     * @param data The timeline item data
     */
    @Transactional
    suspend fun integrateTimelineItem(issue: ObjectId, data: TimelineReadQuery.Data.IssueNode.TimelineItems.Node) {
        val pile = issuePileRepository.findById(issue).awaitSingle()
        if (pile.timelineItems.any { it.githubId == data.asNode()?.id }) throw Exception("TODO???")
        val ti = mapTimelineItem(data)
        if (ti != null) {
            pile.timelineItems += ti
            pile.hasUnsyncedData = true;
            issuePileRepository.save(pile).awaitSingle()
        }
    }

    /**
     * Mark a comment as needing no new comment request
     * @param issue The issue
     * @param comment The comment
     * @param updatedAt The update date of the comment
     * @param body The body of the comment
     */
    @Transactional
    suspend fun markCommentDone(issue: ObjectId, comment: String, updatedAt: OffsetDateTime, body: String) {
        val pile = issuePileRepository.findById(issue).awaitSingle()
        val c = (pile.timelineItems?.find { it.githubId == comment } as? IssueCommentTimelineItem)!!
        c.createdAt = updatedAt
        c.body = body
        c.recheckDone = true
        pile.needsCommentRequest =
            pile.timelineItems.count { (it as? IssueCommentTimelineItem)?.recheckDone == false } != 0
        pile.hasUnsyncedData = true;
        issuePileRepository.save(pile).awaitSingle()
    }

    /**
     * Mark an issue as done
     * @param issue The issue
     */
    @Transactional
    suspend fun markIssueSynced(issue: ObjectId) {
        val pile = issuePileRepository.findById(issue).awaitSingle()
        pile.hasUnsyncedData = false
        issuePileRepository.save(pile).awaitSingle()
    }
}

/**
 * Placeholder for mapping information
 * @param imsProject The IMS project ID
 * @param githubId The GitHub ID
 */
class TODOTimelineItemConversionInformation(
    imsProject: String, githubId: String
) : TimelineItemConversionInformation(imsProject, githubId, null) {}

/**
 * A timeline item that may be integrated into Gropius
 * @param githubId The GitHub ID of the timeline item
 * @param createdAt The creation date of the timeline item
 * @param createdBy The GitHub user that created the timeline item
 * @param oldTitle previous title
 * @param newTitle new title
 */
class RenamedTitleEventTimelineItem(
    githubId: String, createdAt: OffsetDateTime, val createdBy: UserData?, val oldTitle: String, val newTitle: String
) : OwnedGithubTimelineItem(githubId, createdAt) {

    /**
     * Parse API data
     * @param data The API data
     */
    constructor(data: RenamedTitleEventTimelineItemData) : this(
        data.id, data.createdAt, data.actor, data.previousTitle, data.currentTitle
    ) {
    }

    override suspend fun gropiusTimelineItem(
        imsProject: IMSProject,
        service: SyncDataService,
        timelineItemConversionInformation: TimelineItemConversionInformation?,
        issue: Issue
    ): Pair<List<TimelineItem>, TimelineItemConversionInformation> {
        val convInfo =
            timelineItemConversionInformation ?: TODOTimelineItemConversionInformation(imsProject.rawId!!, githubId);
        val githubService = service as GithubDataService
        val gropiusId = convInfo.gropiusId
        val event = if (gropiusId != null) githubService.neoOperations.findById<TitleChangedEvent>(
            gropiusId
        ) else TitleChangedEvent(createdAt, createdAt, oldTitle, newTitle)
        if (event == null) {
            return listOf<TimelineItem>() to convInfo;
        }
        event.createdBy().value = githubService.mapUser(imsProject, createdBy)
        event.lastModifiedBy().value = githubService.mapUser(imsProject, createdBy)
        return listOf<TimelineItem>(event) to convInfo;
    }
}

/**
 * A timeline item that may be integrated into Gropius
 * @param githubId The GitHub ID of the timeline item
 * @param createdAt The creation date of the timeline item
 * @param createdBy The GitHub user that created the timeline item
 * @param label The label that was removed
 */
class UnlabeledEventTimelineItem(
    githubId: String, createdAt: OffsetDateTime, val createdBy: UserData?, val label: LabelData
) : OwnedGithubTimelineItem(githubId, createdAt) {

    /**
     * Parse API data
     * @param data The API data
     */
    constructor(data: UnlabeledEventTimelineItemData) : this(data.id, data.createdAt, data.actor, data.label) {}

    override suspend fun gropiusTimelineItem(
        imsProject: IMSProject,
        service: SyncDataService,
        timelineItemConversionInformation: TimelineItemConversionInformation?,
        issue: Issue
    ): Pair<List<TimelineItem>, TimelineItemConversionInformation> {
        val convInfo =
            timelineItemConversionInformation ?: TODOTimelineItemConversionInformation(imsProject.rawId!!, githubId);
        val githubService = service as GithubDataService
        val gropiusId = convInfo.gropiusId
        val event = if (gropiusId != null) githubService.neoOperations.findById<RemovedLabelEvent>(
            gropiusId
        ) else RemovedLabelEvent(createdAt, createdAt)
        if (event == null) {
            return listOf<TimelineItem>() to convInfo;
        }
        event.createdBy().value = githubService.mapUser(imsProject, createdBy)
        event.lastModifiedBy().value = githubService.mapUser(imsProject, createdBy)
        event.removedLabel().value = githubService.mapLabel(imsProject, label)
        return listOf<TimelineItem>(event) to convInfo;
    }
}

/**
 * A timeline item that may be integrated into Gropius
 * @param githubId The GitHub ID of the timeline item
 * @param createdAt The creation date of the timeline item
 * @param createdBy The GitHub user that created the timeline item
 * @param label The label that was added
 */
class LabeledEventTimelineItem(
    githubId: String, createdAt: OffsetDateTime, val createdBy: UserData?, val label: LabelData
) : OwnedGithubTimelineItem(githubId, createdAt) {

    override suspend fun gropiusTimelineItem(
        imsProject: IMSProject,
        service: SyncDataService,
        timelineItemConversionInformation: TimelineItemConversionInformation?,
        issue: Issue
    ): Pair<List<TimelineItem>, TimelineItemConversionInformation> {
        val convInfo =
            timelineItemConversionInformation ?: TODOTimelineItemConversionInformation(imsProject.rawId!!, githubId);
        val githubService = service as GithubDataService
        val gropiusId = convInfo.gropiusId
        val event = if (gropiusId != null) githubService.neoOperations.findById<AddedLabelEvent>(
            gropiusId
        ) else AddedLabelEvent(createdAt, createdAt)
        if (event == null) {
            return listOf<TimelineItem>() to convInfo;
        }
        event.createdBy().value = githubService.mapUser(imsProject, createdBy)
        event.lastModifiedBy().value = githubService.mapUser(imsProject, createdBy)
        event.addedLabel().value = githubService.mapLabel(imsProject, label)
        return listOf<TimelineItem>(event) to convInfo;
    }

    /**
     * Parse API data
     * @param data The API data
     */
    constructor(data: LabeledEventTimelineItemData) : this(data.id, data.createdAt, data.actor, data.label) {}
}

/**
 * A timeline item that may be integrated into Gropius
 * @param githubId The GitHub ID of the timeline item
 * @param createdAt The creation date of the timeline item
 * @param createdBy The GitHub user that created the timeline item
 * @param user The user to unassign
 */
class UnassignedTimelineItem(
    githubId: String, createdAt: OffsetDateTime, val createdBy: UserData?, val user: String
) : OwnedGithubTimelineItem(githubId, createdAt) {

    /**
     * Parse API data
     * @param data The API data
     */
    constructor(data: UnassignedEventTimelineItemData) : this(
        data.id, data.createdAt, data.actor, data.assignee?.userData()?.login ?: "ghost"
    ) {
    }

    override suspend fun gropiusTimelineItem(
        imsProject: IMSProject,
        service: SyncDataService,
        timelineItemConversionInformation: TimelineItemConversionInformation?,
        issue: Issue
    ): Pair<List<TimelineItem>, TimelineItemConversionInformation> {
        val convInfo =
            timelineItemConversionInformation ?: TODOTimelineItemConversionInformation(imsProject.rawId!!, githubId);
        val githubService = service as GithubDataService
        val gropiusId = convInfo.gropiusId
        val event = if (gropiusId != null) githubService.neoOperations.findById<RemovedAssignmentEvent>(
            gropiusId
        ) else RemovedAssignmentEvent(createdAt, createdAt)
        val opposite = issue.timelineItems().filterIsInstance<Assignment>().sortedBy { it.createdAt }
            .lastOrNull { it.user().value.username == user } // TODO: Catch multiple
        if ((event == null) || (opposite == null)) {
            return listOf<TimelineItem>() to convInfo;
        }
        event.createdBy().value = githubService.mapUser(imsProject, createdBy)
        event.lastModifiedBy().value = githubService.mapUser(imsProject, createdBy)
        event.removedAssignment().value = opposite
        return listOf<TimelineItem>(event) to convInfo;
    }
}

/**
 * A timeline item that may be integrated into Gropius
 * @param githubId The GitHub ID of the timeline item
 * @param createdAt The creation date of the timeline item
 * @param createdBy The GitHub user that created the timeline item
 * @param user The user to assign
 */
class AssignedTimelineItem(
    githubId: String, createdAt: OffsetDateTime, val createdBy: UserData?, val user: UserData?
) : OwnedGithubTimelineItem(githubId, createdAt) {

    /**
     * Parse API data
     * @param data The API data
     */
    constructor(data: AssignedEventTimelineItemData) : this(
        data.id, data.createdAt, data.actor, data.assignee?.userData()
    ) {
    }

    override suspend fun gropiusTimelineItem(
        imsProject: IMSProject,
        service: SyncDataService,
        timelineItemConversionInformation: TimelineItemConversionInformation?,
        issue: Issue
    ): Pair<List<TimelineItem>, TimelineItemConversionInformation> {
        val convInfo =
            timelineItemConversionInformation ?: TODOTimelineItemConversionInformation(imsProject.rawId!!, githubId);
        val githubService = service as GithubDataService
        val gropiusId = convInfo.gropiusId
        val event = if (gropiusId != null) githubService.neoOperations.findById<Assignment>(
            gropiusId
        ) else Assignment(createdAt, createdAt)
        if (event == null) {
            return listOf<TimelineItem>() to convInfo;
        }
        event.createdBy().value = githubService.mapUser(imsProject, createdBy)
        event.lastModifiedBy().value = githubService.mapUser(imsProject, createdBy)
        event.user().value = githubService.mapUser(imsProject, user)
        return listOf<TimelineItem>(event) to convInfo;
    }
}

/**
 * A timeline item that may be integrated into Gropius
 * @param githubId The GitHub ID of the timeline item
 * @param createdAt The creation date of the timeline item
 * @param createdBy The GitHub user that created the timeline item
 */
class ReopenedEventTimelineItem(githubId: String, createdAt: OffsetDateTime, val createdBy: UserData?) :
    OwnedGithubTimelineItem(githubId, createdAt) {

    /**
     * Parse API data
     * @param data The API data
     */
    constructor(data: ReopenedEventTimelineItemData) : this(data.id, data.createdAt, data.actor) {}

    override suspend fun gropiusTimelineItem(
        imsProject: IMSProject,
        service: SyncDataService,
        timelineItemConversionInformation: TimelineItemConversionInformation?,
        issue: Issue
    ): Pair<List<TimelineItem>, TimelineItemConversionInformation> {
        val convInfo =
            timelineItemConversionInformation ?: TODOTimelineItemConversionInformation(imsProject.rawId!!, githubId);
        val githubService = service as GithubDataService
        val gropiusId = convInfo.gropiusId
        val event = if (gropiusId != null) githubService.neoOperations.findById<StateChangedEvent>(
            gropiusId
        ) else StateChangedEvent(createdAt, createdAt)
        if (event == null) {
            return listOf<TimelineItem>() to convInfo;
        }
        event.createdBy().value = githubService.mapUser(imsProject, createdBy)
        event.lastModifiedBy().value = githubService.mapUser(imsProject, createdBy)
        event.newState().value = githubService.issueState(imsProject, true)
        event.oldState().value = githubService.issueState(imsProject, false)
        return listOf<TimelineItem>(event) to convInfo;
    }
}

/**
 * A timeline item that may be integrated into Gropius
 * @param githubId The GitHub ID of the timeline item
 * @param createdAt The creation date of the timeline item
 * @param createdBy The GitHub user that created the timeline item
 */
class ClosedEventTimelineItem(githubId: String, createdAt: OffsetDateTime, val createdBy: UserData?) :
    OwnedGithubTimelineItem(githubId, createdAt) {

    /**
     * Parse API data
     * @param data The API data
     */
    constructor(data: ClosedEventTimelineItemData) : this(data.id, data.createdAt, data.actor) {}

    override suspend fun gropiusTimelineItem(
        imsProject: IMSProject,
        service: SyncDataService,
        timelineItemConversionInformation: TimelineItemConversionInformation?,
        issue: Issue
    ): Pair<List<TimelineItem>, TimelineItemConversionInformation> {
        val convInfo =
            timelineItemConversionInformation ?: TODOTimelineItemConversionInformation(imsProject.rawId!!, githubId);
        val githubService = service as GithubDataService
        val gropiusId = convInfo.gropiusId
        val event = if (gropiusId != null) githubService.neoOperations.findById<StateChangedEvent>(
            gropiusId
        ) else StateChangedEvent(createdAt, createdAt)
        if (event == null) {
            return listOf<TimelineItem>() to convInfo;
        }
        event.createdBy().value = githubService.mapUser(imsProject, createdBy)
        event.lastModifiedBy().value = githubService.mapUser(imsProject, createdBy)
        event.newState().value = githubService.issueState(imsProject, false)
        event.oldState().value = githubService.issueState(imsProject, true)
        return listOf<TimelineItem>(event) to convInfo;
    }
}

/**
 * A timeline item that may be integrated into Gropius
 * @param githubId The GitHub ID of the timeline item
 * @param createdAt The creation date of the timeline item
 * @param body The body of the comment
 * @param createdBy The GitHub user that created the timeline item
 * @param recheckDone Whether the comment has been checked for being done
 */
class IssueCommentTimelineItem(
    githubId: String,
    createdAt: OffsetDateTime,
    var body: String,
    val createdBy: UserData?,
    var recheckDone: Boolean = false
) : OwnedGithubTimelineItem(githubId, createdAt) {

    /**
     * Parse API data
     * @param data The API data
     */
    constructor(data: IssueCommentTimelineItemData) : this(
        data.id, data.createdAt, data.body, data.author
    ) {
    }

    override suspend fun gropiusTimelineItem(
        imsProject: IMSProject,
        service: SyncDataService,
        timelineItemConversionInformation: TimelineItemConversionInformation?,
        issue: Issue
    ): Pair<List<TimelineItem>, TimelineItemConversionInformation> {
        val convInfo =
            timelineItemConversionInformation ?: TODOTimelineItemConversionInformation(imsProject.rawId!!, githubId);
        val githubService = service as GithubDataService
        val gropiusId = convInfo.gropiusId
        val event = if (gropiusId != null) githubService.neoOperations.findById<IssueComment>(
            gropiusId
        ) else IssueComment(createdAt, createdAt, body, createdAt, false)
        if (event == null) {
            return listOf<TimelineItem>() to convInfo;
        }
        event.createdBy().value = githubService.mapUser(imsProject, createdBy)
        event.lastModifiedBy().value = githubService.mapUser(imsProject, createdBy)
        event.bodyLastEditedBy().value = githubService.mapUser(imsProject, createdBy)
        return listOf<TimelineItem>(event) to convInfo;
    }
}

/**
 * Placeholder to stop querying unsupported TimelineItems
 * @param githubId The GitHub ID of the timeline item
 * @param createdAt The creation date of the timeline item
 */
class UnknownTimelineItem(
    githubId: String, createdAt: OffsetDateTime
) : OwnedGithubTimelineItem(githubId, createdAt) {
    override suspend fun gropiusTimelineItem(
        imsProject: IMSProject,
        service: SyncDataService,
        timelineItemConversionInformation: TimelineItemConversionInformation?,
        issue: Issue
    ): Pair<List<TimelineItem>, TimelineItemConversionInformation> {
        val convInfo =
            timelineItemConversionInformation ?: TODOTimelineItemConversionInformation(imsProject.rawId!!, githubId);
        return listOf<TimelineItem>() to convInfo;
    }
}
