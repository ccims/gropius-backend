package gropius.sync

import gropius.model.architecture.IMS
import gropius.model.architecture.IMSIssue
import gropius.model.architecture.IMSProject
import gropius.model.architecture.Trackable
import gropius.model.issue.Issue
import gropius.model.issue.Label
import gropius.model.issue.timeline.*
import gropius.model.template.IMSTemplate
import gropius.model.template.IssueState
import gropius.model.template.IssueType
import gropius.model.user.GropiusUser
import gropius.model.user.IMSUser
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
     * Get the template used to create new issues
     */
    open suspend fun labelStateMap(imsProject: IMSProject): Map<String, String> {
        return mapOf()
    }

    /**
     * Incorporate a comment
     * @param imsProject IMS project to sync
     * @param issueId Remote ID of the issue
     * @param issueComment Comment to sync
     * @param users List of users involved in this timeline item, sorted with most relevant first
     * @return Conversion information
     */
    open suspend fun syncComment(
        imsProject: IMSProject, issueId: String, issueComment: IssueComment, users: List<User>
    ): TimelineItemConversionInformation? {
        return syncFallbackComment(imsProject, issueId, issueComment.body, issueComment, users)
    }

    /**
     * Incorporate a fallback comment
     * @param imsProject IMS project to sync
     * @param issueId Remote ID of the issue
     * @param comment Comment to sync
     * @param original TimelineItem it fell back from
     * @param users List of users involved in this timeline item, sorted with most relevant first
     * @return Conversion information
     */
    abstract suspend fun syncFallbackComment(
        imsProject: IMSProject, issueId: String, comment: String, original: TimelineItem?, users: List<User>
    ): TimelineItemConversionInformation?

    /**
     * Incorporate a title change
     * @param imsProject IMS project to sync
     * @param issueId Remote ID of the issue
     * @param newTitle New title of the issue
     * @param users List of users involved in this timeline item, sorted with most relevant first
     * @return Conversion information
     */
    open suspend fun syncTitleChange(
        imsProject: IMSProject, issueId: String, newTitle: String, users: List<User>
    ): TimelineItemConversionInformation? {
        return syncFallbackComment(imsProject, issueId, "Gropius Title changed to $newTitle", null, users)
    }

    /**
     * Incorporate a state change
     * @param imsProject IMS project to sync
     * @param issueId Remote ID of the issue
     * @param newState New state of the issue
     * @param users List of users involved in this timeline item, sorted with most relevant first
     * @return Conversion information
     */
    abstract suspend fun syncStateChange(
        imsProject: IMSProject, issueId: String, newState: IssueState, users: List<User>
    ): TimelineItemConversionInformation?

    /**
     * Incorporate a templated field change
     * @param imsProject IMS project to sync
     * @param issueId Remote ID of the issue
     * @param fieldChangedEvent Event describing the field change
     * @param users List of users involved in this timeline item, sorted with most relevant first
     * @return Conversion information
     */
    open suspend fun syncTemplatedField(
        imsProject: IMSProject, issueId: String, fieldChangedEvent: TemplatedFieldChangedEvent, users: List<User>
    ): TimelineItemConversionInformation? {
        return syncFallbackComment(
            imsProject,
            issueId,
            "Gropius Field ${fieldChangedEvent.fieldName} changed to ${fieldChangedEvent.newValue}",
            fieldChangedEvent,
            users
        )
    }

    /**
     * Incorporate an added label
     * @param imsProject IMS project to sync
     * @param issueId Remote ID of the issue
     * @param label Label to sync
     * @param users List of users involved in this timeline item, sorted with most relevant first
     * @return Conversion information
     */
    open suspend fun syncAddedLabel(
        imsProject: IMSProject, issueId: String, label: Label, users: List<User>
    ): TimelineItemConversionInformation? {
        return syncFallbackComment(imsProject, issueId, "Gropius Label ${label.name} added", null, users)
    }

    /**
     * Incorporate a removed label
     * @param imsProject IMS project to sync
     * @param issueId Remote ID of the issue
     * @param label Label to sync
     * @param users List of users involved in this timeline item, sorted with most relevant first
     * @return Conversion information
     */
    open suspend fun syncRemovedLabel(
        imsProject: IMSProject, issueId: String, label: Label, users: List<User>
    ): TimelineItemConversionInformation? {
        return syncFallbackComment(imsProject, issueId, "Gropius Label ${label.name} removed", null, users)
    }

    /**
     * Incorporate an added assignment
     * @param imsProject IMS project to sync
     * @param issueId Remote ID of the issue
     * @param assignment Assignment to sync
     * @param users List of users involved in this timeline item, sorted with most relevant first
     * @return Conversion information
     */
    abstract suspend fun syncSingleAssigned(
        imsProject: IMSProject, issueId: String, assignment: Assignment, users: List<User>
    ): TimelineItemConversionInformation?

    /**
     * Incorporate a removed assignment
     * @param imsProject IMS project to sync
     * @param issueId Remote ID of the issue
     * @param assignment Assignment to sync
     * @param users List of users involved in this timeline item, sorted with most relevant first
     * @return Conversion information
     */
    abstract suspend fun syncSingleUnassigned(
        imsProject: IMSProject, issueId: String, assignment: Assignment, users: List<User>
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
     * Check if Outgoing Sync of TemplatedFields is Enabled
     * @param imsProject IMS project to check for
     * @return true if and only if outgoing sync of templatedFields is enabled
     */
    abstract suspend fun isOutgoingTemplatedFieldsEnabled(imsProject: IMSProject): Boolean

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
    private suspend fun doIncoming(imsProject: IMSProject) {
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
                try {
                    syncIncomingIssue(imsProject, it, dereplicatorRequest)/*
                    } catch (e: SyncNotificator.NotificatedError) {
                        syncNotificator.sendNotification(
                            imsIssue, SyncNotificator.NotificationDummy(e)
                        )*/
                } catch (e: Exception) {
                    logger.warn("Exception in issue sync", e)
                }
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
        oldImsProject: IMSProject, incomingIssue: IncomingIssue, dereplicatorRequest: SimpleIssueDereplicatorRequest
    ) {
        val imsProject = collectedSyncInfo.neoOperations.findById<IMSProject>(oldImsProject.rawId!!)!!
        val issueInfo = collectedSyncInfo.issueConversionInformationService.findByImsProjectAndGithubId(
            imsProject.rawId!!, incomingIssue.identification()
        ) ?: IssueConversionInformation(imsProject.rawId!!, incomingIssue.identification(), null)
        var issue = if (issueInfo.gropiusId != null) collectedSyncInfo.issueRepository.findById(issueInfo.gropiusId!!)
            .awaitSingle() else incomingIssue.createIssue(imsProject, syncDataService())
        val oldType = issue.type().value
        val oldState = issue.state().value
        val isNewIssue = !issue.isPersisted
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
        if (isNewIssue) {
            dereplicationResult = issueDereplicator.validateIssue(imsProject, issue, dereplicatorRequest)
            dereplicationResult.resultingIssue
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
        issue = savedList.removeFirst() as Issue
        val updater = IssueAggregationUpdater()
        updater.internalUpdatedNodes += issue
        collectedSyncInfo.issueCleaner.cleanIssue(issue)
        if (isNewIssue) {
            updater.addedIssueToTrackable(
                issue, collectedSyncInfo.neoOperations.findById<Trackable>(imsProject.trackable().value.rawId!!)!!
            )
        } else {
            updater.changedIssueStateOrType(
                issue,
                collectedSyncInfo.neoOperations.findById<IssueState>(oldState.rawId!!)!!,
                collectedSyncInfo.neoOperations.findById<IssueType>(oldType.rawId!!)!!
            )
        }
        savedList.zip(savedNodeHandlers).forEach { (savedNode, savedNodeHandler) ->
            savedNodeHandler(savedNode)
        }
        if (issueInfo.gropiusId == null) {
            issueInfo.gropiusId = issue.rawId!!
        }
        collectedSyncInfo.issueConversionInformationService.save(issueInfo).awaitSingle()
        updater.save(collectedSyncInfo.nodeRepository)
        incomingIssue.markDone(syncDataService())
    }

    /**
     * Lookup a state by its ID
     * @param id ID of the state
     * @return State with the given ID or null if given null
     */
    private suspend fun lookupState(id: String?): IssueState? {
        if (id != null) {
            return collectedSyncInfo.neoOperations.findById<IssueState>(id)!!
        }
        return null
    }

    /**
     * Find the state before the relevant titmeline item
     * @param issue The Issue to work on
     * @param timelineItem The TimelineItem to find the state before
     * @return List of currently applied labels
     * @return The state before the timeline item
     */
    private suspend fun integrateLabelToStateFindCurrentState(
        issue: Issue, timelineItem: TimelineItem
    ): Pair<Set<Label>, IssueState> {
        var lastState: IssueState = issue.state().value
        val activeLabels = mutableSetOf<Label>()
        issue.timelineItems().filter { it.createdAt < timelineItem.createdAt }.sortedBy { it.createdAt }.forEach {
            val addingItem = it as? AddedLabelEvent
            if (addingItem != null) {
                activeLabels.add(addingItem.addedLabel().value!!)
            }
            val removingItem = it as? RemovedLabelEvent
            if (removingItem != null) {
                activeLabels.add(removingItem.removedLabel().value!!)
            }
            val stateChangedEvent = it as? StateChangedEvent
            if (stateChangedEvent != null) {
                lastState = stateChangedEvent.newState().value
            }
        }
        return activeLabels to lastState
    }

    /**
     * Process an added label
     * @param timelineItems List of timeline items
     * @param issue Issue the timeline is on
     * @param imsProject IMS project to sync
     * @param timelineItem Timeline item to process
     * @param lastState State before the timeline item
     * @param mappedStates List of states mapped to labels
     */
    private suspend fun integrateLabelToStateAddedLabel(
        timelineItems: MutableList<TimelineItem>,
        issue: Issue,
        imsProject: IMSProject,
        timelineItem: AddedLabelEvent,
        lastState: IssueState,
        mappedStates: List<IssueState>
    ) {
        val labelStateMap = this.labelStateMap(imsProject)
        val mappedState = lookupState(labelStateMap[timelineItem.addedLabel().value?.name])
        if ((mappedState != null) && !mappedStates.contains(mappedState)) {
            timelineItems.remove(timelineItem)
            val newStateChange = StateChangedEvent(
                timelineItem.createdAt, timelineItem.lastModifiedAt
            )
            newStateChange.oldState().value = lastState
            newStateChange.newState().value = mappedState
            newStateChange.createdBy().value = timelineItem.createdBy().value
            newStateChange.lastModifiedBy().value = timelineItem.lastModifiedBy().value
            timelineItems.add(newStateChange)
            issue.timelineItems().add(newStateChange)
        }
    }

    /**
     * Process an removed label
     * @param timelineItems List of timeline items
     * @param issue Issue the timeline is on
     * @param imsProject IMS project to sync
     * @param timelineItem Timeline item to process
     * @param lastState State before the timeline item
     * @param mappedStates List of states mapped to labels
     */
    private suspend fun integrateLabelToStateRemovedLabel(
        timelineItems: MutableList<TimelineItem>,
        issue: Issue,
        imsProject: IMSProject,
        timelineItem: RemovedLabelEvent,
        lastState: IssueState,
        mappedStates: List<IssueState>
    ) {
        val labelStateMap = this.labelStateMap(imsProject)
        val mappedState = lookupState(labelStateMap[timelineItem.removedLabel().value?.name])
        if ((mappedState != null) && !mappedStates.contains(mappedState)) {
            timelineItems.remove(timelineItem)
            val newStateChange = StateChangedEvent(
                timelineItem.createdAt, timelineItem.lastModifiedAt
            )
            newStateChange.oldState().value = lastState
            newStateChange.newState().value =
                mappedStates.firstOrNull() ?: lastState//TODO("Restore State out of nothing")
            newStateChange.createdBy().value = timelineItem.createdBy().value
            newStateChange.lastModifiedBy().value = timelineItem.lastModifiedBy().value
            timelineItems.add(newStateChange)
            issue.timelineItems().add(newStateChange)
        }
    }

    /**
     * Process an added label
     * @param timelineItems List of timeline items
     * @param issue Issue the timeline is on
     * @param imsProject IMS project to sync
     * @param rawTimelineItems Timeline before transformation
     */
    private suspend fun integrateLabelToState(
        timelineItems: MutableList<TimelineItem>,
        rawTimelineItems: List<TimelineItem>,
        issue: Issue,
        imsProject: IMSProject
    ) {
        rawTimelineItems.toList().forEach { timelineItem ->
            val (activeLabels, lastState) = integrateLabelToStateFindCurrentState(
                issue, timelineItem
            )
            val labelStateMap = this.labelStateMap(imsProject)
            val mappedStates = activeLabels.mapNotNull { lookupState(labelStateMap[it.name]) }
            if (timelineItem is StateChangedEvent) {
                if (mappedStates.isNotEmpty()) {
                    timelineItems.remove(timelineItem)
                }
            }
            if (timelineItem is AddedLabelEvent) {
                integrateLabelToStateAddedLabel(
                    timelineItems, issue, imsProject, timelineItem, lastState, mappedStates
                )
            }
            if (timelineItem is RemovedLabelEvent) {
                integrateLabelToStateRemovedLabel(
                    timelineItems, issue, imsProject, timelineItem, lastState, mappedStates
                )
            }
        }
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
        logger.trace("Syncing incoming for issue ${issue.rawId} $timelineItem ${timelineItem.identification()}")
        val oldInfo = collectedSyncInfo.timelineItemConversionInformationService.findByImsProjectAndGithubId(
            imsProject.rawId!!, timelineItem.identification()
        ).firstOrNull()
        var (rawTimelineItems, newInfo) = timelineItem.gropiusTimelineItem(
            imsProject, syncDataService(), oldInfo, issue
        )
        if (issue.rawId != null) {
            val dereplicationResult =
                issueDereplicator.validateTimelineItem(issue, rawTimelineItems, dereplicatorRequest)
            rawTimelineItems = dereplicationResult.resultingTimelineItems
        }
        val timelineItems = rawTimelineItems.toMutableList()
        integrateLabelToState(timelineItems, rawTimelineItems, issue, imsProject)
        if (timelineItems.isNotEmpty()) {//TODO: Handle multiple
            timelineItems.forEach { it.issue().value = issue }
            issue.timelineItems() += timelineItems
            issue.issueComments() += timelineItems.mapNotNull { it as? IssueComment }
            nodesToSave.add(timelineItems.single())
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
     * @param virtualIDs mapping for timeline items that are geerated with generated ids and do not exist in the database
     * @return true if and only if there are unsynced changes that should be synced to GitHub
     */
    private suspend inline fun <reified AddingItem : TimelineItem, reified RemovingItem : TimelineItem> shouldSyncType(
        imsProject: IMSProject,
        finalBlock: List<TimelineItem>,
        relevantTimeline: List<TimelineItem>,
        restoresDefaultState: Boolean,
        virtualIDs: Map<TimelineItem, String>
    ): Boolean {
        return shouldSyncType(
            imsProject,
            { it is AddingItem },
            { it is RemovingItem },
            finalBlock,
            relevantTimeline,
            restoresDefaultState,
            virtualIDs
        )
    }

    /**
     * Check if TimelineItem should be synced or ignored
     * @param imsProject IMS project to sync
     * @param isAddingItem filter for items with the same semantic as the item to add
     * @param isRemovingItem filter for items invalidating the items matching [isAddingItem]
     * @param finalBlock the last block of similar items that should be checked for syncing
     * @param relevantTimeline Sorted part of the timeline containing only TimelineItems interacting with finalBlock
     * @param restoresDefaultState if the timeline item converges the state of the issue towards the state of an empty issue
     * @param virtualIDs mapping for timeline items that are geerated with generated ids and do not exist in the database
     * @return true if and only if there are unsynced changes that should be synced to GitHub
     */
    private suspend fun shouldSyncType(
        imsProject: IMSProject,
        isAddingItem: suspend (TimelineItem) -> Boolean,
        isRemovingItem: suspend (TimelineItem) -> Boolean,
        finalBlock: List<TimelineItem>,
        relevantTimeline: List<TimelineItem>,
        restoresDefaultState: Boolean,
        virtualIDs: Map<TimelineItem, String> = mapOf()
    ): Boolean {
        if (isAddingItem(finalBlock.last())) {
            val lastNegativeEvent = relevantTimeline.filter { isRemovingItem(it) }.lastOrNull {
                collectedSyncInfo.timelineItemConversionInformationService.findByImsProjectAndGropiusId(
                    imsProject.rawId!!, it.rawId ?: virtualIDs[it]!!
                )?.githubId != null
            }
            logger.debug("LastNegativeEvent $lastNegativeEvent")
            if (lastNegativeEvent == null) {
                return !restoresDefaultState
            } else {
                if (relevantTimeline.filter { isAddingItem(it) }.filter { it.createdAt > lastNegativeEvent.createdAt }
                        .firstOrNull {
                            collectedSyncInfo.timelineItemConversionInformationService.findByImsProjectAndGropiusId(
                                imsProject.rawId!!, it.rawId ?: virtualIDs[it]!!
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
                if (isOutgoingTemplatedFieldsEnabled(imsProject)) {
                    syncOutgoingTemplatedFields(timeline, imsProject, issueInfo)
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
        val labelStateMap = this.labelStateMap(imsProject)
        val stateLabelMap = labelStateMap.map { it.value to it.key }.toMap()
        val virtualIDs = mutableMapOf<TimelineItem, String>()

        val modifiedTimeline =
            timeline.filterIsInstance<AddedLabelEvent>() + timeline.filterIsInstance<RemovedLabelEvent>() + timeline.filterIsInstance<StateChangedEvent>()
                .flatMap {
                    val ret = mutableListOf<TimelineItem>()
                    val labels = labelStateMap.mapNotNull {
                        val labelName = it.key
                        imsProject.trackable().value.labels().firstOrNull { it.name == labelName }
                    }.toMutableSet()
                    if (stateLabelMap.containsKey(it.newState().value.rawId!!)) {
                        val name = stateLabelMap[it.newState().value.rawId!!]
                        val label = imsProject.trackable().value.labels().firstOrNull { it.name == name }
                        if (label != null) {
                            labels -= label
                            val elem = AddedLabelEvent(it.createdAt, it.lastModifiedAt)
                            elem.createdBy().value = it.createdBy().value
                            elem.issue().value = it.issue().value
                            elem.lastModifiedBy().value = it.lastModifiedBy().value
                            elem.addedLabel().value = label
                            ret.add(elem)
                            virtualIDs[elem] = it.rawId!! + "-added"
                        }
                    }
                    for (label in labels) {
                        val elem = RemovedLabelEvent(it.createdAt, it.lastModifiedAt)
                        elem.createdBy().value = it.createdBy().value
                        elem.issue().value = it.issue().value
                        elem.lastModifiedBy().value = it.lastModifiedBy().value
                        elem.removedLabel().value = label
                        ret.add(elem)
                        virtualIDs[elem] = it.rawId!! + "-" + label.rawId!! + "-removed"
                    }
                    ret
                }
        val groups = modifiedTimeline.groupBy {
            when (it) {
                is AddedLabelEvent -> it.addedLabel().value!!
                is RemovedLabelEvent -> it.removedLabel().value!!
                else -> throw IllegalStateException("Virtual Label Generator Defective")
            }
        }
        for ((label, relevantTimeline) in groups) {
            syncOutgoingSingleLabel(
                relevantTimeline.sortedBy { it.createdAt }, imsProject, issueInfo, label, virtualIDs
            )
        }
    }

    /**
     * Sync Outgoing Single Label
     * @param relevantTimeline Timeline of the issue filtered for the label
     * @param imsProject IMS project to sync
     * @param issueInfo Issue to sync
     * @param label Label to sync
     * @param virtualIDs mapping for timeline items that are geerated with generated ids and do not exist in the database
     */
    private suspend fun syncOutgoingSingleLabel(
        relevantTimeline: List<TimelineItem>,
        imsProject: IMSProject,
        issueInfo: IssueConversionInformation,
        label: Label?,
        virtualIDs: Map<TimelineItem, String>
    ) {
        var labelIsSynced = false
        val finalBlock = findFinalTypeBlock(relevantTimeline)
        for (item in finalBlock) {
            val relevantEvent = collectedSyncInfo.timelineItemConversionInformationService.findByImsProjectAndGropiusId(
                imsProject.rawId!!, item.rawId ?: virtualIDs[item]!!
            )
            if (relevantEvent?.githubId != null) {
                labelIsSynced = true
            }
        }
        if (!labelIsSynced) {
            if (shouldSyncType<RemovedLabelEvent, AddedLabelEvent>(
                    imsProject, finalBlock, relevantTimeline, true, virtualIDs
                )
            ) {
                val conversionInformation = syncRemovedLabel(
                    imsProject,
                    issueInfo.githubId,
                    label!!,
                    finalBlock.map { it.lastModifiedBy().value })
                if (conversionInformation != null) {
                    conversionInformation.gropiusId = finalBlock.map { it.rawId ?: virtualIDs[it]!! }.first()
                    collectedSyncInfo.timelineItemConversionInformationService.save(
                        conversionInformation
                    ).awaitSingle()
                }
            }
            if (shouldSyncType<AddedLabelEvent, RemovedLabelEvent>(
                    imsProject, finalBlock, relevantTimeline, false, virtualIDs
                )
            ) {
                val conversionInformation = syncAddedLabel(
                    imsProject,
                    issueInfo.githubId,
                    label!!,
                    finalBlock.map { it.lastModifiedBy().value })
                if (conversionInformation != null) {
                    conversionInformation.gropiusId = finalBlock.map { it.rawId ?: virtualIDs[it]!! }.first()
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
        val virtualIDs = mapOf<TimelineItem, String>()//For future features
        val relevantTimeline = timeline.mapNotNull { it as? TitleChangedEvent }
        if (relevantTimeline.isEmpty()) {
            return
        }
        val finalBlock = findFinalBlock(relevantTimeline) { it.newTitle }
        if (finalBlock.none {
                collectedSyncInfo.timelineItemConversionInformationService.findByImsProjectAndGropiusId(
                    imsProject.rawId!!, it.rawId!!
                ) != null
            }) {
            val conversionInformation = syncTitleChange(
                imsProject,
                issueInfo.githubId,
                finalBlock.first().newTitle,
                finalBlock.map { it.createdBy().value })
            if (conversionInformation != null) {
                conversionInformation.gropiusId = finalBlock.map { it.rawId ?: virtualIDs[it]!! }.first()
                collectedSyncInfo.timelineItemConversionInformationService.save(
                    conversionInformation
                ).awaitSingle()
            }
        }
    }

    /**
     * Sync Outgoing TemplatedFields Changes
     * @param timeline Timeline of the issue
     * @param imsProject IMS project to sync
     * @param issueInfo Issue to sync
     */
    private suspend fun syncOutgoingTemplatedFields(
        timeline: List<TimelineItem>, imsProject: IMSProject, issueInfo: IssueConversionInformation
    ) {
        val virtualIDs = mapOf<TimelineItem, String>()//For future features
        val relevantTimeline = timeline.mapNotNull { it as? TemplatedFieldChangedEvent }
        if (relevantTimeline.isEmpty()) {
            return
        }
        val finalBlock = findFinalBlock(relevantTimeline) { it.fieldName to it.newValue }
        if (finalBlock.none {
                collectedSyncInfo.timelineItemConversionInformationService.findByImsProjectAndGropiusId(
                    imsProject.rawId!!, it.rawId!!
                ) != null
            }) {
            val conversionInformation = syncTemplatedField(
                imsProject,
                issueInfo.githubId,
                finalBlock.first(),
                finalBlock.map { it.createdBy().value })
            if (conversionInformation != null) {
                conversionInformation.gropiusId = finalBlock.map { it.rawId ?: virtualIDs[it]!! }.first()
                collectedSyncInfo.timelineItemConversionInformationService.save(
                    conversionInformation
                ).awaitSingle()
            }
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
        val virtualIDs = mapOf<TimelineItem, String>()//For future features
        val relevantTimeline = timeline.mapNotNull { it as? StateChangedEvent }
        if (relevantTimeline.isEmpty()) {
            return
        }
        val finalBlock = findFinalBlock(relevantTimeline) { it.newState().value }
        logger.debug("finalBlock: $finalBlock in $relevantTimeline being ${relevantTimeline.map { it.newState().value.name }}")
        if (finalBlock.none {
                collectedSyncInfo.timelineItemConversionInformationService.findByImsProjectAndGropiusId(
                    imsProject.rawId!!, it.rawId!!
                ) != null
            }) {
            logger.debug("syncOutgoingStateChanges: $finalBlock")
            val conversionInformation = syncStateChange(imsProject,
                issueInfo.githubId,
                finalBlock.first().newState().value,
                finalBlock.map { it.lastModifiedBy().value })
            if (conversionInformation != null) {
                conversionInformation.gropiusId = finalBlock.map { it.rawId ?: virtualIDs[it]!! }.first()
                collectedSyncInfo.timelineItemConversionInformationService.save(
                    conversionInformation
                ).awaitSingle()
            }
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
        val virtualIDs = mutableMapOf<TimelineItem, String>()

        val modifiedTimeline =
            timeline.filterIsInstance<Assignment>() + timeline.filterIsInstance<RemovedAssignmentEvent>()
        val groups = modifiedTimeline.groupBy {
            when (it) {
                is Assignment -> it
                is RemovedAssignmentEvent -> it.removedAssignment().value!!
                else -> throw IllegalStateException("Kotlin Generator Defective")
            }
        }
        for ((assignment, relevantTimeline) in groups) {
            syncOutgoingSingleAssignment(
                relevantTimeline.sortedBy { it.createdAt }, imsProject, issueInfo, assignment, virtualIDs
            )
        }
    }

    /**
     * Sync Outgoing Assignments
     * @param relevantTimeline Timeline of the issue
     * @param imsProject IMS project to sync
     * @param issueInfo Issue to sync
     * @param assignment Assignment to sync
     * @param virtualIDs mapping for timeline items that are geerated with generated ids and do not exist in the database
     * @param finalBlock Final block relevant for this assignment
     */
    private suspend fun syncOutgoingSingleAssignmentBlock(
        relevantTimeline: List<TimelineItem>,
        imsProject: IMSProject,
        issueInfo: IssueConversionInformation,
        assignment: Assignment,
        virtualIDs: Map<TimelineItem, String>,
        finalBlock: List<TimelineItem>
    ) {
        if (shouldSyncType<RemovedAssignmentEvent, Assignment>(
                imsProject, finalBlock, relevantTimeline, true, virtualIDs
            )
        ) {
            val conversionInformation = syncSingleUnassigned(imsProject,
                issueInfo.githubId,
                assignment,
                finalBlock.map { it.lastModifiedBy().value })
            if (conversionInformation != null) {
                conversionInformation.gropiusId = finalBlock.map { it.rawId ?: virtualIDs[it]!! }.first()
                collectedSyncInfo.timelineItemConversionInformationService.save(
                    conversionInformation
                ).awaitSingle()
            }
        }
        if (shouldSyncType<Assignment, RemovedAssignmentEvent>(
                imsProject, finalBlock, relevantTimeline, false, virtualIDs
            )
        ) {
            val conversionInformation = syncSingleAssigned(imsProject,
                issueInfo.githubId,
                assignment,
                finalBlock.map { it.lastModifiedBy().value })
            if (conversionInformation != null) {
                conversionInformation.gropiusId = finalBlock.map { it.rawId ?: virtualIDs[it]!! }.first()
                collectedSyncInfo.timelineItemConversionInformationService.save(
                    conversionInformation
                ).awaitSingle()
            }
        }
    }

    /**
     * Sync Outgoing Assignments
     * @param relevantTimeline Timeline of the issue
     * @param imsProject IMS project to sync
     * @param issueInfo Issue to sync
     * @param assignment Assignment to sync
     * @param virtualIDs mapping for timeline items that are geerated with generated ids and do not exist in the database
     */
    private suspend fun syncOutgoingSingleAssignment(
        relevantTimeline: List<TimelineItem>,
        imsProject: IMSProject,
        issueInfo: IssueConversionInformation,
        assignment: Assignment,
        virtualIDs: Map<TimelineItem, String>
    ) {
        var assignmentIsSynced = false
        val finalBlock = findFinalTypeBlock(relevantTimeline)
        for (item in finalBlock) {
            val relevantEvent = collectedSyncInfo.timelineItemConversionInformationService.findByImsProjectAndGropiusId(
                imsProject.rawId!!, item.rawId ?: virtualIDs[item]!!
            )
            if (relevantEvent?.githubId != null) {
                assignmentIsSynced = true
            }
        }
        if (!assignmentIsSynced) {
            syncOutgoingSingleAssignmentBlock(
                relevantTimeline, imsProject, issueInfo, assignment, virtualIDs, finalBlock
            )
        }
    }

    /**
     * Sync all data
     */
    suspend fun sync() {
        logger.info("Starting Sync Cycle")
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
        logger.info("Finished Sync Cycle")
    }

    /**
     * Map list of User to GropiusUser
     * @param users The list of users mixed of IMSUser and GropiusUser
     * @return The list of GropiusUser
     */
    suspend fun gropiusUserList(users: List<User>): List<GropiusUser> {
        val outputUsers = users.mapNotNull {
            when (it) {
                is GropiusUser -> it
                is IMSUser -> it.gropiusUser().value
                else -> null
            }
        }
        if (outputUsers.isEmpty() && users.isNotEmpty()) {
            throw IllegalStateException("No Gropius User left as owner")
        }
        return outputUsers
    }
}
