package gropius.sync.jira

import gropius.model.architecture.IMSProject
import gropius.model.issue.Issue
import gropius.model.issue.Label
import gropius.model.issue.timeline.Assignment
import gropius.model.issue.timeline.IssueComment
import gropius.model.issue.timeline.TemplatedFieldChangedEvent
import gropius.model.issue.timeline.TimelineItem
import gropius.model.template.IMSTemplate
import gropius.model.template.IssueState
import gropius.model.user.GropiusUser
import gropius.model.user.IMSUser
import gropius.model.user.User
import gropius.sync.*
import gropius.sync.jira.config.IMSConfig
import gropius.sync.jira.config.IMSConfigManager
import gropius.sync.jira.config.IMSProjectConfig
import gropius.sync.jira.model.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Details of the issue status after the transition.
 *
 * @param id the id of the status
 * @param name the name of the status
 */
@Serializable
data class StatusDetails(val id: String, val name: String) {}

/**
 * A transition that can be performed by the user on an issue
 * @param id the id of the transition
 * @param name the name of the transition
 * @param to the status details of the transition
 */
@Serializable
data class Transition(val id: String, val name: String, val to: StatusDetails) {}

/**
 * all transitions or a transition that can be performed by the user on an issue, based on the issue's status.
 * @param transitions a list of transitions
 */
@Serializable
data class TransitionList(val transitions: List<Transition>) {}

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
        val JQL_FORMATTER = DateTimeFormatter.ofPattern("\"yyyy-MM-dd HH:mm\"")
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

    override suspend fun isOutgoingTemplatedFieldsEnabled(imsProject: IMSProject): Boolean {
        val imsProjectConfig = IMSProjectConfig(helper, imsProject)
        return imsProjectConfig.enableOutgoingTemplatedFields
    }

    override suspend fun fetchData(imsProjects: List<IMSProject>) {
        for (imsProject in imsProjects) {
            jiraDataService.issueTemplate(imsProject)
            jiraDataService.issueType(imsProject, "")
            jiraDataService.issueState(imsProject, null, true)
            jiraDataService.issueState(imsProject, null, false)
        }

        for (imsProject in imsProjects) {
            fetchIssueList(imsProject)
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
                val issueCommentList = jiraDataService.request<Unit>(imsProject, listOf(), HttpMethod.Get, listOf()) {
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
        issueList: List<IssueData>, imsProject: IMSProject
    ) {
        for (issue in issueList) {
            val times = mutableListOf<OffsetDateTime>()
            var startAt = 0
            while (true) {
                val issueCommentList = jiraDataService.request<Unit>(imsProject, listOf(), HttpMethod.Get, listOf()) {
                    appendPathSegments("issue")
                    appendPathSegments(issue.jiraId)
                    appendPathSegments("comment")
                    parameters.append("expand", "names,schema,editmeta,changelog")
                    parameters.append("startAt", "$startAt")
                }.second.body<CommentQuery>()
                issueCommentList.comments.forEach {
                    issueDataService.insertComment(imsProject, issue.jiraId, it)
                }
                startAt = issueCommentList.startAt + issueCommentList.comments.size
                if (startAt >= issueCommentList.total) {
                    break
                }
            }
            for (comment in issue.comments.values) {
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
            for (history in issue.changelog.histories) {
                times.add(
                    OffsetDateTime.parse(
                        history.created, IssueData.formatter
                    )
                )
            }
            val updated = issue.fields["updated"]
            if (updated != null) {
                times.add(
                    OffsetDateTime.parse(
                        updated.jsonPrimitive.content, IssueData.formatter
                    )
                )
            }
            val lastSeenTime = times.maxOrNull()
            if (lastSeenTime != null) {
                syncStatusService.updateTime(imsProject.rawId!!, lastSeenTime)
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
        issueList: List<IssueData>, imsProject: IMSProject
    ) {
        logger.info("ISSUE LIST $issueList")
        val imsConfig = IMSConfig(helper, imsProject.ims().value, imsProject.ims().value.template().value)
        if (imsConfig.isCloud) {
            fetchIssueContentChangelog(issueList.map { it.jiraId }, imsProject)
        }
        fetchIssueContentComments(issueList, imsProject)
    }

    /**
     * Fetch the list of changed issues
     * @param imsProject the IMS project
     * @return issueList the list of issues
     */
    @OptIn(ExperimentalEncodingApi::class)
    private suspend fun fetchIssueList(imsProject: IMSProject) {
        var startAt = 0
        val lastSuccessfulSync: OffsetDateTime? =
            syncStatusService.findByImsProject(imsProject.rawId!!)?.lastSuccessfulSync
        val imsProjectConfig = IMSProjectConfig(helper, imsProject)
        val userList = jiraDataService.collectRequestUsers(imsProject, listOf())
        while (true) {
            val issueList = mutableListOf<IssueData>()
            val times = mutableListOf<OffsetDateTime>()
            val issueResponse = jiraDataService.tokenManager.executeUntilWorking(imsProject, userList, listOf()) {
                val userTimeZone = ZoneId.of(
                    jiraDataService.sendRequest<Unit>(
                        imsProject, HttpMethod.Get, null, {
                            appendPathSegments("myself")
                        }, it
                    ).get().body<UserQuery>().timeZone
                )
                var query = "project=${imsProjectConfig.repo} ORDER BY updated ASC"
                if (lastSuccessfulSync != null) {
                    query = "project=${imsProjectConfig.repo} AND updated > ${
                        lastSuccessfulSync.atZoneSameInstant(userTimeZone).format(JQL_FORMATTER)
                    } ORDER BY updated ASC"
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
                issueList.add(it)
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
            val lastSeenTime = times.maxOrNull()
            fetchIssueContent(issueList, imsProject)
            if (lastSeenTime != null) {
                syncStatusService.updateTime(imsProject.rawId!!, lastSeenTime)
            }
            startAt = issueResponse.startAt + issueResponse.issues.size
            if (startAt >= issueResponse.total) {
                break
            }
        }
    }

    override suspend fun findUnsyncedIssues(imsProject: IMSProject): List<IncomingIssue> {
        return issueDataService.findByImsProject(imsProject.rawId!!)
    }

    override suspend fun syncSingleAssigned(
        imsProject: IMSProject, issueId: String, assignment: Assignment, users: List<User>
    ): TimelineItemConversionInformation? {
        val assignedUser = assignment.user().value
        val imsUsers =
            if (assignedUser as? IMSUser != null) listOf(assignedUser) else if (assignedUser as? GropiusUser != null) assignedUser.imsUsers()
                .filter { it.ims().value == imsProject.ims().value } else emptyList()
        val ids = imsUsers.map { it.username }
        if (ids.isEmpty()) {
            return null
        }
        val response = jiraDataService.request(
            imsProject, users, HttpMethod.Put, gropiusUserList(users), JsonObject(
                mapOf(
                    "fields" to JsonObject(
                        mapOf(
                            "assignee" to JsonObject(
                                mapOf(
                                    "name" to JsonPrimitive(ids.first())
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
            imsProject.rawId!!,
            if (changelogEntry?.items?.singleOrNull()?.field == "assignee") changelogEntry.id else ""
        )
    }

    override suspend fun syncSingleUnassigned(
        imsProject: IMSProject, issueId: String, assignment: Assignment, users: List<User>
    ): TimelineItemConversionInformation? {
        val response = jiraDataService.request(
            imsProject, users, HttpMethod.Put, gropiusUserList(users), JsonObject(
                mapOf(
                    "fields" to JsonObject(
                        mapOf(
                            "assignee" to JsonNull
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
            imsProject.rawId!!,
            if (changelogEntry?.items?.singleOrNull()?.field == "assignee") changelogEntry.id else ""
        )
    }

    override suspend fun syncComment(
        imsProject: IMSProject, issueId: String, issueComment: IssueComment, users: List<User>
    ): TimelineItemConversionInformation? {
        if (issueComment.body.isNullOrEmpty()) {
            return null
        }
        val response = jiraDataService.request(
            imsProject,
            users,
            HttpMethod.Post,
            gropiusUserList(users),
            JsonObject(mapOf("body" to JsonPrimitive(issueComment.body)))
        ) {
            appendPathSegments("issue")
            appendPathSegments(issueId)
            appendPathSegments("comment")
        }.second.body<JsonObject>()
        val iid = response["id"]!!.jsonPrimitive.content
        return JiraTimelineItemConversionInformation(imsProject.rawId!!, iid)
    }

    override suspend fun syncFallbackComment(
        imsProject: IMSProject, issueId: String, comment: String, original: TimelineItem?, users: List<User>
    ): TimelineItemConversionInformation? {
        val response = jiraDataService.request(
            imsProject,
            users,
            HttpMethod.Post,
            gropiusUserList(users),
            JsonObject(mapOf("body" to JsonPrimitive(comment)))
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
            imsProject, users, HttpMethod.Put, gropiusUserList(users), JsonObject(
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

    override suspend fun syncTemplatedField(
        imsProject: IMSProject, issueId: String, fieldChangedEvent: TemplatedFieldChangedEvent, users: List<User>
    ): TimelineItemConversionInformation? {
        val response = jiraDataService.request(
            imsProject, users, HttpMethod.Put, gropiusUserList(users), JsonObject(
                mapOf(
                    "fields" to JsonObject(
                        mapOf(
                            fieldChangedEvent.fieldName to JsonPrimitive(fieldChangedEvent.newValue)
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
            imsProject.rawId!!,
            if (changelogEntry?.items?.singleOrNull()?.field == fieldChangedEvent.fieldName) changelogEntry.id else ""
        )
    }

    override suspend fun syncStateChange(
        imsProject: IMSProject, issueId: String, newState: IssueState, users: List<User>
    ): TimelineItemConversionInformation? {
        val transitions = jiraDataService.request<Unit>(imsProject, users, HttpMethod.Get, gropiusUserList(users)) {
            appendPathSegments("issue")
            appendPathSegments(issueId)
            appendPathSegments("transitions")
        }
        logger.info("syncStateChange transitions ${transitions.second.bodyAsText()} ${transitions.second.body<TransitionList>()} ${newState.name} ${transitions.second.body<TransitionList>().transitions.firstOrNull { it.to.name == newState.name }}")
        val transition =
            transitions.second.body<TransitionList>().transitions.firstOrNull { it.to.name == newState.name }
                ?: return JiraTimelineItemConversionInformation(
                    imsProject.rawId!!, "no transition possible"
                )
        val response = jiraDataService.request(
            imsProject, users, HttpMethod.Post, gropiusUserList(users), JsonObject(
                mapOf(
                    "transition" to JsonObject(
                        mapOf(
                            "id" to JsonPrimitive(transition.id)
                        )
                    )
                )
            )
        ) {
            appendPathSegments("issue")
            appendPathSegments(issueId)
            appendPathSegments("transitions")
        }
        logger.info("syncStateChange response ${response.second.bodyAsText()}")
        return JiraTimelineItemConversionInformation(
            imsProject.rawId!!, "it is safer to duplicate this timeline item"
        )
    }

    override suspend fun syncAddedLabel(
        imsProject: IMSProject, issueId: String, label: Label, users: List<User>
    ): TimelineItemConversionInformation? {
        val response = jiraDataService.request(
            imsProject, users, HttpMethod.Put, gropiusUserList(users), JsonObject(
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
            imsProject, users, HttpMethod.Put, gropiusUserList(users), JsonObject(
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
                gropiusUserList(listOf(issue.createdBy().value, issue.lastModifiedBy().value) + issue.timelineItems()
                    .map { it.createdBy().value }),
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
