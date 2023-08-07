package gropius.sync

import gropius.model.architecture.IMS
import gropius.model.architecture.IMSProject
import gropius.model.issue.timeline.IssueComment
import gropius.model.template.IMSTemplate
import gropius.repository.issue.IssueRepository
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.neo4j.core.ReactiveNeo4jOperations
import org.springframework.stereotype.Component

interface DataFetcher {
    suspend fun fetchData(imsProjects: List<IMSProject>);
}

@Component
class CollectedSyncInfo(
    @Qualifier("graphglueNeo4jOperations")
    val neoOperations: ReactiveNeo4jOperations,
    val issueConversionInformationService: IssueConversionInformationService,
    val issueRepository: IssueRepository,
    val timelineItemConversionInformationService: TimelineItemConversionInformationService,
    val syncNotificator: SyncNotificator
) {}

abstract class AbstractSync(
    val collectedSyncInfo: CollectedSyncInfo
) : DataFetcher {
    private val logger = LoggerFactory.getLogger(AbstractSync::class.java)

    abstract suspend fun findUnsyncedIssues(imsProject: IMSProject): List<IncomingIssue>;
    abstract fun syncDataService(): SyncDataService

    abstract suspend fun findTemplates(): Set<IMSTemplate>

    suspend fun doIncoming(imsProject: IMSProject) {
        try {
            findUnsyncedIssues(imsProject).forEach {
                val issueInfo = collectedSyncInfo.issueConversionInformationService.findByImsProjectAndGithubId(
                    imsProject.rawId!!, it.identification()
                ) ?: IssueConversionInformation(imsProject.rawId!!, it.identification(), null)
                var issue =
                    if (issueInfo.gropiusId != null) collectedSyncInfo.issueRepository.findById(issueInfo.gropiusId!!)
                        .awaitSingle() else it.createIssue(imsProject, syncDataService())
                if (issue.rawId == null) issue = collectedSyncInfo.neoOperations.save(issue).awaitSingle()
                if (issueInfo.gropiusId == null) {
                    issueInfo.gropiusId = issue.rawId!!
                    collectedSyncInfo.issueConversionInformationService.save(issueInfo).awaitSingle()
                }
                //try {
                val timelineItems = it.incomingTimelineItems(syncDataService())
                for (timelineItem in timelineItems) {
                    val oldInfo =
                        collectedSyncInfo.timelineItemConversionInformationService.findByImsProjectAndGithubId(
                            imsProject.rawId!!, timelineItem.identification()
                        )
                    val (timelineItem, newInfo) = timelineItem.gropiusTimelineItem(
                        imsProject, syncDataService(), oldInfo
                    )
                    if (timelineItem.isNotEmpty()) {//TODO: Handle multiple
                        timelineItem.forEach { it.issue().value = issue }
                        newInfo.gropiusId =
                            collectedSyncInfo.neoOperations.save(timelineItem.single()).awaitSingle()!!.rawId
                        issue.issueComments() += timelineItem.mapNotNull { it as? IssueComment }
                        issue = collectedSyncInfo.neoOperations.save(issue).awaitSingle()
                    }
                    if (oldInfo?.id != null) {
                        newInfo.id = oldInfo.id;
                    }
                    collectedSyncInfo.timelineItemConversionInformationService.save(newInfo).awaitSingle()
                }
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

    open suspend fun doOutgoing(imsProject: IMSProject) {
        imsProject.trackable().value.issues().forEach { issue ->
            val timeline = issue.timelineItems().toList().sortedBy { it.createdAt }
            timeline.mapNotNull { it as? IssueComment }.filter {
                collectedSyncInfo.timelineItemConversionInformationService.findByImsProjectAndGropiusId(
                    imsProject.rawId!!, it.rawId!!
                ) == null
            }.forEach {
                val issue = collectedSyncInfo.issueConversionInformationService.findByImsProjectAndGropiusId(
                    imsProject.rawId!!, it.issue().value.rawId!!
                )!!
                val conversionInformation = syncComment(imsProject, issue.githubId, it)
                if (conversionInformation != null) {
                    conversionInformation.gropiusId = it.rawId!!
                    collectedSyncInfo.timelineItemConversionInformationService.save(conversionInformation).awaitSingle()
                }
            }
        }
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
