package gropius.sync

import gropius.model.architecture.IMS
import gropius.model.architecture.IMSProject
import gropius.model.issue.Issue
import gropius.model.issue.Label
import gropius.model.issue.timeline.*
import gropius.model.template.IMSTemplate
import gropius.model.template.IssueState
import gropius.model.user.GropiusUser
import gropius.model.user.User
import gropius.repository.issue.IssueRepository
import io.github.graphglue.model.Node
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.neo4j.core.ReactiveNeo4jOperations
import org.springframework.data.neo4j.core.findAll
import org.springframework.stereotype.Component

interface DataFetcher {
    suspend fun fetchData(imsProjects: List<IMSProject>);
}

class DummyTimelineItemConversionInformation(
    imsProject: String, githubId: String
) : TimelineItemConversionInformation(imsProject, githubId, null) {}

@Component
class CollectedSyncInfo(
    @Qualifier("graphglueNeo4jOperations")
    val neoOperations: ReactiveNeo4jOperations,
    val issueConversionInformationService: IssueConversionInformationService,
    val issueRepository: IssueRepository,
    val timelineItemConversionInformationService: TimelineItemConversionInformationService,
    val syncNotificator: SyncNotificator,
    val issueCleaner: IssueCleaner
) {}

class SimpleIssueDereplicatorRequest(override val dummyUser: User) : IssueDereplicatorRequest {}

abstract class AbstractSync(
    val collectedSyncInfo: CollectedSyncInfo
) : DataFetcher {
    private val logger = LoggerFactory.getLogger(AbstractSync::class.java)

    abstract suspend fun findUnsyncedIssues(imsProject: IMSProject): List<IncomingIssue>;
    abstract fun syncDataService(): SyncDataService

    abstract suspend fun findTemplates(): Set<IMSTemplate>

    val issueDereplicator: IssueDereplicator = InvasiveDereplicator()

    suspend fun doIncoming(imsProject: IMSProject) {
        val dereplicatorRequest = SimpleIssueDereplicatorRequest(
            collectedSyncInfo.neoOperations.findAll<GropiusUser>().filter { it.username == "gropius" }.firstOrNull()
                ?: collectedSyncInfo.neoOperations.save(
                    GropiusUser(
                        "Gropius", null, null, "gropius", false
                    )
                ).awaitSingle()
        )
        try {
            findUnsyncedIssues(imsProject).forEach {
                val issueInfo = collectedSyncInfo.issueConversionInformationService.findByImsProjectAndGithubId(
                    imsProject.rawId!!, it.identification()
                ) ?: IssueConversionInformation(imsProject.rawId!!, it.identification(), null)
                var issue =
                    if (issueInfo.gropiusId != null) collectedSyncInfo.issueRepository.findById(issueInfo.gropiusId!!)
                        .awaitSingle() else it.createIssue(imsProject, syncDataService())
                val nodesToSave = mutableListOf<Node>(issue)
                val savedNodeHandlers = mutableListOf<suspend (node: Node) -> Unit>()
                //try {
                val timelineItems = it.incomingTimelineItems(syncDataService())
                for (timelineItem in timelineItems) {
                    val oldInfo =
                        collectedSyncInfo.timelineItemConversionInformationService.findByImsProjectAndGithubId(
                            imsProject.rawId!!, timelineItem.identification()
                        )
                    var (timelineItem, newInfo) = timelineItem.gropiusTimelineItem(
                        imsProject, syncDataService(), oldInfo
                    )
                    if (issue.rawId != null) {
                        val dereplicationResult =
                            issueDereplicator.validateTimelineItem(issue, timelineItem, dereplicatorRequest)
                        timelineItem = dereplicationResult.resultingTimelineItems
                    }
                    if (timelineItem.isNotEmpty()) {//TODO: Handle multiple
                        timelineItem.forEach { it.issue().value = issue }
                        issue.timelineItems() += timelineItem
                        issue.issueComments() += timelineItem.mapNotNull { it as? IssueComment }
                        nodesToSave.add(timelineItem.single())
                        savedNodeHandlers.add {
                            newInfo.gropiusId = (it as TimelineItem).rawId
                            if (oldInfo?.id != null) {
                                newInfo.id = oldInfo.id;
                            }
                            collectedSyncInfo.timelineItemConversionInformationService.save(newInfo).awaitSingle()
                        }
                    }
                }
                var dereplicationResult: IssueDereplicatorIssueResult? = null
                if (issue.rawId == null) {
                    dereplicationResult = issueDereplicator.validateIssue(imsProject, issue, dereplicatorRequest)
                    issue = dereplicationResult.resultingIssue
                    for (fakeSyncedItem in dereplicationResult.fakeSyncedItems) {
                        nodesToSave.add(fakeSyncedItem)
                        savedNodeHandlers.add {
                            val tici =
                                DummyTimelineItemConversionInformation(imsProject.rawId!!, (it as TimelineItem).rawId!!)
                            tici.gropiusId = (it as TimelineItem).rawId
                            collectedSyncInfo.timelineItemConversionInformationService.save(tici).awaitSingle()
                        }
                    }
                }
                val savedList = collectedSyncInfo.neoOperations.saveAll(nodesToSave).collectList().awaitSingle()
                val savedIssue = savedList.removeFirst()
                if (issue.rawId == null) issue = savedIssue as Issue
                savedList.zip(savedNodeHandlers).forEach { (savedNode, savedNodeHandler) ->
                    savedNodeHandler(savedNode)
                }
                if (issueInfo.gropiusId == null) {
                    issueInfo.gropiusId = issue.rawId!!
                }
                collectedSyncInfo.issueConversionInformationService.save(issueInfo).awaitSingle()
                collectedSyncInfo.issueCleaner.cleanIssue(issue.rawId!!)
                it.markDone(syncDataService())
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

    abstract suspend fun syncComment(
        imsProject: IMSProject, issueId: String, issueComment: IssueComment
    ): TimelineItemConversionInformation?

    abstract suspend fun syncTitleChange(
        imsProject: IMSProject, issueId: String, newTitle: String
    ): TimelineItemConversionInformation?

    abstract suspend fun syncStateChange(
        imsProject: IMSProject, issueId: String, newState: IssueState
    ): TimelineItemConversionInformation?

    abstract suspend fun syncAddedLabel(
        imsProject: IMSProject, issueId: String, label: Label
    ): TimelineItemConversionInformation?

    abstract suspend fun syncRemovedLabel(
        imsProject: IMSProject, issueId: String, label: Label
    ): TimelineItemConversionInformation?

    abstract suspend fun createOutgoingIssue(imsProject: IMSProject, issue: Issue): IssueConversionInformation?;

    abstract suspend fun isOutgoingEnabled(imsProject: IMSProject): Boolean
    abstract suspend fun isOutgoingLabelsEnabled(imsProject: IMSProject): Boolean

    abstract suspend fun isOutgoingCommentsEnabled(imsProject: IMSProject): Boolean

    abstract suspend fun isOutgoingTitleChangedEnabled(imsProject: IMSProject): Boolean

    abstract suspend fun isOutgoingAssignmentsEnabled(imsProject: IMSProject): Boolean

    abstract suspend fun isOutgoingStatesEnabled(imsProject: IMSProject): Boolean

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
            println("LNE $lastNegativeEvent")
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
            var labelIsSynced = false
            val finalBlock = findFinalTypeBlock(relevantTimeline)
            for (item in finalBlock) {
                val relevantEvent =
                    collectedSyncInfo.timelineItemConversionInformationService.findByImsProjectAndGropiusId(
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
                    val conversionInformation = syncRemovedLabel(
                        imsProject, issueInfo.githubId, label!!/*,finalBlock.map { it.lastModifiedBy().value }*/
                    )
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
                    val conversionInformation = syncAddedLabel(
                        imsProject, issueInfo.githubId, label!!/*,finalBlock.map { it.lastModifiedBy().value }*/
                    )
                    if (conversionInformation != null) {
                        conversionInformation.gropiusId = finalBlock.map { it.rawId!! }.first()
                        collectedSyncInfo.timelineItemConversionInformationService.save(
                            conversionInformation
                        ).awaitSingle()
                    }
                }
            }
        }
    }

    private suspend fun syncOutgoingComments(
        timeline: List<TimelineItem>, imsProject: IMSProject, issueInfo: IssueConversionInformation
    ) {
        timeline.mapNotNull { it as? IssueComment }.filter {
            collectedSyncInfo.timelineItemConversionInformationService.findByImsProjectAndGropiusId(
                imsProject.rawId!!, it.rawId!!
            ) == null
        }.forEach {
            val conversionInformation = syncComment(imsProject, issueInfo.githubId, it)
            if (conversionInformation != null) {
                conversionInformation.gropiusId = it.rawId!!
                collectedSyncInfo.timelineItemConversionInformationService.save(conversionInformation).awaitSingle()
            }
        }
    }

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
            syncTitleChange(imsProject, issueInfo.githubId, finalBlock.first().newTitle)
        }
    }

    private suspend fun syncOutgoingStateChanges(
        timeline: List<TimelineItem>, imsProject: IMSProject, issueInfo: IssueConversionInformation
    ) {
        val relevantTimeline = timeline.mapNotNull { it as? StateChangedEvent }
        if (relevantTimeline.isEmpty()) return
        val finalBlock = findFinalBlock(relevantTimeline) { it.newState().value }
        println("finalBlock: $finalBlock")
        if (finalBlock.none {
                collectedSyncInfo.timelineItemConversionInformationService.findByImsProjectAndGropiusId(
                    imsProject.rawId!!, it.rawId!!
                ) != null
            }) {
            println("syncOutgoingStateChanges: $finalBlock")
            syncStateChange(imsProject, issueInfo.githubId, finalBlock.first().newState().value)
        }
    }

    private suspend fun syncOutgoingAssignments(
        timeline: List<TimelineItem>, imsProject: IMSProject, issueInfo: IssueConversionInformation
    ) {
    }

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
