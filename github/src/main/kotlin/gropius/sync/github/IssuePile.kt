package gropius.sync.github

import gropius.model.architecture.IMSProject
import gropius.model.architecture.Project
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
import kotlinx.coroutines.reactive.awaitFirst
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

@Document
abstract class OwnedGithubTimelineItem(
    githubId: String, createdAt: OffsetDateTime
) : GithubTimelineItem(githubId, createdAt) {
    override suspend fun identification(): String {
        return githubId
    }
}

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
    val createdBy: String,
    @Indexed
    var needsTimelineRequest: Boolean = true,
    @Indexed
    var needsCommentRequest: Boolean = true,
    @Indexed
    var hasUnsyncedData: Boolean = true
) : IncomingIssue(), DereplicatorIssueInfo, DereplicatorTitleChangeIssueInfo {
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
        val issue = Issue(createdAt, lastUpdate, mutableMapOf(), initialTitle, lastUpdate, null, null, null, null)
        issue.body().value = Body(createdAt, lastUpdate, initialDescription, lastUpdate)
        issue.body().value.lastModifiedBy().value = githubService.userMapper.mapUser(imsProject, createdBy)
        issue.body().value.bodyLastEditedBy().value = githubService.userMapper.mapUser(imsProject, createdBy)
        issue.body().value.createdBy().value = githubService.userMapper.mapUser(imsProject, createdBy)
        issue.createdBy().value = githubService.userMapper.mapUser(imsProject, createdBy)
        issue.lastModifiedBy().value = githubService.userMapper.mapUser(imsProject, createdBy)
        issue.body().value.issue().value = issue
        issue.state().value = githubService.issueState()
        issue.template().value = githubService.issueTemplate()
        issue.trackables() += githubService.neoOperations.findAll(Project::class.java).awaitFirst()
        issue.type().value = githubService.issueType()
        return issue
    }
}

@Repository
interface IssuePileRepository : ReactiveMongoRepository<IssuePileData, ObjectId> {
    suspend fun findByImsProjectAndGithubId(
        imsProject: String, githubId: String
    ): IssuePileData?

    suspend fun findFirstByImsProjectOrderByLastUpdateDesc(
        imsProject: String
    ): IssuePileData?

    suspend fun findByImsProjectAndNeedsTimelineRequest(
        imsProject: String, needsTimelineRequest: Boolean
    ): List<IssuePileData>

    suspend fun findByImsProjectAndNeedsCommentRequest(
        imsProject: String, needsCommentRequest: Boolean
    ): List<IssuePileData>

    suspend fun findByImsProjectAndHasUnsyncedData(
        imsProject: String, hasUnsyncedData: Boolean
    ): List<IssuePileData>
}

@Service
class IssuePileService(val issuePileRepository: IssuePileRepository) : IssuePileRepository by issuePileRepository {
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
            data.author!!.login
        )
        pile.lastUpdate = data.updatedAt
        pile.needsTimelineRequest = true;
        pile.needsCommentRequest = true;
        pile.timelineItems.filter { (it as? IssueCommentTimelineItem) != null }
            .forEach { (it as IssueCommentTimelineItem).recheckDone = false }
        issuePileRepository.save(pile).awaitSingle()
    }

    @Transactional
    suspend fun markIssueTimelineDone(
        issue: ObjectId
    ) {
        val pile = issuePileRepository.findById(issue).awaitSingle()
        pile.needsTimelineRequest = false
        issuePileRepository.save(pile).awaitSingle()
    }

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
                //throw IllegalArgumentException("Invalid GraphQL query response in timeline")
                null
            }
        }
    }

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

    @Transactional
    suspend fun markIssueSynced(issue: ObjectId) {
        val pile = issuePileRepository.findById(issue).awaitSingle()
        pile.hasUnsyncedData = false
        issuePileRepository.save(pile).awaitSingle()
    }
}

class TODOTimelineItemConversionInformation(
    imsProject: String, githubId: String
) : TimelineItemConversionInformation(imsProject, githubId, null) {}

class RenamedTitleEventTimelineItem(
    githubId: String, createdAt: OffsetDateTime, val createdBy: String?, val oldTitle: String, val newTitle: String
) : OwnedGithubTimelineItem(githubId, createdAt) {
    constructor(data: RenamedTitleEventTimelineItemData) : this(
        data.id, data.createdAt, data.actor?.login, data.previousTitle, data.currentTitle
    ) {
    }

    override suspend fun gropiusTimelineItem(
        imsProject: IMSProject,
        service: SyncDataService,
        timelineItemConversionInformation: TimelineItemConversionInformation?
    ): Pair<List<TimelineItem>, TimelineItemConversionInformation> {
        val convInfo =
            timelineItemConversionInformation ?: TODOTimelineItemConversionInformation(imsProject.rawId!!, githubId);
        val githubService = service as GithubDataService
        if ((createdBy != null)) {
            val gropiusId = convInfo.gropiusId
            val event = if (gropiusId != null) githubService.neoOperations.findById<TitleChangedEvent>(
                gropiusId
            ) else TitleChangedEvent(createdAt, createdAt, oldTitle, newTitle)
            if (event == null) {
                return listOf<TimelineItem>() to convInfo;
            }
            event.createdBy().value = githubService.userMapper.mapUser(imsProject, createdBy)
            event.lastModifiedBy().value = githubService.userMapper.mapUser(imsProject, createdBy)
            return listOf<TimelineItem>(event) to convInfo;
        }
        return listOf<TimelineItem>() to convInfo;
    }

}

class UnlabeledEventTimelineItem(
    githubId: String, createdAt: OffsetDateTime, val createdBy: String?, val label: LabelData
) : OwnedGithubTimelineItem(githubId, createdAt) {
    constructor(data: UnlabeledEventTimelineItemData) : this(data.id, data.createdAt, data.actor?.login, data.label) {}

    override suspend fun gropiusTimelineItem(
        imsProject: IMSProject,
        service: SyncDataService,
        timelineItemConversionInformation: TimelineItemConversionInformation?
    ): Pair<List<TimelineItem>, TimelineItemConversionInformation> {
        val convInfo =
            timelineItemConversionInformation ?: TODOTimelineItemConversionInformation(imsProject.rawId!!, githubId);
        val githubService = service as GithubDataService
        if ((createdBy != null)) {
            val gropiusId = convInfo.gropiusId
            val event = if (gropiusId != null) githubService.neoOperations.findById<RemovedLabelEvent>(
                gropiusId
            ) else RemovedLabelEvent(createdAt, createdAt)
            if (event == null) {
                return listOf<TimelineItem>() to convInfo;
            }
            event.createdBy().value = githubService.userMapper.mapUser(imsProject, createdBy)
            event.lastModifiedBy().value = githubService.userMapper.mapUser(imsProject, createdBy)
            event.removedLabel().value = githubService.mapLabel(imsProject, label)
            return listOf<TimelineItem>(event) to convInfo;
        }
        return listOf<TimelineItem>() to convInfo;
    }
}

class LabeledEventTimelineItem(
    githubId: String, createdAt: OffsetDateTime, val createdBy: String?, val label: LabelData
) : OwnedGithubTimelineItem(githubId, createdAt) {
    constructor(data: LabeledEventTimelineItemData) : this(data.id, data.createdAt, data.actor?.login, data.label) {}

    override suspend fun gropiusTimelineItem(
        imsProject: IMSProject,
        service: SyncDataService,
        timelineItemConversionInformation: TimelineItemConversionInformation?
    ): Pair<List<TimelineItem>, TimelineItemConversionInformation> {
        val convInfo =
            timelineItemConversionInformation ?: TODOTimelineItemConversionInformation(imsProject.rawId!!, githubId);
        val githubService = service as GithubDataService
        if ((createdBy != null)) {
            val gropiusId = convInfo.gropiusId
            val event = if (gropiusId != null) githubService.neoOperations.findById<AddedLabelEvent>(
                gropiusId
            ) else AddedLabelEvent(createdAt, createdAt)
            if (event == null) {
                return listOf<TimelineItem>() to convInfo;
            }
            event.createdBy().value = githubService.userMapper.mapUser(imsProject, createdBy)
            event.lastModifiedBy().value = githubService.userMapper.mapUser(imsProject, createdBy)
            event.addedLabel().value = githubService.mapLabel(imsProject, label)
            return listOf<TimelineItem>(event) to convInfo;
        }
        return listOf<TimelineItem>() to convInfo;
    }
}

class UnassignedTimelineItem(
    githubId: String, createdAt: OffsetDateTime, val createdBy: String?, val user: String
) : OwnedGithubTimelineItem(githubId, createdAt) {
    constructor(data: UnassignedEventTimelineItemData) : this(
        data.id, data.createdAt, data.actor?.login, data.assignee?.userData()?.login!!
    ) {
    }

    override suspend fun gropiusTimelineItem(
        imsProject: IMSProject,
        service: SyncDataService,
        timelineItemConversionInformation: TimelineItemConversionInformation?
    ): Pair<List<TimelineItem>, TimelineItemConversionInformation> {
        val convInfo =
            timelineItemConversionInformation ?: TODOTimelineItemConversionInformation(imsProject.rawId!!, githubId);
        val githubService = service as GithubDataService
        if ((createdBy != null)) {
            val gropiusId = convInfo.gropiusId
            val event = if (gropiusId != null) githubService.neoOperations.findById<RemovedAssignmentEvent>(
                gropiusId
            ) else RemovedAssignmentEvent(createdAt, createdAt)
            if (event == null) {
                return listOf<TimelineItem>() to convInfo;
            }
            event.createdBy().value = githubService.userMapper.mapUser(imsProject, createdBy)
            event.lastModifiedBy().value = githubService.userMapper.mapUser(imsProject, createdBy)
            event.removedAssignment().value = TODO()
            return listOf<TimelineItem>(event) to convInfo;
        }
        return listOf<TimelineItem>() to convInfo;
    }
}

class AssignedTimelineItem(
    githubId: String, createdAt: OffsetDateTime, val createdBy: String?, val user: String
) : OwnedGithubTimelineItem(githubId, createdAt) {
    constructor(data: AssignedEventTimelineItemData) : this(
        data.id, data.createdAt, data.actor?.login, data.assignee?.userData()?.login!!
    ) {
    }

    override suspend fun gropiusTimelineItem(
        imsProject: IMSProject,
        service: SyncDataService,
        timelineItemConversionInformation: TimelineItemConversionInformation?
    ): Pair<List<TimelineItem>, TimelineItemConversionInformation> {
        val convInfo =
            timelineItemConversionInformation ?: TODOTimelineItemConversionInformation(imsProject.rawId!!, githubId);
        val githubService = service as GithubDataService
        if ((createdBy != null)) {
            val gropiusId = convInfo.gropiusId
            val event = if (gropiusId != null) githubService.neoOperations.findById<Assignment>(
                gropiusId
            ) else Assignment(createdAt, createdAt)
            if (event == null) {
                return listOf<TimelineItem>() to convInfo;
            }
            event.createdBy().value = githubService.userMapper.mapUser(imsProject, createdBy)
            event.lastModifiedBy().value = githubService.userMapper.mapUser(imsProject, createdBy)
            event.user().value = githubService.userMapper.mapUser(imsProject, user)
            return listOf<TimelineItem>(event) to convInfo;
        }
        return listOf<TimelineItem>() to convInfo;
    }
}

class ReopenedEventTimelineItem(githubId: String, createdAt: OffsetDateTime) :
    OwnedGithubTimelineItem(githubId, createdAt) {
    constructor(data: ReopenedEventTimelineItemData) : this(data.id, data.createdAt) {}

    override suspend fun gropiusTimelineItem(
        imsProject: IMSProject,
        service: SyncDataService,
        timelineItemConversionInformation: TimelineItemConversionInformation?
    ): Pair<List<TimelineItem>, TimelineItemConversionInformation> {
        val convInfo =
            timelineItemConversionInformation ?: TODOTimelineItemConversionInformation(imsProject.rawId!!, githubId);
        return listOf<TimelineItem>() to convInfo;
    }
}

class ClosedEventTimelineItem(githubId: String, createdAt: OffsetDateTime) :
    OwnedGithubTimelineItem(githubId, createdAt) {
    constructor(data: ClosedEventTimelineItemData) : this(data.id, data.createdAt) {}

    override suspend fun gropiusTimelineItem(
        imsProject: IMSProject,
        service: SyncDataService,
        timelineItemConversionInformation: TimelineItemConversionInformation?
    ): Pair<List<TimelineItem>, TimelineItemConversionInformation> {
        val convInfo =
            timelineItemConversionInformation ?: TODOTimelineItemConversionInformation(imsProject.rawId!!, githubId);
        return listOf<TimelineItem>() to convInfo;
    }
}

class IssueCommentTimelineItem(
    githubId: String,
    createdAt: OffsetDateTime,
    var body: String,
    val createdBy: String?,
    var recheckDone: Boolean = false
) : OwnedGithubTimelineItem(githubId, createdAt) {
    constructor(data: IssueCommentTimelineItemData) : this(
        data.id, data.createdAt, data.body, data.author?.login
    ) {
    }

    override suspend fun gropiusTimelineItem(
        imsProject: IMSProject,
        service: SyncDataService,
        timelineItemConversionInformation: TimelineItemConversionInformation?
    ): Pair<List<TimelineItem>, TimelineItemConversionInformation> {
        val convInfo =
            timelineItemConversionInformation ?: TODOTimelineItemConversionInformation(imsProject.rawId!!, githubId);
        val githubService = service as GithubDataService
        if ((createdBy != null)) {
            val gropiusId = convInfo.gropiusId
            val event = if (gropiusId != null) githubService.neoOperations.findById<IssueComment>(
                gropiusId
            ) else IssueComment(createdAt, createdAt, body, createdAt, false)
            if (event == null) {
                return listOf<TimelineItem>() to convInfo;
            }
            event.createdBy().value = githubService.userMapper.mapUser(imsProject, createdBy)
            event.lastModifiedBy().value = githubService.userMapper.mapUser(imsProject, createdBy)
            event.bodyLastEditedBy().value = githubService.userMapper.mapUser(imsProject, createdBy)
            return listOf<TimelineItem>(event) to convInfo;
        }
        return listOf<TimelineItem>() to convInfo;
    }
}

class UnknownTimelineItem(
    githubId: String, createdAt: OffsetDateTime
) : OwnedGithubTimelineItem(githubId, createdAt) {
    override suspend fun gropiusTimelineItem(
        imsProject: IMSProject,
        service: SyncDataService,
        timelineItemConversionInformation: TimelineItemConversionInformation?
    ): Pair<List<TimelineItem>, TimelineItemConversionInformation> {
        val convInfo =
            timelineItemConversionInformation ?: TODOTimelineItemConversionInformation(imsProject.rawId!!, githubId);
        return listOf<TimelineItem>() to convInfo;
    }
}
