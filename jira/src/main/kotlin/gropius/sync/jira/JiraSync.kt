package gropius.sync.jira

import gropius.model.architecture.IMSProject
import gropius.model.issue.Issue
import gropius.model.issue.Label
import gropius.model.issue.timeline.IssueComment
import gropius.model.template.IMSTemplate
import gropius.model.template.IssueState
import gropius.model.user.User
import gropius.sync.*
import gropius.sync.jira.config.IMSConfig
import gropius.sync.jira.config.IMSConfigManager
import gropius.sync.jira.config.IMSProjectConfig
import gropius.sync.jira.model.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Jira main sync class
 * @param jiraDataService the data service for Jira
 * @param helper the json helper
 * @param cursorResourceWalkerDataService the data service for the cursor resource walker
 * @param imsConfigManager the config manager for the IMS
 * @param collectedSyncInfo the collected sync info
 * @param loadBalancedDataFetcher the load balanced data fetcher
 * @param issueDataService the data service for issues
 * @param syncStatusService the sync status service
 */
@Component
final class JiraSync(
    val jiraDataService: JiraDataService,
    val helper: JsonHelper,
    val cursorResourceWalkerDataService: CursorResourceWalkerDataService,
    val imsConfigManager: IMSConfigManager,
    collectedSyncInfo: CollectedSyncInfo,
    val loadBalancedDataFetcher: LoadBalancedDataFetcher = LoadBalancedDataFetcher(),
    val issueDataService: IssueDataService,
    val syncStatusService: SyncStatusService
) : AbstractSync(collectedSyncInfo) {

    companion object {
        /**
         * Formatter for JQL dates
         */
        val jqlFormatter = DateTimeFormatter.ofPattern("\"yyyy-MM-dd HH:mm\"")
    }

    /**
     * Logger used to print notifications
     */
    private val logger = LoggerFactory.getLogger(JiraSync::class.java)

    override fun syncDataService(): SyncDataService {
        return jiraDataService
    }

    override suspend fun findTemplates(): Set<IMSTemplate> {
        return imsConfigManager.findTemplates()
    }

    override suspend fun isOutgoingEnabled(imsProject: IMSProject): Boolean {
        val imsProjectConfig = IMSProjectConfig(helper, imsProject)
        return imsProjectConfig.enableOutgoing
    }

    override suspend fun isOutgoingLabelsEnabled(imsProject: IMSProject): Boolean {
        val imsProjectConfig = IMSProjectConfig(helper, imsProject)
        return imsProjectConfig.enableOutgoingLabels
    }

    override suspend fun isOutgoingCommentsEnabled(imsProject: IMSProject): Boolean {
        val imsProjectConfig = IMSProjectConfig(helper, imsProject)
        return imsProjectConfig.enableOutgoingComments
    }

    override suspend fun isOutgoingTitleChangedEnabled(imsProject: IMSProject): Boolean {
        val imsProjectConfig = IMSProjectConfig(helper, imsProject)
        return imsProjectConfig.enableOutgoingTitleChanges
    }

    override suspend fun isOutgoingAssignmentsEnabled(imsProject: IMSProject): Boolean {
        val imsProjectConfig = IMSProjectConfig(helper, imsProject)
        return imsProjectConfig.enableOutgoingAssignments
    }

    override suspend fun isOutgoingStatesEnabled(imsProject: IMSProject): Boolean {
        val imsProjectConfig = IMSProjectConfig(helper, imsProject)
        return imsProjectConfig.enableOutgoingState
    }

    override suspend fun fetchData(imsProjects: List<IMSProject>) {
        for (imsProject in imsProjects) {
            jiraDataService.issueTemplate(imsProject)
            jiraDataService.issueType(imsProject)
            jiraDataService.issueState(imsProject, true)
            jiraDataService.issueState(imsProject, false)
        }

        for (imsProject in imsProjects) {
            val (issueList, lastSeenTime) = fetchIssueList(imsProject)
            fetchIssueContent(issueList, imsProject)
            if (lastSeenTime != null) {
                syncStatusService.updateTime(imsProject.rawId!!, lastSeenTime)
            }
        }
    }

    /**
     * Fetch the changelog of the issues
     * @param issueList the list of issues
     * @param imsProject the IMS project
     */
    @OptIn(ExperimentalEncodingApi::class)
    private suspend fun fetchIssueContentChangelog(
        issueList: List<String>, imsProject: IMSProject
    ) {
        for (issueId in issueList) {
            var startAt = 0
            while (true) {
                val issueCommentList = jiraDataService.request<Unit>(imsProject, listOf(), HttpMethod.Get) {
                    appendPathSegments("issue")
                    appendPathSegments(issueId)
                    appendPathSegments("changelog")
                    parameters.append("startAt", "$startAt")
                }.second.body<ValueChangeLogContainer>()
                issueCommentList.values.forEach {
                    issueDataService.insertChangelogEntry(imsProject, issueId, it)
                }
                startAt = issueCommentList.startAt + issueCommentList.values.size
                if (startAt >= issueCommentList.total) {
                    break
                }
            }
        }
    }

    /**
     * Fetch the comments of the issues
     * @param issueList the list of issues
     * @param imsProject the IMS project
     */
    @OptIn(ExperimentalEncodingApi::class)
    private suspend fun fetchIssueContentComments(
        issueList: List<String>, imsProject: IMSProject
    ) {
        for (issueId in issueList) {
            var startAt = 0
            while (true) {
                val issueCommentList = jiraDataService.request<Unit>(imsProject, listOf(), HttpMethod.Get) {
                    appendPathSegments("issue")
                    appendPathSegments(issueId)
                    appendPathSegments("comment")
                    parameters.append("expand", "names,schema,editmeta,changelog")
                    parameters.append("startAt", "$startAt")
                }.second.body<CommentQuery>()
                issueCommentList.comments.forEach {
                    issueDataService.insertComment(imsProject, issueId, it)
                }
                startAt = issueCommentList.startAt + issueCommentList.comments.size
                if (startAt >= issueCommentList.total) {
                    break
                }
            }
        }
    }

    /**
     * Fetch the content of the issues
     * @param issueList the list of issues
     * @param imsProject the IMS project
     */
    @OptIn(ExperimentalEncodingApi::class)
    private suspend fun fetchIssueContent(
        issueList: List<String>, imsProject: IMSProject
    ) {
        logger.info("ISSUE LIST $issueList")
        val imsConfig = IMSConfig(helper, imsProject.ims().value, imsProject.ims().value.template().value)
        fetchIssueContentComments(issueList, imsProject)
        if (imsConfig.isCloud != false) {
            fetchIssueContentChangelog(issueList, imsProject)
        }
    }

    /**
     * Fetch the list of changed issues
     * @param imsProject the IMS project
     * @return issueList the list of issues
     */
    @OptIn(ExperimentalEncodingApi::class)
    private suspend fun fetchIssueList(
        imsProject: IMSProject
    ): Pair<List<String>, OffsetDateTime?> {
        val issueList = mutableListOf<String>()
        var startAt = 0
        val lastSuccessfulSync: OffsetDateTime? =
            syncStatusService.findByImsProject(imsProject.rawId!!)?.lastSuccessfulSync
        val times = mutableListOf<OffsetDateTime>()
        while (true) {
            val imsProjectConfig = IMSProjectConfig(helper, imsProject)
            val userList = jiraDataService.collectRequestUsers(imsProject, listOf())
            val issueResponse = jiraDataService.tokenManager.executeUntilWorking(imsProject.ims().value, userList) {
                val userTimeZone = ZoneId.of(
                    jiraDataService.sendRequest<Unit>(
                        imsProject, HttpMethod.Get, null, {
                            appendPathSegments("myself")
                        }, it
                    ).get().body<UserQuery>().timeZone
                )
                var query = "project=${imsProjectConfig.repo}"
                if (lastSuccessfulSync != null) {
                    query = "project=${imsProjectConfig.repo} AND updated > ${
                        lastSuccessfulSync.atZoneSameInstant(userTimeZone).format(jqlFormatter)
                    }"
                }
                logger.info("With $lastSuccessfulSync, ${imsProjectConfig.repo} and $userTimeZone, the query is '$query'")
                jiraDataService.sendRequest<Unit>(
                    imsProject, HttpMethod.Get, null, {
                        appendPathSegments("search")
                        parameters.append("jql", query)
                        parameters.append("expand", "names,schema,editmeta,changelog")
                        parameters.append("startAt", "$startAt")
                    }, it
                )
            }.second.body<ProjectQuery>()
            issueResponse.issues(imsProject).forEach {
                issueList.add(it.jiraId)
                issueDataService.insertIssue(imsProject, it)
                for (comment in it.comments.values) {
                    times.add(
                        OffsetDateTime.parse(
                            comment.created, IssueData.formatter
                        )
                    )
                    times.add(
                        OffsetDateTime.parse(
                            comment.updated, IssueData.formatter
                        )
                    )
                }
                for (history in it.changelog.histories) {
                    times.add(
                        OffsetDateTime.parse(
                            history.created, IssueData.formatter
                        )
                    )
                }
                val updated = it.fields["updated"]
                if (updated != null) {
                    times.add(
                        OffsetDateTime.parse(
                            updated.jsonPrimitive.content, IssueData.formatter
                        )
                    )
                }
            }
            startAt = issueResponse.startAt + issueResponse.issues.size
            if (startAt >= issueResponse.total) break
        }
        return issueList to times.maxOrNull()
    }

    override suspend fun findUnsyncedIssues(imsProject: IMSProject): List<IncomingIssue> {
        return issueDataService.findByImsProject(imsProject.rawId!!)
    }

    override suspend fun syncComment(
        imsProject: IMSProject, issueId: String, issueComment: IssueComment, users: List<User>
    ): TimelineItemConversionInformation? {
        if (issueComment.body.isNullOrEmpty()) {
            return null
        }
        val response = jiraDataService.request(
            imsProject, users, HttpMethod.Post, JsonObject(mapOf("body" to JsonPrimitive(issueComment.body)))
        ) {
            appendPathSegments("issue")
            appendPathSegments(issueId)
            appendPathSegments("comment")
        }.second.body<JsonObject>()
        val iid = response["id"]!!.jsonPrimitive.content
        return JiraTimelineItemConversionInformation(imsProject.rawId!!, iid)
    }

    override suspend fun syncTitleChange(
        imsProject: IMSProject, issueId: String, newTitle: String, users: List<User>
    ): TimelineItemConversionInformation? {
        val response = jiraDataService.request(
            imsProject, users, HttpMethod.Put, JsonObject(
                mapOf(
                    "fields" to JsonObject(
                        mapOf(
                            "summary" to JsonPrimitive(newTitle)
                        )
                    )
                )
            )
        ) {
            appendPathSegments("issue")
            appendPathSegments(issueId)
            parameters.append("returnIssue", "true")
            parameters.append("expand", "names,schema,editmeta,changelog")

        }
        val changelogEntry = response.second.body<IssueBean>().changelog.histories.lastOrNull()
        return JiraTimelineItemConversionInformation(
            imsProject.rawId!!, if (changelogEntry?.items?.singleOrNull()?.field == "summary") changelogEntry.id else ""
        )
    }

    override suspend fun syncStateChange(
        imsProject: IMSProject, issueId: String, newState: IssueState, users: List<User>
    ): TimelineItemConversionInformation? {
        if (true) {
            //TODO: Jira State Transitions
            return null;
        }
        jiraDataService.request(
            imsProject, users, HttpMethod.Put, JsonObject(
                mapOf(
                    "fields" to JsonObject(
                        mapOf(
                            "resolution" to if (newState.isOpen) JsonNull else JsonPrimitive("Done"),
                            "status" to if (newState.isOpen) JsonPrimitive("To Do") else JsonPrimitive("Done")
                        )
                    )
                )
            )
        ) {
            appendPathSegments("issue")
            appendPathSegments(issueId)
        }
        return JiraTimelineItemConversionInformation(
            imsProject.rawId!!, TODO("Program State Changes")
        )
    }

    override suspend fun syncAddedLabel(
        imsProject: IMSProject, issueId: String, label: Label, users: List<User>
    ): TimelineItemConversionInformation? {
        val response = jiraDataService.request(
            imsProject, users, HttpMethod.Put, JsonObject(
                mapOf(
                    "update" to JsonObject(
                        mapOf(
                            "labels" to JsonArray(
                                listOf(
                                    JsonObject(
                                        mapOf(
                                            "add" to JsonPrimitive(
                                                jirafyLabelName(label.name)
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        ) {
            appendPathSegments("issue")
            appendPathSegments(issueId)
            parameters.append("returnIssue", "true")
            parameters.append("expand", "names,schema,editmeta,changelog")
        }
        val changelogEntry = response.second.body<IssueBean>().changelog.histories.lastOrNull()
        return JiraTimelineItemConversionInformation(
            imsProject.rawId!!, if (changelogEntry?.items?.singleOrNull()?.field == "labels") changelogEntry.id else ""
        )
    }

    /**
     * Scape a Gropius Label name for Jira
     * @param gropiusName The name of the Label onb gropius side
     * @return the cleaned label
     */
    private fun jirafyLabelName(gropiusName: String): String {
        return gropiusName.replace("[^A-Za-z0-9]+".toRegex(), "_").replace("^_*".toRegex(), "")
            .replace("_*$".toRegex(), "")
    }

    override suspend fun syncRemovedLabel(
        imsProject: IMSProject, issueId: String, label: Label, users: List<User>
    ): TimelineItemConversionInformation? {
        val response = jiraDataService.request(
            imsProject, users, HttpMethod.Put, JsonObject(
                mapOf(
                    "update" to JsonObject(
                        mapOf(
                            "labels" to JsonArray(
                                listOf(
                                    JsonObject(
                                        mapOf(
                                            "remove" to JsonPrimitive(
                                                jirafyLabelName(label.name)
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        ) {
            appendPathSegments("issue")
            appendPathSegments(issueId)
            parameters.append("returnIssue", "true")
            parameters.append("expand", "names,schema,editmeta,changelog")
        }
        val changelogEntry = response.second.body<IssueBean>().changelog.histories.lastOrNull()
        return JiraTimelineItemConversionInformation(
            imsProject.rawId!!, if (changelogEntry?.items?.singleOrNull()?.field == "labels") changelogEntry.id else ""
        )
    }

    override suspend fun createOutgoingIssue(imsProject: IMSProject, issue: Issue): IssueConversionInformation? {
        try {
            val imsProjectConfig = IMSProjectConfig(helper, imsProject)
            val iid = jiraDataService.request(
                imsProject,
                listOf(issue.createdBy().value, issue.lastModifiedBy().value) + issue.timelineItems()
                    .map { it.createdBy().value },
                HttpMethod.Post,
                IssueQueryRequest(
                    IssueQueryRequestFields(
                        issue.title,
                        issue.bodyBody,
                        IssueTypeRequest("Bug"),
                        ProjectRequest(imsProjectConfig.repo),
                        listOf()
                    )
                )
            ) {
                appendPathSegments("issue")
            }.second.body<JsonObject>()["id"]!!.jsonPrimitive.content
            return IssueConversionInformation(imsProject.rawId!!, iid, issue.rawId!!)
        } catch (e: ClientRequestException) {
            return null
        }
    }
}
