package gropius.sync

import gropius.model.architecture.IMS
import gropius.model.architecture.IMSProject
import gropius.model.architecture.Project
import gropius.model.issue.timeline.IssueComment
import gropius.model.template.IMSIssueTemplate
import gropius.model.template.IMSProjectTemplate
import gropius.model.template.IMSTemplate
import gropius.model.template.IMSUserTemplate
import gropius.repository.issue.IssueRepository
import kotlinx.coroutines.reactive.awaitFirstOrNull
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

    suspend fun imsTemplate(): IMSTemplate {
        val newImsTemplate = IMSTemplate(
            "noims", "", mutableMapOf(), false
        )
        newImsTemplate.imsProjectTemplate().value = IMSProjectTemplate("noimsproject", "", mutableMapOf())
        newImsTemplate.imsIssueTemplate().value = IMSIssueTemplate("noimsuser", "", mutableMapOf())
        newImsTemplate.imsUserTemplate().value = IMSUserTemplate("noimsuser", "", mutableMapOf())
        return collectedSyncInfo.neoOperations.findAll(IMSTemplate::class.java).awaitFirstOrNull()
            ?: collectedSyncInfo.neoOperations.save(
                newImsTemplate
            ).awaitSingle()
    }

    suspend fun imsProjectTemplate(): IMSProjectTemplate {
        return imsTemplate().imsProjectTemplate().value
    }

    suspend fun ims(): IMS {
        val newIms = IMS(
            "noims", "", mutableMapOf()
        )
        newIms.template().value = imsTemplate()
        return collectedSyncInfo.neoOperations.findAll(IMS::class.java).awaitFirstOrNull()
            ?: collectedSyncInfo.neoOperations.save(
                newIms
            ).awaitSingle()
    }

    suspend fun project(): Project {
        return collectedSyncInfo.neoOperations.findAll(Project::class.java).awaitFirstOrNull()
            ?: collectedSyncInfo.neoOperations.save(
                Project(
                    "noproject", "", null
                )
            ).awaitSingle()
    }

    suspend fun imsProject(): IMSProject {
        val newImsProject = IMSProject(mutableMapOf())
        newImsProject.ims().value = ims()
        newImsProject.trackable().value = project()
        newImsProject.template().value = imsProjectTemplate()
        return collectedSyncInfo.neoOperations.findAll(IMSProject::class.java).awaitFirstOrNull()
            ?: collectedSyncInfo.neoOperations.save(newImsProject).awaitSingle()
    }

    abstract suspend fun findUnsyncedIssues(imsProject: IMSProject): List<IncomingIssue>;
    abstract fun syncDataService(): SyncDataService

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

    suspend fun sync() {
        fetchData(listOf(imsProject()))
        doIncoming(imsProject())
    }
}
