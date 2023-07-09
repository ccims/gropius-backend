package gropius.sync.github

import com.apollographql.apollo3.ApolloClient
import gropius.GithubConfigurationProperties
import gropius.model.architecture.IMS
import gropius.model.issue.Issue
import gropius.model.issue.Label
import gropius.model.issue.timeline.*
import gropius.model.user.GropiusUser
import gropius.model.user.IMSUser
import gropius.model.user.User
import gropius.repository.issue.IssueRepository
import gropius.repository.user.IMSUserRepository
import gropius.sync.JsonHelper
import gropius.sync.SyncNotificator
import gropius.sync.TokenManager
import gropius.sync.github.config.IMSProjectConfig
import gropius.sync.github.generated.*
import gropius.sync.github.generated.MutateAddLabelMutation.Data.AddLabelsToLabelable.Labelable.Companion.asIssue
import gropius.sync.github.generated.MutateCreateCommentMutation.Data.AddComment.CommentEdge.Node.Companion.asIssueTimelineItems
import gropius.sync.github.generated.MutateCreateLabelMutation.Data.CreateLabel.Label.Companion.labelData
import gropius.sync.github.generated.MutateRemoveLabelMutation.Data.RemoveLabelsFromLabelable.Labelable.Companion.asIssue
import gropius.sync.github.model.IssueInfo
import gropius.sync.github.model.LabelInfo
import gropius.sync.github.repository.IssueInfoRepository
import gropius.sync.github.repository.LabelInfoRepository
import gropius.sync.github.repository.TimelineEventInfoRepository
import gropius.sync.github.utils.TimelineItemHandler
import kotlinx.coroutines.reactor.awaitSingle
import org.neo4j.cypherdsl.core.Cypher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Stateless component for the outgoing part of the sync
 * @param issueInfoRepository Reference for the spring instance of IssueInfoRepository
 * @param timelineEventInfoRepository Reference for the spring instance of TimelineEventInfoRepository
 * @param helper Reference for the spring instance of JsonHelper
 * @param tokenManager Reference for the spring instance of TokenManager
 * @param issueRepository Reference for the spring instance of IssueRepository
 * @param imsUserRepository Reference for the spring instance of IMSUserRepository
 * @param incoming Reference for the spring instance of Incoming
 * @param timelineItemHandler Reference for the spring instance of TimelineItemHandler
 * @param githubConfigurationProperties Reference for the spring instance of GithubConfigurationProperties
 */
@Component
class Outgoing(
    private val issueInfoRepository: IssueInfoRepository,
    private val timelineEventInfoRepository: TimelineEventInfoRepository,
    private val helper: JsonHelper,
    private val tokenManager: TokenManager,
    private val issueRepository: IssueRepository,
    private val imsUserRepository: IMSUserRepository,
    private val incoming: Incoming,
    private val nodeSourcerer: NodeSourcerer,
    private val labelInfoRepository: LabelInfoRepository,
    private val timelineItemHandler: TimelineItemHandler,
    private val githubConfigurationProperties: GithubConfigurationProperties
) {
    /**
     * Logger used to print notifications
     */
    private val logger = LoggerFactory.getLogger(Outgoing::class.java)

    /**
     * Find the last consecutive list of blocks of the same searchLambda
     * @param relevantTimeline List of timeline items filtered for issue and sorted by date
     * @param searchLambda Lambda returning a value that is equal if the items should be considered equal
     * @return Consecutive same type timeline items
     */
    private suspend fun <T> findFinalBlock(
        relevantTimeline: List<TimelineItem>, searchLambda: suspend (TimelineItem) -> T
    ): List<TimelineItem> {
        val lastItem = relevantTimeline.last()
        val finalItems = mutableListOf<TimelineItem>()
        for (item in relevantTimeline.reversed()) {
            if (searchLambda(item) != searchLambda(lastItem)) {
                break
            }
            finalItems += item
        }
        return finalItems
    }

    /**
     * Find the last consecutive list of blocks of the same type
     * @param relevantTimeline List of timeline items filtered for issue and sorted by date
     * @return Consecutive same type timeline items
     */
    private suspend fun findFinalTypeBlock(relevantTimeline: List<TimelineItem>): List<TimelineItem> {
        return findFinalBlock(relevantTimeline) { it::class };
    }

    /**
     * Find token for an IMSUser
     * @param user user to search token for
     * @param imsProjectConfig active config
     * @return string if token found, null otherwise
     */
    private suspend fun extractIMSUserToken(imsProjectConfig: IMSProjectConfig, user: IMSUser): String? {
        if (user.ims().value == imsProjectConfig.imsConfig.ims) {
            val token = tokenManager.getGithubUserToken(user)
            if (token != null) {
                return token
            }
        }
        return null
    }

    /**
     * Find all IMSUsers intersecting the configured IMS and the given user
     * @param user user to search token for
     * @param imsProjectConfig active config
     * @return list of all IMSUsers
     */
    private suspend fun findAllIMSUsers(imsProjectConfig: IMSProjectConfig, user: User): List<IMSUser> {
        val node = Cypher.node(IMSUser::class.simpleName).named("iMSUser")
        val imsNode = Cypher.node(IMS::class.simpleName)
            .withProperties(mapOf("id" to Cypher.anonParameter(imsProjectConfig.imsConfig.ims.rawId!!)))
        val imsCondition = node.relationshipFrom(imsNode, IMS.USER).asCondition()
        val userNode = Cypher.node(GropiusUser::class.simpleName)
            .withProperties(mapOf("userId" to Cypher.anonParameter(user.rawId!!)))
        val userCondition = node.relationshipTo(userNode, IMSUser.GROPIUS_USER).asCondition()
        return imsUserRepository.findAll(
            imsCondition.and(userCondition)
        ).collectList().awaitSingle()
    }

    /**
     * Find token for a User or any similar enough user
     * @param user user to search token for
     * @param imsProjectConfig active config
     * @return string if token found, null otherwise
     */
    private suspend fun extractUserToken(imsProjectConfig: IMSProjectConfig, user: User): String? {
        val activeGropiusUser: GropiusUser = if (user is IMSUser) {
            val token = extractIMSUserToken(imsProjectConfig, user)
            if (token != null) {
                return token
            }
            user.gropiusUser().value ?: return null
        } else {
            user as GropiusUser
        }
        for (imsUser in findAllIMSUsers(imsProjectConfig, activeGropiusUser)) {
            val imsUserToken = extractIMSUserToken(imsProjectConfig, imsUser)
            if (imsUserToken != null) {
                return imsUserToken
            }
        }
        return null
    }

    /**
     * Create client to mutate on GitHub working as one of the users or the fallback
     * @param imsProjectConfig active config
     * @param userList List of users which have provided changes
     * @return the client initialized with a token for writing
     */
    private suspend fun createClient(imsProjectConfig: IMSProjectConfig, userList: Iterable<User>): ApolloClient {
        var token: String? = null
        for (user in userList) {
            token = extractUserToken(imsProjectConfig, user)
        }
        if (token == null) {
            token = tokenManager.getTokenForIMSUser(
                imsProjectConfig.imsConfig.ims, imsProjectConfig.imsConfig.readUser, null
            )
        }
        return ApolloClient.Builder().serverUrl(imsProjectConfig.imsConfig.graphQLUrl.toString())
            .addHttpHeader("Authorization", "bearer $token")
            .addHttpHeader("Accept", "application/json, application/vnd.github.bane-preview+json").build()
    }

    /**
     * Mutate an ReopenIssue upto GitHub
     * @param imsProjectConfig active config
     * @param issueInfo info of the issue containing the timeline item
     * @param userList users that have contributed to the event
     * @return List of functions that contain the actual mutation executors
     */
    private suspend fun githubReopenIssue(
        imsProjectConfig: IMSProjectConfig, issueInfo: IssueInfo, userList: Iterable<User>
    ): List<suspend () -> Unit> {
        logger.info("Scheduling reopening ${issueInfo.neo4jId}")
        return listOf {
            val client = createClient(imsProjectConfig, userList)
            val response = client.mutation(MutateReopenIssueMutation(issueInfo.githubId)).execute()
            val item = response.data?.reopenIssue?.issue?.timelineItems?.nodes?.lastOrNull()
            if (item != null) {
                incoming.handleTimelineEvent(imsProjectConfig, issueInfo, item)
            }
        }
    }

    /**
     * Mutate an CloseIssue upto GitHub
     * @param imsProjectConfig active config
     * @param issueInfo info of the issue containing the timeline item
     * @param userList users that have contributed to the event
     * @return List of functions that contain the actual mutation executors
     */
    private suspend fun githubCloseIssue(
        imsProjectConfig: IMSProjectConfig, issueInfo: IssueInfo, userList: Iterable<User>
    ): List<suspend () -> Unit> {
        logger.info("Scheduling closing ${issueInfo.neo4jId}")
        return listOf {
            val client = createClient(imsProjectConfig, userList)
            val response = client.mutation(MutateCloseIssueMutation(issueInfo.githubId)).execute()
            val item = response.data?.closeIssue?.issue?.timelineItems?.nodes?.lastOrNull()
            if (item != null) {
                incoming.handleTimelineEvent(imsProjectConfig, issueInfo, item)
            }
        }
    }

    /**
     * Mutate an AddedLabelEvent upto GitHub
     * @param imsProjectConfig active config
     * @param issueInfo info of the issue containing the timeline item
     * @param userList users that have contributed to the event
     * @param label the label that has been added
     * @return List of functions that contain the actual mutation executors
     */
    private suspend fun githubAddLabel(
        imsProjectConfig: IMSProjectConfig, issueInfo: IssueInfo, label: Label, userList: Iterable<User>
    ): List<suspend () -> Unit> {
        return listOf {
            val labelInfo = labelInfoRepository.findByNeo4jId(label.rawId!!)
            if (labelInfo != null) {
                logger.info("Adding existing ${label.name} (${label.rawId}) to ${issueInfo.neo4jId}")
                addExistingLabel(labelInfo, imsProjectConfig, userList, issueInfo)
            } else {
                logger.info("Adding new ${label.name} (${label.rawId}) to ${issueInfo.neo4jId}")
                addCreatedLabel(imsProjectConfig, userList, issueInfo, label)
            }
        }
    }

    /**
     * Create and add label to issue
     * @param imsProjectConfig active config
     * @param issueInfo info of the issue containing the timeline item
     * @param userList users that have contributed to the event
     * @param label the label that has been added
     * @return List of functions that contain the actual mutation executors
     */
    private suspend fun addCreatedLabel(
        imsProjectConfig: IMSProjectConfig, userList: Iterable<User>, issueInfo: IssueInfo, label: Label
    ) {

        val client = createClient(imsProjectConfig, userList)
        val repositoryId = client.query(RepositoryIDQuery(imsProjectConfig.repo.owner, imsProjectConfig.repo.repo))
            .execute().data?.repository?.id
        if (repositoryId != null) {
            val createLabelResponse = client.mutation(
                MutateCreateLabelMutation(
                    repositoryId, label.name, label.description, label.color
                )
            ).execute()
            if (createLabelResponse.errors != null) {
                logger.warn("Errors while syncing label: ${createLabelResponse.errors}")
            }
            val newLabel = createLabelResponse.data?.createLabel?.label?.labelData()
            if (newLabel != null) {
                nodeSourcerer.ensureLabel(imsProjectConfig, newLabel, label.rawId)
                val response = client.mutation(MutateAddLabelMutation(issueInfo.githubId, newLabel.id)).execute()
                if (response.errors != null) {
                    logger.warn("Errors while syncing label: ${createLabelResponse.errors}")
                }
                val item = response.data?.addLabelsToLabelable?.labelable?.asIssue()?.timelineItems?.nodes?.lastOrNull()
                if (item != null) {
                    incoming.handleTimelineEvent(imsProjectConfig, issueInfo, item)
                }
            }
        }
    }

    /**
     * Add existing label to issue
     * @param imsProjectConfig active config
     * @param issueInfo info of the issue containing the timeline item
     * @param userList users that have contributed to the event
     * @param labelInfo info about the label that has been added
     * @return List of functions that contain the actual mutation executors
     */
    private suspend fun addExistingLabel(
        labelInfo: LabelInfo, imsProjectConfig: IMSProjectConfig, userList: Iterable<User>, issueInfo: IssueInfo
    ) {
        if (labelInfo.url == imsProjectConfig.url) {
            val client = createClient(imsProjectConfig, userList)
            val response = client.mutation(MutateAddLabelMutation(issueInfo.githubId, labelInfo.githubId)).execute()
            val item = response.data?.addLabelsToLabelable?.labelable?.asIssue()?.timelineItems?.nodes?.lastOrNull()
            if (item != null) {
                incoming.handleTimelineEvent(imsProjectConfig, issueInfo, item)
            }
        }
    }

    /**
     * Mutate an RemovedLabelEvent upto GitHub
     * @param imsProjectConfig active config
     * @param issueInfo info of the issue containing the timeline item
     * @param userList users that have contributed to the event
     * @param label the label that has been removed
     * @return List of functions that contain the actual mutation executors
     */
    private suspend fun githubRemoveLabel(
        imsProjectConfig: IMSProjectConfig, issueInfo: IssueInfo, label: Label, userList: Iterable<User>
    ): List<suspend () -> Unit> {
        logger.info("Scheduling removing ${label.name} (${label.rawId}) from ${issueInfo.neo4jId}")
        return listOf {
            val labelId = labelInfoRepository.findByNeo4jId(label.rawId!!)
            if (labelId != null) {
                val client = createClient(imsProjectConfig, userList)
                val response =
                    client.mutation(MutateRemoveLabelMutation(issueInfo.githubId, labelId.githubId)).execute()
                val item =
                    response.data?.removeLabelsFromLabelable?.labelable?.asIssue()?.timelineItems?.nodes?.lastOrNull()
                if (item != null) {
                    incoming.handleTimelineEvent(imsProjectConfig, issueInfo, item)
                }
            }
        }
    }

    /**
     * Mutate an IssueComment upto GitHub
     * @param imsProjectConfig active config
     * @param issueInfo info of the issue containing the timeline item
     * @param user user that has contributed to the event
     * @param comment the comment to post
     * @return List of functions that contain the actual mutation executors
     */
    private suspend fun githubPostComment(
        imsProjectConfig: IMSProjectConfig, issueInfo: IssueInfo, comment: IssueComment, user: User
    ): List<suspend () -> Unit> {
        logger.info("Scheduling comment ${comment.body} on ${issueInfo.neo4jId}")
        return listOf {
            val client = createClient(imsProjectConfig, listOf(user))
            val response = client.mutation(MutateCreateCommentMutation(issueInfo.githubId, comment.body)).execute()
            val item = response.data?.addComment?.commentEdge?.node?.asIssueTimelineItems()
            if (item != null) {
                incoming.handleTimelineEventIssueComment(imsProjectConfig, issueInfo, item, comment.rawId)
            }
        }
    }

    /**
     * Mutate the open/close state of an issue upto GitHub
     * @param imsProjectConfig active config
     * @param issueInfo info of the issue containing the timeline item
     * @param timeline TimelineItems for this issue
     * @return List of functions that contain the actual mutation executors
     */
    private suspend fun pushReopenClose(
        imsProjectConfig: IMSProjectConfig, issueInfo: IssueInfo, timeline: List<TimelineItem>
    ): List<suspend () -> Unit> {
        val relevantTimeline = timeline.filterIsInstance<StateChangedEvent>()
        if (relevantTimeline.isEmpty()) {
            return listOf()
        }
        val finalBlock = findFinalBlock(relevantTimeline) { (it as StateChangedEvent).newState().value.isOpen }
        for (item in finalBlock) {
            if (timelineEventInfoRepository.findByNeo4jId(item.rawId!!) != null) {
                return listOf()
            }
        }
        return handleFinalStateChangedBlock(finalBlock, relevantTimeline, imsProjectConfig, issueInfo)
    }

    /**
     * Mutate the comments of an issue upto GitHub
     * @param imsProjectConfig active config
     * @param issueInfo info of the issue containing the timeline item
     * @param timeline TimelineItems for this issue
     * @return List of functions that contain the actual mutation executors
     */
    private suspend fun pushComments(
        imsProjectConfig: IMSProjectConfig, issueInfo: IssueInfo, timeline: List<TimelineItem>
    ): List<suspend () -> Unit> {
        return timeline.mapNotNull { it as? IssueComment }
            .filter { timelineEventInfoRepository.findByNeo4jId(it.rawId!!) == null }.flatMap {
                githubPostComment(
                    imsProjectConfig, issueInfo, it, it.lastModifiedBy().value
                )
            }
    }

    /**
     * Convert the finalBlock of an stateChanged relevantTimeline into the mutations
     * @param imsProjectConfig active config
     * @param issueInfo info of the issue containing the timeline item
     * @param relevantTimeline TimelineItems for this issue filtered to reopen/close and sorted by date
     * @param finalBlock Final similarly typed block of relevantTimeline
     * @return List of functions that contain the actual mutation executors
     */
    private suspend fun handleFinalStateChangedBlock(
        finalBlock: List<TimelineItem>,
        relevantTimeline: List<TimelineItem>,
        imsProjectConfig: IMSProjectConfig,
        issueInfo: IssueInfo
    ): List<suspend () -> Unit> {
        return handleFinalStateChangedToClosedBlock(
            finalBlock, relevantTimeline, imsProjectConfig, issueInfo
        ) + handleFinalStateChangedToOpenBlock(finalBlock, relevantTimeline, imsProjectConfig, issueInfo)
    }

    /**
     * Convert the finalBlock of an stateChanged relevantTimeline into the mutations
     * Handles only events which change it to open
     * @param imsProjectConfig active config
     * @param issueInfo info of the issue containing the timeline item
     * @param relevantTimeline TimelineItems for this issue filtered to reopen/close and sorted by date
     * @param finalBlock Final similarly typed block of relevantTimeline
     * @return List of functions that contain the actual mutation executors
     */
    private suspend fun handleFinalStateChangedToOpenBlock(
        finalBlock: List<TimelineItem>,
        relevantTimeline: List<TimelineItem>,
        imsProjectConfig: IMSProjectConfig,
        issueInfo: IssueInfo
    ): List<suspend () -> Unit> {
        return if (shouldSyncType({ it is StateChangedEvent && it.newState().value.isOpen },
                { it is StateChangedEvent && !it.newState().value.isOpen },
                finalBlock,
                relevantTimeline,
                true
            )
        ) {
            githubReopenIssue(imsProjectConfig, issueInfo, finalBlock.map { it.lastModifiedBy().value })
        } else {
            emptyList()
        }
    }

    /**
     * Convert the finalBlock of an stateChanged relevantTimeline into the mutations
     * Handles only events which change it to closed
     * @param imsProjectConfig active config
     * @param issueInfo info of the issue containing the timeline item
     * @param relevantTimeline TimelineItems for this issue filtered to reopen/close and sorted by date
     * @param finalBlock Final similarly typed block of relevantTimeline
     * @return List of functions that contain the actual mutation executors
     */
    private suspend fun handleFinalStateChangedToClosedBlock(
        finalBlock: List<TimelineItem>,
        relevantTimeline: List<TimelineItem>,
        imsProjectConfig: IMSProjectConfig,
        issueInfo: IssueInfo
    ): List<suspend () -> Unit> {
        return if (shouldSyncType({ it is StateChangedEvent && !it.newState().value.isOpen },
                { it is StateChangedEvent && it.newState().value.isOpen },
                finalBlock,
                relevantTimeline,
                false
            )
        ) {
            githubCloseIssue(imsProjectConfig, issueInfo, finalBlock.map { it.lastModifiedBy().value })
        } else {
            emptyList()
        }
    }

    /**
     * Check if TimelineItem should be synced or ignored
     * @param AddingItem Item type with the same semantic as the item to add
     * @param RemovingItem Item type invalidating the AddingItem
     * @param finalBlock the last block of similar items that should be checked for syncing
     * @param relevantTimeline Sorted part of the timeline containing only TimelineItems interacting with finalBlock
     * @param restoresDefaultState if the timeline item converges the state of the issue towards the state of an empty issue
     * @return true if and only if there are unsynced changes that should be synced to GitHub
     */
    private suspend inline fun <reified AddingItem : TimelineItem, reified RemovingItem : TimelineItem> shouldSyncType(
        finalBlock: List<TimelineItem>, relevantTimeline: List<TimelineItem>, restoresDefaultState: Boolean
    ): Boolean {
        return shouldSyncType({ it is AddingItem },
            { it is RemovingItem },
            finalBlock,
            relevantTimeline,
            restoresDefaultState
        )
    }

    /**
     * Check if TimelineItem should be synced or ignored
     * @param isAddingItem filter for items with the same semantic as the item to add
     * @param isRemovingItem filter for items invalidating the items matching [isAddingItem]
     * @param finalBlock the last block of similar items that should be checked for syncing
     * @param relevantTimeline Sorted part of the timeline containing only TimelineItems interacting with finalBlock
     * @param restoresDefaultState if the timeline item converges the state of the issue towards the state of an empty issue
     * @return true if and only if there are unsynced changes that should be synced to GitHub
     */
    private suspend fun shouldSyncType(
        isAddingItem: suspend (TimelineItem) -> Boolean,
        isRemovingItem: suspend (TimelineItem) -> Boolean,
        finalBlock: List<TimelineItem>,
        relevantTimeline: List<TimelineItem>,
        restoresDefaultState: Boolean
    ): Boolean {
        if (isAddingItem(finalBlock.last())) {
            val lastNegativeEvent = relevantTimeline.filter { isRemovingItem(it) }
                .lastOrNull { timelineEventInfoRepository.findByNeo4jId(it.rawId!!)?.githubId != null }
            if (lastNegativeEvent == null) {
                return !restoresDefaultState
            } else {
                if (relevantTimeline.filter { isAddingItem(it) }.filter { it.createdAt > lastNegativeEvent.createdAt }
                        .firstOrNull { timelineEventInfoRepository.findByNeo4jId(it.rawId!!)?.githubId != null } == null) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Mutate the labels of an issue upto GitHub
     * @param imsProjectConfig active config
     * @param issueInfo info of the issue containing the timeline item
     * @param timeline TimelineItems for this issue
     * @return List of functions that contain the actual mutation executors
     */
    private suspend fun pushLabels(
        imsProjectConfig: IMSProjectConfig, issueInfo: IssueInfo, timeline: List<TimelineItem>
    ): List<suspend () -> Unit> {
        val groups = timeline.filter { (it is AddedLabelEvent) || (it is RemovedLabelEvent) }.groupBy {
            when (it) {
                is AddedLabelEvent -> it.addedLabel().value
                is RemovedLabelEvent -> it.removedLabel().value
                else -> throw IllegalStateException()
            }
        }
        val collectedMutations = mutableListOf<suspend () -> Unit>()
        for ((label, relevantTimeline) in groups) {
            if (label != null) {
                handleSingleLabel(relevantTimeline, collectedMutations, imsProjectConfig, issueInfo, label)
            }
        }
        return collectedMutations
    }

    /**
     * Mutate the labels of an issue upto GitHub
     * @param imsProjectConfig active config
     * @param issueInfo info of the issue containing the timeline item
     * @param relevantTimeline Sorted labeling TimelineItems filtered for the same label
     * @param collectedMutations List to insert the mutations into
     * @param label The label this group manipulates
     * @return true if the item has already been synced and should not be synced again
     */
    private suspend fun handleSingleLabel(
        relevantTimeline: List<TimelineItem>,
        collectedMutations: MutableList<suspend () -> Unit>,
        imsProjectConfig: IMSProjectConfig,
        issueInfo: IssueInfo,
        label: Label
    ): Boolean {
        val finalBlock = findFinalTypeBlock(relevantTimeline)
        for (item in finalBlock) {
            if (timelineEventInfoRepository.findByNeo4jId(item.rawId!!)?.githubId != null) {
                return true
            }
        }
        if (shouldSyncType<AddedLabelEvent, RemovedLabelEvent>(
                finalBlock, relevantTimeline, false
            )
        ) {
            collectedMutations += githubAddLabel(imsProjectConfig,
                issueInfo,
                label,
                finalBlock.map { it.lastModifiedBy().value })
        }
        if (shouldSyncType<RemovedLabelEvent, AddedLabelEvent>(
                finalBlock, relevantTimeline, true
            )
        ) {
            collectedMutations += githubRemoveLabel(imsProjectConfig,
                issueInfo,
                label,
                finalBlock.map { it.lastModifiedBy().value })
        }
        return false
    }

    /**
     * Mutate the labels of an issue upto GitHub
     * @param imsProjectConfig active config
     * @param issueInfo info of the issue containing the timeline item
     * @param relevantTimeline Sorted labeling TimelineItems filtered for the same label
     * @param collectedMutations List to insert the mutations into
     * @param label The label this group manipulates
     * @return true if the item has already been synced and should not be synced again
     */
    private suspend fun createIssue(
        imsProjectConfig: IMSProjectConfig, issue: Issue
    ): List<suspend () -> Unit> {
        val collectedMutations = mutableListOf<suspend () -> Unit>()
        val client = createClient(imsProjectConfig, listOf(issue.createdBy().value))
        val repositoryId = client.query(RepositoryIDQuery(imsProjectConfig.repo.owner, imsProjectConfig.repo.repo))
            .execute().data?.repository?.id
        if (repositoryId != null) {
            val response =
                client.mutation(MutateCreateIssueMutation(repositoryId, issue.title, issue.body().value.body)).execute()
            val item = response.data?.createIssue?.issue
            if (item != null) {
                nodeSourcerer.ensureIssue(imsProjectConfig, item, issue.rawId)
            }
        }
        return collectedMutations
    }

    /**
     * Check a modified issue for mutateable changes
     * @param issue Issue to check
     * @param imsProjectConfig active config
     * @param issueInfo IssueInfo containing the IssueInfo used for writing updates back to mongo
     */
    suspend fun issueModified(
        imsProjectConfig: IMSProjectConfig, issue: Issue, issueInfo: IssueInfo
    ): List<suspend () -> Unit> {
        logger.info("Issue (${issue.title}) ${issue.rawId} has outgoing modifications")
        val collectedMutations = mutableListOf<suspend () -> Unit>()
        val timeline = issue.timelineItems().toList().sortedBy { it.createdAt }
        collectedMutations += pushReopenClose(imsProjectConfig, issueInfo, timeline)
        collectedMutations += pushLabels(imsProjectConfig, issueInfo, timeline)
        collectedMutations += pushComments(imsProjectConfig, issueInfo, timeline)
        return collectedMutations
    }

    /**
     * Sync issues of one IMSProject
     * @param imsProjectConfig the config of the IMSProject
     */
    suspend fun syncIssues(imsProjectConfig: IMSProjectConfig) {
        val collectedMutations = mutableListOf<suspend () -> Unit>()
        for (issue in imsProjectConfig.imsProject.trackable().value.issues()) {
            val issueInfo = issueInfoRepository.findByUrlAndNeo4jId(imsProjectConfig.url, issue.rawId!!)
            if (issueInfo?.githubId != null) {
                if ((issueInfo.lastOutgoingSync == null) || (issue.lastUpdatedAt != issueInfo.lastOutgoingSync)) {
                    collectedMutations += issueModified(imsProjectConfig, issue, issueInfo)
                    issueInfo.lastOutgoingSync = issue.lastUpdatedAt
                    issueInfoRepository.save(issueInfo).awaitSingle()
                }
            } else {
                collectedMutations += createIssue(imsProjectConfig, issue)
            }
        }
        if (collectedMutations.size > githubConfigurationProperties.maxMutationCount) {
            throw SyncNotificator.NotificatedError("SYNC_GITHUB_TOO_MANY_MUTATIONS")
        }
        logger.info("Pushing ${collectedMutations.size} mutations")
        for (mutation in collectedMutations) {
            mutation()
        }
    }
}
