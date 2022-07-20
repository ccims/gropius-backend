package gropius.sync.github

import gropius.sync.IssueCleaner
import gropius.sync.github.generated.fragment.IssueDataExtensive
import gropius.sync.github.generated.fragment.TimelineItemData
import gropius.sync.github.generated.fragment.TimelineItemData.Companion.asIssueComment
import gropius.sync.github.generated.fragment.TimelineItemData.Companion.asNode
import gropius.sync.github.model.IssueInfo
import gropius.sync.github.model.TimelineEventInfo
import gropius.sync.github.model.TimelineItemDataCache
import gropius.sync.github.repository.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Criteria.where
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Component
import java.time.OffsetDateTime

/**
 * Stateless component for the incoming part of the sync
 */
@Component
class Incoming(
    /**
     * Reference for the spring instance of RepositoryInfoRepository
     */
    private val repositoryInfoRepository: RepositoryInfoRepository,
    /**
     * Reference for the spring instance of IssueInfoRepository
     */
    private val issueInfoRepository: IssueInfoRepository,
    /**
     * Reference for the spring instance of UserInfoRepository
     */
    private val userInfoRepository: UserInfoRepository,
    /**
     * Reference for the spring instance of LabelInfoRepository
     */
    private val labelInfoRepository: LabelInfoRepository,
    /**
     * Reference for the spring instance of ReactiveMongoOperations
     */
    private val mongoOperations: ReactiveMongoOperations,
    /**
     * Reference for the spring instance of ReactiveMongoOperations
     */
    private val timelineEventInfoRepository: TimelineEventInfoRepository,
    /**
     * Reference for the spring instance of IssueCleaner
     */
    private val issueCleaner: IssueCleaner,
    /**
     * Reference for the spring instance of NodeSourcerer
     */
    private val nodeSourcerer: NodeSourcerer,
    /**
     * Reference for the spring instance of TimelineItemHandler
     */
    private val timelineItemHandler: TimelineItemHandler
) {

    /**
     * Mark issue as dirty
     * @param info The full dataset of an issue
     * @return The DateTime the issue was last changed
     */
    suspend fun issueModified(info: IssueDataExtensive): OffsetDateTime {
        val (neoIssue, mongoIssue) = nodeSourcerer.ensureIssue(info)
        mongoOperations.updateFirst(
            Query(where("_id").`is`(mongoIssue.id!!)),
            Update().set(IssueInfo::dirty.name, true),
            IssueInfo::class.java
        ).awaitSingle()
        return info.updatedAt
    }

    /**
     * Save a single timline event into the database
     * @param issue the issue the timeline belongs to
     * @param event a single timeline item
     * @return The time of the event or null for error
     */
    suspend fun handleTimelineEvent(issue: IssueInfo, event: TimelineItemData): OffsetDateTime? {
        val dbEntry = timelineEventInfoRepository.findByGithubId(event.asNode()!!.id)
        return if ((dbEntry != null) && !((event.asIssueComment()?.lastEditedAt != null) && (event.asIssueComment()!!.lastEditedAt!! > dbEntry.lastModifiedAt))) {
            dbEntry.lastModifiedAt
        } else {
            val (neoId, time) = timelineItemHandler.handleIssueModifiedItem(issue, event)
            if (time != null) {
                timelineEventInfoRepository.save(TimelineEventInfo(event.asNode()!!.id, neoId, time, event.__typename))
                    .awaitSingle()
            }
            time
        }
    }

    /**
     * Sync github to gropius
     */
    suspend fun sync() {
        val issueGrabber = IssueGrabber(repositoryInfoRepository, mongoOperations)
        issueGrabber.requestNewNodes()
        issueGrabber.iterate {
            issueModified(it)
        }
        for (issue in issueInfoRepository.findByDirtyIsTrue().toList()) {
            val timelineGrabber = TimelineGrabber(issueInfoRepository, mongoOperations, issue.githubId)
            timelineGrabber.requestNewNodes()
        }
        for (issueId in mongoOperations.query<TimelineItemDataCache>().matching(
            Query.query(Criteria.where(TimelineItemDataCache::attempts.name).not().gte(7))
        ).all().asFlow().map { it.issue }.toSet()) {
            val issue = issueInfoRepository.findByGithubId(issueId)!!
            val timelineGrabber = TimelineGrabber(issueInfoRepository, mongoOperations, issue.githubId)
            timelineGrabber.iterate {
                handleTimelineEvent(issue, it)
            }
            issueCleaner.cleanIssue(issue.neo4jId)
            mongoOperations.updateFirst(
                Query(where("_id").`is`(issue.id)), Update().set(IssueInfo::dirty.name, false), IssueInfo::class.java
            ).awaitSingle()
        }
    }
}