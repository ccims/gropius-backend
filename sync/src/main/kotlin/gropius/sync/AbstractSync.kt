package gropius.sync

import gropius.model.architecture.IMS
import gropius.model.architecture.IMSIssue
import gropius.model.architecture.IMSProject
import gropius.model.issue.Issue
import gropius.model.issue.Label
import gropius.model.issue.timeline.*
import gropius.model.template.IMSTemplate
import gropius.model.template.IssueState
import gropius.model.template.IssueType
import gropius.model.user.GropiusUser
import gropius.model.user.User
import gropius.repository.common.NodeRepository
import gropius.repository.issue.IssueRepository
import gropius.service.issue.IssueAggregationUpdater
import io.github.graphglue.model.Node
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.neo4j.core.ReactiveNeo4jOperations
import org.springframework.data.neo4j.core.findAll
import org.springframework.data.neo4j.core.findById
import org.springframework.stereotype.Component

/**
 * Interface for syncing data
 */
interface DataFetcher {

    /**
     * Execute the ata fetching for a given list of IMSProjects
     */
    suspend fun fetchData(imsProjects: List<IMSProject>);
}

/**
 * Fallback Timeline Item Conversion
 * @param imsProject IMS project to sync
 * @param githubId GitHub ID of the timeline item
 */
class DummyTimelineItemConversionInformation(
    imsProject: String, githubId: String
) : TimelineItemConversionInformation(imsProject, githubId, null) {}

/**
 * Bean accessor
 * @param neoOperations Neo4j operations bean
 * @param issueConversionInformationService IssueConversionInformationService bean
 * @param issueRepository IssueRepository bean
 * @param timelineItemConversionInformationService TimelineItemConversionInformationService bean
 * @param syncNotificator SyncNotificator bean
 * @param issueCleaner IssueCleaner bean
 */
@Component
class CollectedSyncInfo(
    @Qualifier("graphglueNeo4jOperations")
    val neoOperations: ReactiveNeo4jOperations,
    val issueConversionInformationService: IssueConversionInformationService,
    val issueRepository: IssueRepository,
    val timelineItemConversionInformationService: TimelineItemConversionInformationService,
    val syncNotificator: SyncNotificator,
    val issueCleaner: IssueCleaner,
    val nodeRepository: NodeRepository
) {}

/**
 * Simple static issue dereplicator request implmeneation
 */
class SimpleIssueDereplicatorRequest(
    override val dummyUser: User,
    override val neoOperations: ReactiveNeo4jOperations,
    override val issueRepository: IssueRepository
) : IssueDereplicatorRequest {}

/**
 * Base class for sync
 * @param collectedSyncInfo Bean accessor
 */
abstract class AbstractSync(
    val collectedSyncInfo: CollectedSyncInfo
) : DataFetcher {
    private val logger = LoggerFactory.getLogger(AbstractSync::class.java)

    /**
     * Currently active dereplicator
     */
    val issueDereplicator: IssueDereplicator = HeuristicDereplicator(1.0, 1.0)

    /**
     * Get the currently unsynced issue for an IMSProject
     * @param imsProject IMS project to sync
     * @return List of unsynced issues
     */
    abstract suspend fun findUnsyncedIssues(imsProject: IMSProject): List<IncomingIssue>;

    /**
     * Get the sync data which should be given to further methods
     */
    abstract fun syncDataService(): SyncDataService

    /**
     * Get the template used to create new issues
     */
    abstract suspend fun findTemplates(): Set<IMSTemplate>

    /**
     * Incorporate a comment
     * @param imsProject IMS project to sync
     * @param issueId GitHub ID of the issue
     * @param issueComment Comment to sync
     * @param users List of users involved in this timeline item, sorted with most relevant first
     * @return Conversion information
     */
    abstract suspend fun syncComment(
        imsProject: IMSProject, issueId: String, issueComment: IssueComment, users: List<User>
    ): TimelineItemConversionInformation?

    /**
     * Incorporate a title change
     * @param imsProject IMS project to sync
     * @param issueId GitHub ID of the issue
     * @param newTitle New title of the issue
     * @param users List of users involved in this timeline item, sorted with most relevant first
     * @return Conversion information
     */
    abstract suspend fun syncTitleChange(
        imsProject: IMSProject, issueId: String, newTitle: String, users: List<User>
    ): TimelineItemConversionInformation?

    /**
     * Incorporate a state change
     * @param imsProject IMS project to sync
     * @param issueId GitHub ID of the issue
     * @param newState New state of the issue
     * @param users List of users involved in this timeline item, sorted with most relevant first
     * @return Conversion information
     */
    abstract suspend fun syncStateChange(
        imsProject: IMSProject, issueId: String, newState: IssueState, users: List<User>
    ): TimelineItemConversionInformation?

    /**
     * Incorporate an added label
     * @param imsProject IMS project to sync
     * @param issueId GitHub ID of the issue
     * @param label Label to sync
     * @param users List of users involved in this timeline item, sorted with most relevant first
     * @return Conversion information
     */
    abstract suspend fun syncAddedLabel(
        imsProject: IMSProject, issueId: String, label: Label, users: List<User>
    ): TimelineItemConversionInformation?

    /**
     * Incorporate a removed label
     * @param imsProject IMS project to sync
     * @param issueId GitHub ID of the issue
     * @param label Label to sync
     * @param users List of users involved in this timeline item, sorted with most relevant first
     * @return Conversion information
     */
    abstract suspend fun syncRemovedLabel(
        imsProject: IMSProject, issueId: String, label: Label, users: List<User>
    ): TimelineItemConversionInformation?

    /**
     * Create an issue on the IMS
     * @param imsProject IMS project to sync
     * @param issue Issue to sync
     * @return Conversion information
     */
    abstract suspend fun createOutgoingIssue(imsProject: IMSProject, issue: Issue): IssueConversionInformation?;

    /**
     * Check if Outgoing Sync is Enabled
     * @param imsProject IMS project to check for
     * @return true if and only if outgoing sync is enabled
     */
    abstract suspend fun isOutgoingEnabled(imsProject: IMSProject): Boolean

    /**
     * Check if Outgoing Sync of Labels is Enabled
     * @param imsProject IMS project to check for
     * @return true if and only if outgoing sync of labels is enabled
     */
    abstract suspend fun isOutgoingLabelsEnabled(imsProject: IMSProject): Boolean

    /**
     * Check if Outgoing Sync of Comments is Enabled
     * @param imsProject IMS project to check for
     * @return true if and only if outgoing sync of comments is enabled
     */
    abstract suspend fun isOutgoingCommentsEnabled(imsProject: IMSProject): Boolean

    /**
     * Check if Outgoing Sync of Title Changes is Enabled
     * @param imsProject IMS project to check for
     * @return true if and only if outgoing sync of title changes is enabled
     */
    abstract suspend fun isOutgoingTitleChangedEnabled(imsProject: IMSProject): Boolean

    /**
     * Check if Outgoing Sync of Assignments is Enabled
     * @param imsProject IMS project to check for
     * @return true if and only if outgoing sync of assignments is enabled
     */
    abstract suspend fun isOutgoingAssignmentsEnabled(imsProject: IMSProject): Boolean

    /**
     * Check if Outgoing Sync of State Changes is Enabled
     * @param imsProject IMS project to check for
     * @return true if and only if outgoing sync of state changes is enabled
     */
    abstract suspend fun isOutgoingStatesEnabled(imsProject: IMSProject): Boolean

    /**
     * Sync Incoming Part
     * @param imsProject IMS project to sync
     */
    suspend fun doIncoming(imsProject: IMSProject) {
        val dereplicatorRequest = SimpleIssueDereplicatorRequest(
            collectedSyncInfo.neoOperations.findAll<GropiusUser>().filter { it.username == "gropius" }.firstOrNull()
                ?: collectedSyncInfo.neoOperations.save(
                    GropiusUser(
                        "Gropius", null, null, "gropius", false
                    )
                ).awaitSingle(), collectedSyncInfo.neoOperations, collectedSyncInfo.issueRepository
        )
        try {
            findUnsyncedIssues(imsProject).forEach {
                syncIncomingIssue(imsProject, it, dereplicatorRequest)
                //} catch (e: SyncNotificator.NotificatedError) {
                //    syncNotificator.sendNotification(
                //        imsIssue, SyncNotificator.NotificationDummy(e)
                //    )
                //} catch (e: Exception) {
                //    logger.warn("Error in issue sync", e)
                //}
            }
        } catch (e: SyncNotificator.NotificatedError) {
            logger.warn("Error in IMSProject sync", e)
            collectedSyncInfo.syncNotificator.sendNotification(
                imsProject, SyncNotificator.NotificationDummy(e)
            )
        } catch (e: Exception) {
            logger.warn("Error in IMS sync", e)
        }
    }

    /**
     * Sync one incoming issue
     * @param imsProject IMS project to sync
     * @param incomingIssue Issue to sync
     * @param dereplicatorRequest Request for the dereplicator
     * @return Conversion information
     */
    private suspend fun syncIncomingIssue(
        imsProject: IMSProject, incomingIssue: IncomingIssue, dereplicatorRequest: SimpleIssueDereplicatorRequest
    ) {
        val issueInfo = collectedSyncInfo.issueConversionInformationService.findByImsProjectAndGithubId(
            imsProject.rawId!!, incomingIssue.identification()
        ) ?: IssueConversionInformation(imsProject.rawId!!, incomingIssue.identification(), null)
        var issue = if (issueInfo.gropiusId != null) collectedSyncInfo.issueRepository.findById(issueInfo.gropiusId!!)
            .awaitSingle() else incomingIssue.createIssue(imsProject, syncDataService())
        val oldType = issue.type().value
        val oldState = issue.state().value
        val isNewIssue = issue.rawId == null
        if (issue.imsIssues().none { it.imsProject().value == imsProject }) {
            val imsIssue = IMSIssue(mutableMapOf())
            imsIssue.issue().value = issue
            imsIssue.imsProject().value = imsProject
            imsIssue.template().value = imsProject.template().value.partOf().value.imsIssueTemplate().value
            issue.imsIssues() += imsIssue
        }
        val imsIssue = issue.imsIssues().single { it.imsProject().value == imsProject }
        incomingIssue.fillImsIssueTemplatedFields(imsIssue.templatedFields, syncDataService())
        val nodesToSave = mutableListOf<Node>(issue)
        val savedNodeHandlers = mutableListOf<suspend (node: Node) -> Unit>()
        val timelineItems = incomingIssue.incomingTimelineItems(syncDataService())
        for (timelineItem in timelineItems) {
            syncIncomingTimelineItem(
                imsProject, timelineItem, issue, dereplicatorRequest, nodesToSave, savedNodeHandlers
            )
        }
        var dereplicationResult: IssueDereplicatorIssueResult? = null
        if (issue.rawId == null) {
            dereplicationResult = issueDereplicator.validateIssue(imsProject, issue, dereplicatorRequest)
            issue = dereplicationResult.resultingIssue
            for (fakeSyncedItem in dereplicationResult.fakeSyncedItems) {
                nodesToSave.add(fakeSyncedItem)
                savedNodeHandlers.add { updatedNode ->
                    val conversionInfo = DummyTimelineItemConversionInformation(
                        imsProject.rawId!!, (updatedNode as TimelineItem).rawId!!
                    )
                    conversionInfo.gropiusId = (updatedNode as TimelineItem).rawId
                    collectedSyncInfo.timelineItemConversionInformationService.save(conversionInfo).awaitSingle()
                }
            }
        }
        val savedList = collectedSyncInfo.neoOperations.saveAll(nodesToSave).collectList().awaitSingle()
        val savedIssue = savedList.removeFirst()
        if (issue.rawId == null) issue = savedIssue as Issue
        val updater = IssueAggregationUpdater()
        if (isNewIssue) {
            updater.addedIssueToTrackable(issue, imsProject.trackable().value)
        }
        updater.changedIssueStateOrType(
            issue,
            collectedSyncInfo.neoOperations.findById<IssueState>(oldState.rawId!!)!!,
            collectedSyncInfo.neoOperations.findById<IssueType>(oldType.rawId!!)!!
        )
        savedList.zip(savedNodeHandlers).forEach { (savedNode, savedNodeHandler) ->
            savedNodeHandler(savedNode)
        }
        if (issueInfo.gropiusId == null) {
            issueInfo.gropiusId = issue.rawId!!
        }
        collectedSyncInfo.issueConversionInformationService.save(issueInfo).awaitSingle()
        collectedSyncInfo.issueCleaner.cleanIssue(issue.rawId!!)
        incomingIssue.markDone(syncDataService())
        updater.save(collectedSyncInfo.nodeRepository)
    }

    /**
     * Sync one incoming timeline item
     * @param imsProject IMS project to sync
     * @param timelineItem Timeline item to sync
     * @param issue Issue to sync
     * @param dereplicatorRequest Request for the dereplicator
     * @param nodesToSave List of nodes to save
     * @param savedNodeHandlers List of handlers for saved nodes
     * @return Conversion information
     */
    private suspend fun syncIncomingTimelineItem(
        imsProject: IMSProject,
        timelineItem: IncomingTimelineItem,
        issue: Issue,
        dereplicatorRequest: SimpleIssueDereplicatorRequest,
        nodesToSave: MutableList<Node>,
        savedNodeHandlers: MutableList<suspend (node: Node) -> Unit>
    ) {
        logger.info("Syncing incoming for issue ${issue.rawId} $timelineItem ${timelineItem.identification()}")
        val oldInfo = collectedSyncInfo.timelineItemConversionInformationService.findByImsProjectAndGithubId(
            imsProject.rawId!!, timelineItem.identification()
        )
        var (timelineItem, newInfo) = timelineItem.gropiusTimelineItem(
            imsProject, syncDataService(), oldInfo, issue
        )
        if (issue.rawId != null) {
            val dereplicationResult = issueDereplicator.validateTimelineItem(issue, timelineItem, dereplicatorRequest)
            timelineItem = dereplicationResult.resultingTimelineItems
        }
        if (timelineItem.isNotEmpty()) {//TODO: Handle multiple
            timelineItem.forEach { it.issue().value = issue }
            issue.timelineItems() += timelineItem
            issue.issueComments() += timelineItem.mapNotNull { it as? IssueComment }
            nodesToSave.add(timelineItem.single())
            savedNodeHandlers.add { savedNode ->
                newInfo.gropiusId = (savedNode as TimelineItem).rawId
                if (oldInfo?.id != null) {
                    newInfo.id = oldInfo.id;
                }
                collectedSyncInfo.timelineItemConversionInformationService.save(newInfo).awaitSingle()
            }
        }
    }

    /**
     * Find the last consecutive list of blocks of the same searchLambda
     * @param relevantTimeline List of timeline items filtered for issue and sorted by date
     * @param searchLambda Lambda returning a value that is equal if the items should be considered equal
     * @return Consecutive same type timeline items
     */
    private suspend inline fun <T, reified BT : TimelineItem> findFinalBlock(
        relevantTimeline: List<BT>, searchLambda: suspend (BT) -> T
    ): List<BT> {
        val lastItem = relevantTimeline.last()
        val finalItems = mutableListOf<BT>()
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
     * Check if TimelineItem should be synced or ignored
     * @param AddingItem Item type with the same semantic as the item to add
     * @param RemovingItem Item type invalidating the AddingItem
     * @param finalBlock the last block of similar items that should be checked for syncing
     * @param relevantTimeline Sorted part of the timeline containing only TimelineItems interacting with finalBlock
     * @param restoresDefaultState if the timeline item converges the state of the issue towards the state of an empty issue
     * @return true if and only if there are unsynced changes that should be synced to GitHub
     */
    private suspend inline fun <reified AddingItem : TimelineItem, reified RemovingItem : TimelineItem> shouldSyncType(
        imsProject: IMSProject,
        finalBlock: List<TimelineItem>,
        relevantTimeline: List<TimelineItem>,
        restoresDefaultState: Boolean
    ): Boolean {
        return shouldSyncType(
            imsProject,
            { it is AddingItem },
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
        imsProject: IMSProject,
        isAddingItem: suspend (TimelineItem) -> Boolean,
        isRemovingItem: suspend (TimelineItem) -> Boolean,
        finalBlock: List<TimelineItem>,
        relevantTimeline: List<TimelineItem>,
        restoresDefaultState: Boolean
    ): Boolean {
        if (isAddingItem(finalBlock.last())) {
            val lastNegativeEvent = relevantTimeline.filter { isRemovingItem(it) }.lastOrNull {
                collectedSyncInfo.timelineItemConversionInformationService.findByImsProjectAndGropiusId(
                    imsProject.rawId!!, it.rawId!!
                )?.githubId != null
            }
            logger.trace("LastNegativeEvent $lastNegativeEvent")
            if (lastNegativeEvent == null) {
                return !restoresDefaultState
            } else {
                if (relevantTimeline.filter { isAddingItem(it) }.filter { it.createdAt > lastNegativeEvent.createdAt }
                        .firstOrNull {
                            collectedSyncInfo.timelineItemConversionInformationService.findByImsProjectAndGropiusId(
                                imsProject.rawId!!, it.rawId!!
                            )?.githubId != null
                        } == null) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Sync Outgoing issues
     * @param imsProject IMS project to sync
     */
    open suspend fun doOutgoing(imsProject: IMSProject) {
        if (!isOutgoingEnabled(imsProject)) {
            return
        }
        imsProject.trackable().value.issues().forEach { issue ->
            var issueInfo = collectedSyncInfo.issueConversionInformationService.findByImsProjectAndGropiusId(
                imsProject.rawId!!, issue.rawId!!
            )
            if (issueInfo == null) {
                val outgoingIssue = createOutgoingIssue(imsProject, issue)
                if (outgoingIssue != null) {
                    outgoingIssue.gropiusId = issue.rawId!!
                    issueInfo = collectedSyncInfo.issueConversionInformationService.save(outgoingIssue).awaitSingle()
                }
            }
            if (issueInfo != null) {
                val timeline = issue.timelineItems().toList().sortedBy { it.createdAt }
                if (isOutgoingCommentsEnabled(imsProject)) {
                    syncOutgoingComments(timeline, imsProject, issueInfo)
                }
                if (isOutgoingLabelsEnabled(imsProject)) {
                    syncOutgoingLabels(timeline, imsProject, issueInfo)
                }
                if (isOutgoingTitleChangedEnabled(imsProject)) {
                    syncOutgoingTitleChanges(timeline, imsProject, issueInfo)
                }
                if (isOutgoingAssignmentsEnabled(imsProject)) {
                    syncOutgoingAssignments(timeline, imsProject, issueInfo)
                }
                if (isOutgoingStatesEnabled(imsProject)) {
                    syncOutgoingStateChanges(timeline, imsProject, issueInfo)
                }
            }
        }
    }

    /**
     * Sync Outgoing Labels
     * @param timeline Timeline of the issue
     * @param imsProject IMS project to sync
     * @param issueInfo Issue to sync
     */
    private suspend fun syncOutgoingLabels(
        timeline: List<TimelineItem>, imsProject: IMSProject, issueInfo: IssueConversionInformation
    ) {
        val groups = timeline.filter { (it is AddedLabelEvent) || (it is RemovedLabelEvent) }.groupBy {
            when (it) {
                is AddedLabelEvent -> it.addedLabel().value
                is RemovedLabelEvent -> it.removedLabel().value
                else -> throw IllegalStateException()
            }
        }
        val collectedMutations = mutableListOf<suspend () -> Unit>()
        for ((label, relevantTimeline) in groups) {
            syncOutgoingSingleLabel(relevantTimeline, imsProject, issueInfo, label)
        }
    }

    /**
     * Sync Outgoing Single Label
     * @param relevantTimeline Timeline of the issue filtered for the label
     * @param imsProject IMS project to sync
     * @param issueInfo Issue to sync
     * @param label Label to sync
     */
    private suspend fun syncOutgoingSingleLabel(
        relevantTimeline: List<TimelineItem>,
        imsProject: IMSProject,
        issueInfo: IssueConversionInformation,
        label: Label?
    ) {
        var labelIsSynced = false
        val finalBlock = findFinalTypeBlock(relevantTimeline)
        for (item in finalBlock) {
            val relevantEvent = collectedSyncInfo.timelineItemConversionInformationService.findByImsProjectAndGropiusId(
                imsProject.rawId!!, item.rawId!!
            )
            if (relevantEvent?.githubId != null) {
                labelIsSynced = true
            }
        }
        if (!labelIsSynced) {
            if (shouldSyncType<RemovedLabelEvent, AddedLabelEvent>(
                    imsProject, finalBlock, relevantTimeline, true
                )
            ) {
                val conversionInformation = syncRemovedLabel(imsProject,
                    issueInfo.githubId,
                    label!!,
                    finalBlock.map { it.lastModifiedBy().value })
                if (conversionInformation != null) {
                    conversionInformation.gropiusId = finalBlock.map { it.rawId!! }.first()
                    collectedSyncInfo.timelineItemConversionInformationService.save(
                        conversionInformation
                    ).awaitSingle()
                }
            }
            if (shouldSyncType<AddedLabelEvent, RemovedLabelEvent>(
                    imsProject, finalBlock, relevantTimeline, false
                )
            ) {
                val conversionInformation = syncAddedLabel(imsProject,
                    issueInfo.githubId,
                    label!!,
                    finalBlock.map { it.lastModifiedBy().value })
                if (conversionInformation != null) {
                    conversionInformation.gropiusId = finalBlock.map { it.rawId!! }.first()
                    collectedSyncInfo.timelineItemConversionInformationService.save(
                        conversionInformation
                    ).awaitSingle()
                }
            }
        }
    }

    /**
     * Sync Outgoing Comments
     * @param timeline Timeline of the issue
     * @param imsProject IMS project to sync
     * @param issueInfo Issue to sync
     */
    private suspend fun syncOutgoingComments(
        timeline: List<TimelineItem>, imsProject: IMSProject, issueInfo: IssueConversionInformation
    ) {
        timeline.mapNotNull { it as? IssueComment }.filter {
            collectedSyncInfo.timelineItemConversionInformationService.findByImsProjectAndGropiusId(
                imsProject.rawId!!, it.rawId!!
            ) == null
        }.forEach {
            val conversionInformation = syncComment(
                imsProject,
                issueInfo.githubId,
                it,
                listOf(it.createdBy().value, it.lastModifiedBy().value, it.bodyLastEditedBy().value)
            )
            if (conversionInformation != null) {
                conversionInformation.gropiusId = it.rawId!!
                collectedSyncInfo.timelineItemConversionInformationService.save(conversionInformation).awaitSingle()
            }
        }
    }

    /**
     * Sync Outgoing Title Changes
     * @param timeline Timeline of the issue
     * @param imsProject IMS project to sync
     * @param issueInfo Issue to sync
     */
    private suspend fun syncOutgoingTitleChanges(
        timeline: List<TimelineItem>, imsProject: IMSProject, issueInfo: IssueConversionInformation
    ) {
        val relevantTimeline = timeline.mapNotNull { it as? TitleChangedEvent }
        if (relevantTimeline.isEmpty()) return
        val finalBlock = findFinalBlock(relevantTimeline) { it.newTitle }
        if (finalBlock.none {
                collectedSyncInfo.timelineItemConversionInformationService.findByImsProjectAndGropiusId(
                    imsProject.rawId!!, it.rawId!!
                ) != null
            }) {
            syncTitleChange(imsProject,
                issueInfo.githubId,
                finalBlock.first().newTitle,
                finalBlock.map { it.createdBy().value })
        }
    }

    /**
     * Sync Outgoing State Changes
     * @param timeline Timeline of the issue
     * @param imsProject IMS project to sync
     * @param issueInfo Issue to sync
     */
    private suspend fun syncOutgoingStateChanges(
        timeline: List<TimelineItem>, imsProject: IMSProject, issueInfo: IssueConversionInformation
    ) {
        val relevantTimeline = timeline.mapNotNull { it as? StateChangedEvent }
        if (relevantTimeline.isEmpty()) return
        val finalBlock = findFinalBlock(relevantTimeline) { it.newState().value }
        logger.trace("finalBlock: $finalBlock")
        if (finalBlock.none {
                collectedSyncInfo.timelineItemConversionInformationService.findByImsProjectAndGropiusId(
                    imsProject.rawId!!, it.rawId!!
                ) != null
            }) {
            logger.trace("syncOutgoingStateChanges: $finalBlock")
            syncStateChange(imsProject,
                issueInfo.githubId,
                finalBlock.first().newState().value,
                finalBlock.map { it.lastModifiedBy().value })
        }
    }

    /**
     * Sync Outgoing Assignments
     * @param timeline Timeline of the issue
     * @param imsProject IMS project to sync
     * @param issueInfo Issue to sync
     */
    private suspend fun syncOutgoingAssignments(
        timeline: List<TimelineItem>, imsProject: IMSProject, issueInfo: IssueConversionInformation
    ) {
    }

    /**
     * Sync all data
     */
    suspend fun sync() {
        val imsTemplates = findTemplates()
        logger.info("Found ${imsTemplates.size} IMSTemplate")
        val imss = mutableListOf<IMS>()
        for (imsTemplate in imsTemplates) imss += imsTemplate.usedIn()
        logger.info("Found ${imss.size} IMS")
        val imsProjects = mutableListOf<IMSProject>()
        for (ims in imss) imsProjects += ims.projects()
        logger.info("Found ${imsProjects.size} IMSProject")
        fetchData(imsProjects)
        for (imsProject in imsProjects) {
            doIncoming(imsProject)
            doOutgoing(imsProject)
        }
    }
}
