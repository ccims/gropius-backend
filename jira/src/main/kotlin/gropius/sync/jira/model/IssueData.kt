package gropius.sync.jira.model

import gropius.model.architecture.IMSProject
import gropius.model.architecture.Project
import gropius.model.issue.Issue
import gropius.model.issue.timeline.Body
import gropius.model.issue.timeline.TimelineItem
import gropius.model.issue.timeline.TitleChangedEvent
import gropius.sync.IncomingIssue
import gropius.sync.IncomingTimelineItem
import gropius.sync.SyncDataService
import gropius.sync.TimelineItemConversionInformation
import gropius.sync.jira.JiraDataService
import jakarta.transaction.Transactional
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.serialization.json.*
import org.bson.*
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class JiraTimelineItemConversionInformation(
    imsProject: String, githubId: String
) : TimelineItemConversionInformation(imsProject, githubId, null) {}

class JiraTimelineItem(val id: String, val created: String, val author: JsonObject, val data: ChangelogFieldEntry) :
    IncomingTimelineItem() {
    override suspend fun identification(): String {
        return id
    }

    override suspend fun gropiusTimelineItem(
        imsProject: IMSProject,
        service: SyncDataService,
        timelineItemConversionInformation: TimelineItemConversionInformation?
    ): Pair<List<TimelineItem>, TimelineItemConversionInformation> {
        val jiraService = (service as JiraDataService)
        if (data.fieldId == "summary") {
            val convInfo =
                timelineItemConversionInformation ?: JiraTimelineItemConversionInformation(imsProject.rawId!!, id);
            val titleChangedEvent: TitleChangedEvent = TitleChangedEvent(
                OffsetDateTime.parse(
                    created, IssueData.formatter
                ), OffsetDateTime.parse(
                    created, IssueData.formatter
                ), data.fromString!!, data.toString!!
            )
            titleChangedEvent.createdBy().value = jiraService.mapUser(imsProject, author)
            titleChangedEvent.lastModifiedBy().value = jiraService.mapUser(imsProject, author)
            return listOf<TimelineItem>(
                titleChangedEvent
            ) to convInfo;
        }
        val convInfo =
            timelineItemConversionInformation ?: JiraTimelineItemConversionInformation(imsProject.rawId!!, id);
        return listOf<TimelineItem>() to convInfo;
    }
}

@Document
data class IssueData(
    val imsProject: String,
    val expand: String,
    val jiraId: String,
    val self: String,
    val key: String,
    var editmeta: JsonObject,
    var changelog: ChangleLogContainer,
    val fields: MutableMap<String, JsonElement>,
    var names: JsonObject,
    var schema: JsonObject
) : IncomingIssue() {
    @Id
    var id: ObjectId? = null

    companion object {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd\'T\'HH:mm:ss.SSSZ")
    }

    fun bean() = IssueQuery(
        expand, jiraId, self, key, editmeta, changelog, fields.toMap(), names, schema
    )

    override suspend fun incomingTimelineItems(service: SyncDataService): List<IncomingTimelineItem> {
        return changelog.histories.flatMap { oit ->
            oit.items.map {
                JiraTimelineItem(
                    oit.id, oit.created, oit.author, it
                )
            }
        }
    }

    override suspend fun identification(): String {
        return jiraId
    }

    override suspend fun markDone(service: SyncDataService) {
        //TODO("Not yet implemented")
    }

    override suspend fun createIssue(imsProject: IMSProject, service: SyncDataService): Issue {
        val jiraService = (service as JiraDataService)
        val created = OffsetDateTime.parse(
            fields["created"]!!.jsonPrimitive.content, formatter
        )
        val updated = OffsetDateTime.parse(
            fields["updated"]!!.jsonPrimitive.content, formatter
        )
        val issue = Issue(
            created, updated, mutableMapOf(), fields["summary"]!!.jsonPrimitive.content, updated, null, null, null, null
        )
        println("DESC ${fields["description"]}")
        issue.body().value = Body(
            created, updated, fields["description"]!!.jsonPrimitive.content, updated
        )
        issue.body().value.lastModifiedBy().value = jiraService.mapUser(imsProject, fields["reporter"]!!)
        issue.body().value.bodyLastEditedBy().value = jiraService.mapUser(imsProject, fields["reporter"]!!)
        issue.body().value.createdBy().value = jiraService.mapUser(imsProject, fields["reporter"]!!)
        issue.createdBy().value = jiraService.mapUser(imsProject, fields["creator"]!!)
        issue.lastModifiedBy().value = jiraService.mapUser(imsProject, fields["creator"]!!)
        issue.body().value.issue().value = issue
        issue.state().value = jiraService.issueState()
        issue.template().value = jiraService.issueTemplate()
        issue.trackables() += jiraService.neoOperations.findAll(Project::class.java).awaitFirst()
        issue.type().value = jiraService.issueType()
        return issue
    }
}

public fun BsonValue.toKJson(): JsonElement {
    return if (this is BsonDocument) JsonObject(this.mapValues { it.value.toKJson() })
    else if (this is BsonArray) JsonArray(this.values.map { it.toKJson() })
    else if (this is BsonString) JsonPrimitive(this.value)
    else if (this is BsonInt64) JsonPrimitive(this.value)
    else if (this is BsonInt32) JsonPrimitive(this.value)
    else if (this is BsonBoolean) JsonPrimitive(this.value)
    else if (this is BsonDouble) JsonPrimitive(this.value)
    else if (this is BsonNull) JsonNull
    else {
        println(this)
        TODO()
    }
}

public fun BsonDocument.toKJson(): JsonObject {
    return JsonObject(this.mapValues { it.value.toKJson() })
}

public fun JsonElement.toBson(): BsonValue {
    return if (this is JsonObject) this.toBson() else if (this is JsonArray) BsonArray(this.map { it.toBson() })
    else if (this is JsonNull) BsonNull()
    else if (this is JsonPrimitive) {
        if (this.booleanOrNull != null) return BsonBoolean(this.boolean)
        if (this.longOrNull != null) return BsonInt64(this.long)
        if (this.doubleOrNull != null) return BsonDouble(this.double)
        if (this.isString) return BsonString(this.content)
        TODO()
    } else TODO()
}

public fun JsonObject.toBson(): BsonDocument {
    return BsonDocument(this.map {
        BsonElement(
            it.key, it.value.toBson()
        )
    })
}

@Repository
interface IssueDataRepository : ReactiveMongoRepository<IssueData, ObjectId> {
    suspend fun findByImsProjectAndJiraId(
        imsProject: String, jiraId: String
    ): IssueData?

    suspend fun findByImsProject(
        imsProject: String
    ): List<IssueData>

}

@Service
class IssueDataService(val issuePileRepository: IssueDataRepository) : IssueDataRepository by issuePileRepository {
    @Transactional
    suspend fun insertIssue(imsProject: IMSProject, rawIssueData: IssueData) {
        println("LOOKING FOR ${imsProject.rawId!!} AND ${rawIssueData.jiraId}")
        val issueData = findByImsProjectAndJiraId(imsProject.rawId!!, rawIssueData.jiraId) ?: rawIssueData
        println("ISSUE ${issueData.id}")
        issuePileRepository.save(issueData).awaitSingle()
    }
}
