package gropius.sync.github

import com.apollographql.apollo3.ApolloClient
import gropius.model.architecture.IMSIssue
import gropius.model.architecture.IMSProject
import gropius.model.issue.Issue
import gropius.repository.architecture.IMSIssueRepository
import gropius.sync.*
import gropius.sync.github.config.IMSConfigManager
import gropius.sync.github.config.IMSProjectConfig
import gropius.sync.github.generated.fragment.IssueData
import gropius.sync.github.generated.fragment.IssueDataExtensive
import gropius.sync.github.generated.fragment.TimelineItemData
import gropius.sync.github.generated.fragment.TimelineItemData.Companion.asIssueComment
import gropius.sync.github.generated.fragment.TimelineItemData.Companion.asNode
import gropius.sync.github.model.IssueInfo
import gropius.sync.github.model.TimelineEventInfo
import gropius.sync.github.repository.IssueInfoRepository
import gropius.sync.github.repository.RepositoryInfoRepository
import gropius.sync.github.repository.TimelineEventInfoRepository
import gropius.sync.github.utils.IssueGrabber
import gropius.sync.github.utils.TimelineGrabber
import gropius.sync.github.utils.TimelineItemHandler
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.neo4j.cypherdsl.core.Cypher
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Criteria.where
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.neo4j.core.ReactiveNeo4jOperations
import org.springframework.data.neo4j.core.deleteAllById
import org.springframework.data.neo4j.core.findById
import org.springframework.stereotype.Component
import java.time.OffsetDateTime

/**
 * Stateless component for the incoming part of the sync
 * @param helper Reference for the spring instance of JsonHelper
 * @param imsConfigManager Reference for the spring instance of IMSConfigManager
 * @param neoOperations Reference for the spring instance of ReactiveNeo4jOperations
 * @param syncNotificator Reference for the spring instance of SyncNotificator
 * @param tokenManager Reference for the spring instance of TokenManager
 * @param repositoryInfoRepository Reference for the spring instance of RepositoryInfoRepository
 * @param issueInfoRepository Reference for the spring instance of IssueInfoRepository
 * @param mongoOperations Reference for the spring instance of ReactiveMongoOperations
 * @param timelineEventInfoRepository Reference for the spring instance of TimelineEventInfoRepository
 * @param issueCleaner Reference for the spring instance of IssueCleaner
 * @param nodeSourcerer Reference for the spring instance of NodeSourcerer
 * @param timelineItemHandler Reference for the spring instance of TimelineItemHandler
 * @param imsIssueRepository Reference for the spring instance of IMSIssueRepository
 */
@Component
class Incoming(
    private val repositoryInfoRepository: RepositoryInfoRepository,
    private val issueInfoRepository: IssueInfoRepository,
    private val mongoOperations: ReactiveMongoOperations,
    private val timelineEventInfoRepository: TimelineEventInfoRepository,
    private val issueCleaner: IssueCleaner,
    private val nodeSourcerer: NodeSourcerer,
    private val timelineItemHandler: TimelineItemHandler,
    @Qualifier("graphglueNeo4jOperations")
    private val neoOperations: ReactiveNeo4jOperations,
    private val helper: JsonHelper,
    private val imsConfigManager: IMSConfigManager,
    private val syncNotificator: SyncNotificator,
    private val tokenManager: TokenManager,
    private val imsIssueRepository: IMSIssueRepository
) {

    /**
     * Logger used to print notifications
     */
    private val logger = LoggerFactory.getLogger(Incoming::class.java)

    /**
     * Mark issue as dirty
     * @param info The full dataset of an issue
     * @param imsProjectConfig Config of the active project
     * @return The DateTime the issue was last changed
     */
    private suspend fun issueModified(imsProjectConfig: IMSProjectConfig, info: IssueDataExtensive): OffsetDateTime {
        val (_, mongoIssue) = nodeSourcerer.ensureIssue(imsProjectConfig, info)
        mongoOperations.updateFirst(
            Query(where("_id").`is`(mongoIssue.id!!)), Update().set(IssueInfo::dirty.name, true), IssueInfo::class.java
        ).awaitSingle()
        return info.updatedAt
    }

    /**
     * Save a single timeline event into the database
     * @param issueInfo the issue the timeline belongs to
     * @param event a single timeline item
     * @param imsProjectConfig Config of the active project
     * @return The time of the event or null for error
     */
    @Suppress("UNUSED_VALUE")
    suspend fun handleTimelineEvent(
        imsProjectConfig: IMSProjectConfig, issueInfo: IssueInfo, event: TimelineItemData
    ): OffsetDateTime? {
        val dbEntry = timelineEventInfoRepository.findByUrlAndGithubId(imsProjectConfig.url, event.asNode()!!.id)
        return if (event.asIssueComment() != null) {
            handleTimelineEventIssueComment(imsProjectConfig, issueInfo, event, dbEntry?.neo4jId)
        } else {
            dbEntry?.lastModifiedAt ?: handleTimelineEventNonIssueComment(imsProjectConfig, issueInfo, event)
        }
    }

    /**
     * Save any timeline item except issue comment into the database
     * @param issueInfo the issue the timeline belongs to
     * @param event a single timeline item
     * @param imsProjectConfig Config of the active project
     * @return The time of the event or null for error
     */
    @Suppress("UNUSED_VALUE")
    private suspend fun handleTimelineEventNonIssueComment(
        imsProjectConfig: IMSProjectConfig, issueInfo: IssueInfo, event: TimelineItemData
    ): OffsetDateTime? {
        val (neoId, time) = timelineItemHandler.handleIssueModifiedItem(imsProjectConfig, issueInfo, event)
        if (time != null) {
            timelineEventInfoRepository.save(
                TimelineEventInfo(
                    event.asNode()!!.id, neoId, time, event.__typename, imsProjectConfig.url
                )
            ).awaitSingle()
            var issue = neoOperations.findById<Issue>(issueInfo.neo4jId)!!
            issue.lastUpdatedAt = maxOf(issue.lastUpdatedAt, time)
            issue = neoOperations.save(issue).awaitSingle()
        }
        return time
    }

    /**
     * Save an issue comment of the timeline into the database
     * @param issueInfo the issue the timeline belongs to
     * @param event a single timeline item
     * @param imsProjectConfig Config of the active project
     * @param neo4jID ID of the issue if it already exists in db
     * @return The time of the event or null for error
     */
    @Suppress("UNUSED_VALUE")
    suspend fun handleTimelineEventIssueComment(
        imsProjectConfig: IMSProjectConfig, issueInfo: IssueInfo, event: TimelineItemData, neo4jID: String?
    ): OffsetDateTime? {
        val (neoId, time) = timelineItemHandler.handleIssueComment(
            imsProjectConfig, issueInfo, event.asIssueComment()!!, neo4jID
        )
        if (time != null) {
            val node =
                (if (neoId != null) timelineEventInfoRepository.findByNeo4jId(neoId) else null) ?: TimelineEventInfo(
                    event.asNode()!!.id, neoId, time, event.__typename, imsProjectConfig.url
                )
            node.githubId = event.asNode()!!.id
            node.neo4jId = neoId
            node.lastModifiedAt = time
            timelineEventInfoRepository.save(node).awaitSingle()
            var issue = neoOperations.findById<Issue>(issueInfo.neo4jId)!!
            issue.lastUpdatedAt = maxOf(issue.lastUpdatedAt, time)
            issue = neoOperations.save(issue).awaitSingle()
        }
        return time
    }

    /**
     * Create or read the IMSIssue for a given issue and IMSProject
     * @param imsProjectConfig Config of the imsProject to sync
     * @param issue Issue to connect the project to
     * @param issueData GitHub issue data containing url, number, ...
     */
    suspend fun ensureIMSIssue(
        imsProjectConfig: IMSProjectConfig, issue: Issue, issueData: IssueData
    ): IMSIssue {
        val node = Cypher.node(IMSIssue::class.simpleName).named("iMSIssue")
        val imsProjectNode = Cypher.node(IMSProject::class.simpleName)
            .withProperties(mapOf("id" to Cypher.anonParameter(imsProjectConfig.imsProject.rawId!!)))
        val issueNode =
            Cypher.node(Issue::class.simpleName).withProperties(mapOf("id" to Cypher.anonParameter(issue.rawId!!)))
        val imsProjectCondition = node.relationshipTo(imsProjectNode, IMSIssue.PROJECT).asCondition()
        val issueCondition = node.relationshipTo(issueNode, IMSIssue.ISSUE).asCondition()
        val imsIssueList =
            imsIssueRepository.findAll(imsProjectCondition.and(issueCondition)).collectList().awaitSingle()
        var imsIssue: IMSIssue
        if (imsIssueList.size == 0) {
            imsIssue = IMSIssue(mutableMapOf())
            imsIssue.issue().value = issue
            imsIssue.imsProject().value = imsProjectConfig.imsProject
        } else {
            imsIssue = imsIssueList.removeFirst()
            neoOperations.deleteAllById<IMSIssue>(imsIssueList.map { it.rawId!! }).awaitSingleOrNull()
        }
        imsIssue.templatedFields["url"] = helper.objectMapper.writeValueAsString(issueData.url)
        imsIssue.templatedFields["id"] = helper.objectMapper.writeValueAsString(issueData.number)
        imsIssue.templatedFields["number"] = helper.objectMapper.writeValueAsString(issueData.number)
        imsIssue.template().value = imsProjectConfig.imsConfig.imsTemplate.imsIssueTemplate().value
        imsIssue = neoOperations.save(imsIssue).awaitSingle()
        return imsIssue
    }

    /**
     * Sync one IMSProject
     * @param imsProjectConfig the config of the IMSProject
     * @param apolloClient the client to use4 for grpahql queries
     */
    suspend fun syncProject(imsProjectConfig: IMSProjectConfig, apolloClient: ApolloClient) {
        val issueGrabber = IssueGrabber(
            imsProjectConfig.repo, repositoryInfoRepository, mongoOperations, apolloClient, imsProjectConfig
        )
        issueGrabber.requestNewNodes()
        issueGrabber.iterate {
            issueModified(imsProjectConfig, it)
        }
        for (issue in issueInfoRepository.findByUrlAndDirtyIsTrue(imsProjectConfig.url).toList()) {
            val timelineGrabber = TimelineGrabber(
                issueInfoRepository, mongoOperations, issue.githubId, apolloClient, imsProjectConfig
            )
            timelineGrabber.requestNewNodes()
        }
    }

    /**
     * Sync issues of one IMSProject
     * @param imsProjectConfig the config of the IMSProject
     * @param apolloClient the client to use4 for grpahql queries
     */
    suspend fun syncIssues(imsProjectConfig: IMSProjectConfig, apolloClient: ApolloClient) {
        syncProject(imsProjectConfig, apolloClient)
        for (issueInfo in issueInfoRepository.findByUrlAndDirtyIsTrue(imsProjectConfig.url).toList()) {
            val issue = neoOperations.findById<Issue>(issueInfo.neo4jId)!!
            val imsIssue = ensureIMSIssue(imsProjectConfig, issue, issueInfo.issueData)
            try {
                grabIssueTimelineItems(issueInfo, apolloClient, imsProjectConfig)
            } catch (e: SyncNotificator.NotificatedError) {
                syncNotificator.sendNotification(
                    imsIssue, SyncNotificator.NotificationDummy(e)
                )
            } catch (e: Exception) {
                logger.warn("Error in issue sync", e)
            }
        }
    }

    /**
     * Sync issues of one IMSProject
     * @param imsProjectConfig the config of the IMSProject
     * @param apolloClient the client to use4 for grpahql queries
     * @param issueInfo The info of the issue to sync
     */
    private suspend fun grabIssueTimelineItems(
        issueInfo: IssueInfo, apolloClient: ApolloClient, imsProjectConfig: IMSProjectConfig
    ) {
        val timelineGrabber = TimelineGrabber(
            issueInfoRepository, mongoOperations, issueInfo.githubId, apolloClient, imsProjectConfig
        )
        val errorInserting = timelineGrabber.iterate {
            handleTimelineEvent(imsProjectConfig, issueInfo, it)
        }
        issueCleaner.cleanIssue(issueInfo.neo4jId)
        if (!errorInserting) {
            logger.info("Finished issue: " + issueInfo.id!!.toHexString())
            mongoOperations.updateFirst(
                Query(where("_id").`is`(issueInfo.id)),
                Update().set(IssueInfo::dirty.name, false),
                IssueInfo::class.java
            ).awaitSingle()
        }
    }
}
