package gropius.sync.github

import gropius.model.architecture.IMSIssue
import gropius.model.architecture.IMSProject
import gropius.model.issue.Issue
import gropius.repository.architecture.IMSIssueRepository
import gropius.sync.*
import gropius.sync.github.config.IMSConfigManager
import gropius.sync.github.config.IMSProjectConfig
import gropius.sync.github.generated.fragment.IssueData
import gropius.sync.github.repository.IssueInfoRepository
import gropius.sync.github.repository.RepositoryInfoRepository
import gropius.sync.github.repository.TimelineEventInfoRepository
import gropius.sync.github.utils.TimelineItemHandler
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.neo4j.cypherdsl.core.Cypher
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.neo4j.core.ReactiveNeo4jOperations
import org.springframework.data.neo4j.core.deleteAllById
import org.springframework.stereotype.Component

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
     * Create or read the IMSIssue for a given issue and IMSProject
     * @param imsProjectConfig Config of the imsProject to sync
     * @param issue Issue to connect the project to
     * @param issueData GitHub issue data containing url, number, ...
     */
    suspend fun ensureIMSIssue(
        imsProjectConfig: IMSProjectConfig, issue: Issue, issueData: IssueData
    ): IMSIssue {
        val imsProject: IMSProject? = null
        val node = Cypher.node(IMSIssue::class.simpleName).named("iMSIssue")
        val imsProjectNode = Cypher.node(IMSProject::class.simpleName)
            .withProperties(mapOf("id" to Cypher.anonParameter(imsProject?.rawId!!)))
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
            imsIssue.imsProject().value = imsProject!!
        } else {
            imsIssue = imsIssueList.removeFirst()
            neoOperations.deleteAllById<IMSIssue>(imsIssueList.map { it.rawId!! }).awaitSingleOrNull()
        }
        imsIssue.templatedFields["url"] = helper.objectMapper.writeValueAsString(issueData.url)
        imsIssue.templatedFields["id"] = helper.objectMapper.writeValueAsString(issueData.number)
        imsIssue.templatedFields["number"] = helper.objectMapper.writeValueAsString(issueData.number)
        //imsIssue.template().value = imsProjectConfig.imsConfig.imsTemplate.imsIssueTemplate().value
        imsIssue = neoOperations.save(imsIssue).awaitSingle()
        return imsIssue
    }
}
