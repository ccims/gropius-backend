package gropius.sync.github

import gropius.sync.github.generated.IssueReadQuery
import gropius.sync.github.generated.TimelineReadQuery
import gropius.sync.github.generated.fragment.IssueCommentTimelineItemData
import gropius.sync.github.generated.fragment.TimelineItemData.Companion.asNode
import jakarta.transaction.Transactional
import kotlinx.coroutines.reactor.awaitSingle
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Document
abstract class NTimelineItem(
    @Indexed
    val githubId: String,
    @Indexed
    var createdAt: OffsetDateTime,
) {}

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
    val timelineItems: MutableList<NTimelineItem>,
    @Indexed
    var needsTimelineRequest: Boolean = true,
    @Indexed
    var needsCommentRequest: Boolean = true
) {
    @Id
    var id: ObjectId? = null
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
}

@Service
class IssuePileService(val issuePileRepository: IssuePileRepository) : IssuePileRepository by issuePileRepository {
    @Transactional
    suspend fun integrateIssue(
        imsProject: String, data: IssueReadQuery.Data.Repository.Issues.Node
    ) {
        val pile = issuePileRepository.findByImsProjectAndGithubId(imsProject, data.id) ?: IssuePileData(
            imsProject, data.id, data.title, data.body, data.updatedAt, mutableListOf()
        )
        pile.lastUpdate = data.updatedAt
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

    fun mapTimelineItem(data: TimelineReadQuery.Data.IssueNode.TimelineItems.Node): NTimelineItem? {
        return when (data) {
            is IssueCommentTimelineItemData -> IssueCommentTimelineItem(data)
            /*is ClosedEventTimelineItemData -> handleIssueClosed(imsProjectConfig, issue, event)
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
            is UnpinnedEventTimelineItemData -> Pair(null, event.createdAt)*/

            // Handle all events separately, as GitHub does not inherit the createdAt time anywhere
            /*is AddedToProjectEventTimelineItemData -> Pair(null, event.createdAt)
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
            is UnsubscribedEventTimelineItemData -> Pair(null, event.createdAt)*/
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
        if (ti != null) pile.timelineItems += ti
        issuePileRepository.save(pile).awaitSingle()
    }
}

@Document
class IssueCommentTimelineItem(
    githubId: String, createdAt: OffsetDateTime, val body: String
) : NTimelineItem(githubId, createdAt) {
    constructor(data: IssueCommentTimelineItemData) : this(data.id, data.createdAt, data.body) {}
}
