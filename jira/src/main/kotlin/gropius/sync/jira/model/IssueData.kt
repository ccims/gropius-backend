package gropius.sync.jira.model

import com.fasterxml.jackson.databind.JsonNode
import gropius.model.architecture.IMSProject
import gropius.model.architecture.Project
import gropius.model.issue.Issue
import gropius.model.issue.timeline.*
import gropius.sync.IncomingIssue
import gropius.sync.IncomingTimelineItem
import gropius.sync.SyncDataService
import gropius.sync.TimelineItemConversionInformation
import gropius.sync.jira.JiraDataService
import gropius.util.schema.Schema
import gropius.util.schema.Type
import jakarta.transaction.Transactional
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.serialization.json.*
import org.bson.*
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.data.neo4j.core.findById
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/**
 * TimelineItem maping between Jira and Gropius
 * @param imsProject IMSProject the timeline item belongs to
 * @param githubId the id of the timeline item in github
 */
class JiraTimelineItemConversionInformation(
    imsProject: String, githubId: String
) : TimelineItemConversionInformation(imsProject, githubId, null) {}

/**
 * Intermediate representation of a single TimelineItem from Jira
 * @param id the id of the timeline item
 * @param created the creation date of the timeline item
 * @param author the author of the timeline item
 * @param data the data of the timeline item
 */
data class JiraTimelineItem(
    val id: String,
    val created: String,
    val author: JsonObject,
    val data: ChangelogFieldEntry
) :
    IncomingTimelineItem() {
    /**
     * Logger used to print notifications
     */
    private val logger = LoggerFactory.getLogger(JiraTimelineItem::class.java)

    override suspend fun identification(): String {
        return id
    }

    override suspend fun gropiusTimelineItem(
        imsProject: IMSProject,
        service: SyncDataService,
        timelineItemConversionInformation: TimelineItemConversionInformation?,
        issue: Issue
    ): Pair<List<TimelineItem>, TimelineItemConversionInformation> {
        val jiraService = (service as JiraDataService)
        if (data.fieldId == "summary") {
            return gropiusSummary(timelineItemConversionInformation, imsProject, service, jiraService)
        } else if (data.fieldId == "resolution") {
            return gropiusState(
                timelineItemConversionInformation, imsProject, service, jiraService
            )
        } else if (data.fieldId == "labels") {
            return gropiusLabels(
                timelineItemConversionInformation, imsProject, service, jiraService
            )
        }
        println("SPECS ${issue.template().value.templateFieldSpecifications} for ${data.field}")
        if (issue.template().value.templateFieldSpecifications.containsKey(data.field)) {
            val schema = issue.template().value.templateFieldSpecifications[data.field]!!
            val parsedSchema = jiraService.objectMapper.readValue(schema, Schema::class.java)

            println("Parsed schema ${issue.template().value.templateFieldSpecifications[data.field]!!} to $parsedSchema is ${parsedSchema.type == Type.STRING}")

            if (parsedSchema.type == Type.STRING) {
                return gropiusTemplatedField(
                    timelineItemConversionInformation, imsProject, service, jiraService, parsedSchema.nullable
                )
            }
        }
        val convInfo =
            timelineItemConversionInformation ?: JiraTimelineItemConversionInformation(imsProject.rawId!!, id);
        return listOf<TimelineItem>() to convInfo;
    }

    private suspend fun gropiusTemplatedField(
        timelineItemConversionInformation: TimelineItemConversionInformation?,
        imsProject: IMSProject,
        service: JiraDataService,
        jiraService: JiraDataService,
        isNull: Boolean
    ): Pair<List<TimelineItem>, TimelineItemConversionInformation> {
        val convInfo =
            timelineItemConversionInformation ?: JiraTimelineItemConversionInformation(imsProject.rawId!!, id);
        val timelineId = timelineItemConversionInformation?.gropiusId
        val source = data.from
        val destination = data.to
        logger.info("GOING FROM $source to $destination")
        val templateEvent: TemplatedFieldChangedEvent =
            (if (timelineId != null) service.neoOperations.findById<TemplatedFieldChangedEvent>(
                timelineId
            ) else null) ?: TemplatedFieldChangedEvent(
                OffsetDateTime.parse(
                    created, IssueData.formatter
                ), OffsetDateTime.parse(
                    created, IssueData.formatter
                ), data.field, service.jsonNodeMapper.jsonNodeToDeterministicString(
                    service.objectMapper.valueToTree<JsonNode>(
                        data.from ?: data.fromString ?: (if (!isNull) "" else null)
                    )
                ), service.jsonNodeMapper.jsonNodeToDeterministicString(
                    service.objectMapper.valueToTree<JsonNode>(
                        data.to ?: data.toString ?: (if (!isNull) "" else null)
                    )
                )
            )
        templateEvent.createdBy().value = jiraService.mapUser(imsProject, author)
        templateEvent.lastModifiedBy().value = jiraService.mapUser(imsProject, author)
        return listOf<TimelineItem>(
            templateEvent
        ) to convInfo;
    }

    /**
     * Convert a Jira Label to Gropius Label
     * @param timelineItemConversionInformation the timeline item conversion information
     * @param imsProject the ims project
     * @param service the service
     * @param jiraService the jira service
     * @return the pair of timeline items and conversion information
     */
    private suspend fun gropiusLabels(
        timelineItemConversionInformation: TimelineItemConversionInformation?,
        imsProject: IMSProject,
        service: JiraDataService,
        jiraService: JiraDataService
    ): Pair<List<TimelineItem>, TimelineItemConversionInformation> {
        val convInfo =
            timelineItemConversionInformation ?: JiraTimelineItemConversionInformation(imsProject.rawId!!, id);
        val timelineId = timelineItemConversionInformation?.gropiusId
        val sourceSet = data.fromString!!.split(' ').toSet()
        val destinationSet = data.toString!!.split(' ').toSet()
        logger.info("GOING FROM $sourceSet to $destinationSet, meaning added: ${(destinationSet subtract sourceSet)} and removed ${(sourceSet subtract destinationSet)}")
        for (addedLabel in (destinationSet subtract sourceSet)) {
            val addedLabelEvent: AddedLabelEvent =
                (if (timelineId != null) service.neoOperations.findById<AddedLabelEvent>(
                    timelineId
                ) else null) ?: AddedLabelEvent(
                    OffsetDateTime.parse(
                        created, IssueData.formatter
                    ), OffsetDateTime.parse(
                        created, IssueData.formatter
                    )
                )
            addedLabelEvent.createdBy().value = jiraService.mapUser(imsProject, author)
            addedLabelEvent.lastModifiedBy().value = jiraService.mapUser(imsProject, author)
            addedLabelEvent.addedLabel().value = jiraService.mapLabel(imsProject, addedLabel)
            return listOf<TimelineItem>(
                addedLabelEvent
            ) to convInfo;
        }
        for (removedLabel in (sourceSet subtract destinationSet)) {
            val removedLabelEvent: RemovedLabelEvent =
                (if (timelineId != null) service.neoOperations.findById<RemovedLabelEvent>(
                    timelineId
                ) else null) ?: RemovedLabelEvent(
                    OffsetDateTime.parse(
                        created, IssueData.formatter
                    ), OffsetDateTime.parse(
                        created, IssueData.formatter
                    )
                )
            removedLabelEvent.createdBy().value = jiraService.mapUser(imsProject, author)
            removedLabelEvent.lastModifiedBy().value = jiraService.mapUser(imsProject, author)
            removedLabelEvent.removedLabel().value = jiraService.mapLabel(imsProject, removedLabel)
            return listOf<TimelineItem>(
                removedLabelEvent
            ) to convInfo;
        }
        return listOf<TimelineItem>() to convInfo;
    }

    /**
     * Convert a single state change to a Gropius StateChangedEvent
     * @param timelineItemConversionInformation the timeline item conversion information
     * @param imsProject the ims project
     * @param service the service
     * @param jiraService the jira service
     * @return the pair of timeline items and conversion information
     */
    private suspend fun gropiusState(
        timelineItemConversionInformation: TimelineItemConversionInformation?,
        imsProject: IMSProject,
        service: JiraDataService,
        jiraService: JiraDataService
    ): Pair<List<TimelineItem>, TimelineItemConversionInformation> {
        val convInfo =
            timelineItemConversionInformation ?: JiraTimelineItemConversionInformation(imsProject.rawId!!, id);
        val timelineId = timelineItemConversionInformation?.gropiusId
        val titleChangedEvent: StateChangedEvent =
            (if (timelineId != null) service.neoOperations.findById<StateChangedEvent>(
                timelineId
            ) else null) ?: StateChangedEvent(
                OffsetDateTime.parse(
                    created, IssueData.formatter
                ), OffsetDateTime.parse(
                    created, IssueData.formatter
                )
            )
        titleChangedEvent.createdBy().value = jiraService.mapUser(imsProject, author)
        titleChangedEvent.lastModifiedBy().value = jiraService.mapUser(imsProject, author)
        titleChangedEvent.oldState().value = jiraService.issueState(imsProject, data.fromString == null)
        titleChangedEvent.newState().value = jiraService.issueState(imsProject, data.toString == null)
        return listOf<TimelineItem>(
            titleChangedEvent
        ) to convInfo;
    }

    /**
     * Convert a single title change to a Gropius TitleChangedEvent
     * @param timelineItemConversionInformation the timeline item conversion information
     * @param imsProject the ims project
     * @param service the service
     * @param jiraService the jira service
     * @return the pair of timeline items and conversion information
     */
    private suspend fun gropiusSummary(
        timelineItemConversionInformation: TimelineItemConversionInformation?,
        imsProject: IMSProject,
        service: JiraDataService,
        jiraService: JiraDataService
    ): Pair<List<TimelineItem>, TimelineItemConversionInformation> {
        val convInfo =
            timelineItemConversionInformation ?: JiraTimelineItemConversionInformation(imsProject.rawId!!, id);
        val timelineId = timelineItemConversionInformation?.gropiusId
        val titleChangedEvent: TitleChangedEvent =
            (if (timelineId != null) service.neoOperations.findById<TitleChangedEvent>(
                timelineId
            ) else null) ?: TitleChangedEvent(
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
}

/**
 * Intermediate representation of a single comment from Jira
 * @param id the id of the comment
 * @param comment the comment
 */
class JiraCommentTimelineItem(val issueId: String, val comment: JiraComment) : IncomingTimelineItem() {
    override suspend fun identification(): String {
        return comment.id
    }

    override suspend fun gropiusTimelineItem(
        imsProject: IMSProject,
        service: SyncDataService,
        timelineItemConversionInformation: TimelineItemConversionInformation?,
        issue: Issue
    ): Pair<List<TimelineItem>, TimelineItemConversionInformation> {
        val jiraService = (service as JiraDataService)
        val convInfo = timelineItemConversionInformation ?: JiraTimelineItemConversionInformation(
            imsProject.rawId!!, identification()
        );
        val timelineId = timelineItemConversionInformation?.gropiusId
        val commentEvent: IssueComment = (if (timelineId != null) service.neoOperations.findById<IssueComment>(
            timelineId
        ) else null) ?: IssueComment(
            OffsetDateTime.parse(
                comment.created, IssueData.formatter
            ), OffsetDateTime.parse(
                comment.updated, IssueData.formatter
            ), comment.body, OffsetDateTime.parse(
                comment.updated, IssueData.formatter
            ), false
        )
        commentEvent.createdBy().value = jiraService.mapUser(imsProject, comment.author)
        commentEvent.lastModifiedBy().value = jiraService.mapUser(imsProject, comment.updateAuthor)
        commentEvent.bodyLastEditedBy().value = jiraService.mapUser(imsProject, comment.updateAuthor)
        return listOf<TimelineItem>(
            commentEvent
        ) to convInfo;
    }

}

/**
 * Data of a single issue from the API to the database
 * @param imsProject the ims project the issue belongs to
 * @param expand the expand string
 * @param jiraId the id of the issue in Jira
 * @param self the self link
 * @param key the key of the issue
 * @param editmeta the edit meta data
 * @param changelog the changelog
 * @param fields the fields
 * @param names the names
 * @param schema the schema
 * @param comments the comments
 */
@Document
data class IssueData(
    val imsProject: String,
    val expand: String,
    val jiraId: String,
    val self: String,
    val key: String,
    var editmeta: JsonObject,
    var changelog: ChangeLogContainer,
    val fields: MutableMap<String, JsonElement>,
    var names: JsonObject,
    var schema: JsonObject,
    val comments: MutableMap<String, JiraComment> = mutableMapOf()
) : IncomingIssue() {

    /**
     * MongoDB ID
     */
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
        } + comments.map { JiraCommentTimelineItem(identification(), it.value) }
    }

    override suspend fun identification(): String {
        return jiraId
    }

    override suspend fun markDone(service: SyncDataService) {
        //TODO("Not yet implemented")
    }

    override suspend fun fillImsIssueTemplatedFields(
        templatedFields: MutableMap<String, String>,
        service: SyncDataService
    ) {
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
            created,
            updated,
            mutableMapOf(),
            fields["summary"]!!.jsonPrimitive.content,
            fields["description"]!!.jsonPrimitive.content,
            updated,
            null,
            null,
            null,
            null
        )
        issue.body().value = Body(
            created, updated, updated
        )
        issue.body().value.lastModifiedBy().value = jiraService.mapUser(imsProject, fields["reporter"]!!)
        issue.body().value.bodyLastEditedBy().value = jiraService.mapUser(imsProject, fields["reporter"]!!)
        issue.body().value.createdBy().value = jiraService.mapUser(imsProject, fields["reporter"]!!)
        issue.createdBy().value = jiraService.mapUser(imsProject, fields["creator"]!!)
        issue.lastModifiedBy().value = jiraService.mapUser(imsProject, fields["creator"]!!)
        issue.body().value.issue().value = issue
        issue.state().value = jiraService.issueState(imsProject, true)
        issue.template().value = jiraService.issueTemplate(imsProject)
        issue.trackables() += jiraService.neoOperations.findAll(Project::class.java).awaitFirst()
        issue.type().value = jiraService.issueType(imsProject)
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
        TODO("$this")
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

/**
 * Repository for IssueData
 */
@Repository
interface IssueDataRepository : ReactiveMongoRepository<IssueData, ObjectId> {

    /**
     * Find an issue by its IMSProject and JiraId
     * @param imsProject the IMSProject the issue belongs to
     * @param jiraId the JiraId of the issue
     * @return the issue if found, null otherwise
     */
    suspend fun findByImsProjectAndJiraId(
        imsProject: String, jiraId: String
    ): IssueData?

    /**
     * Find all issues by their IMSProject
     * @param imsProject the IMSProject the issues belong to
     * @return the issues
     */
    suspend fun findByImsProject(
        imsProject: String
    ): List<IssueData>
}

@Service
class IssueDataService(val issuePileRepository: IssueDataRepository) : IssueDataRepository by issuePileRepository {
    /**
     * Logger used to print notifications
     */
    private val logger = LoggerFactory.getLogger(IssueDataService::class.java)

    /**
     * Insert an issue into the database
     * @param imsProject the IMSProject the issue belongs to
     * @param rawIssueData the issue data
     */
    @Transactional
    suspend fun insertIssue(imsProject: IMSProject, rawIssueData: IssueData) {
        logger.info("LOOKING FOR ${imsProject.rawId!!} AND ${rawIssueData.jiraId}")
        val issueData = findByImsProjectAndJiraId(imsProject.rawId!!, rawIssueData.jiraId) ?: rawIssueData
        logger.info("ISSUE ${issueData.id}")
        val knownParts = issueData.changelog.histories.map { it.id }.toSet()
        rawIssueData.changelog.histories.forEach {
            if (!knownParts.contains(it.id)) {
                issueData.changelog.histories.add(it)
            }
        }
        issuePileRepository.save(issueData).awaitSingle()
    }

    /**
     * Insert a comment into the database
     * @param imsProject the IMSProject the comment belongs to
     * @param jiraId the JiraId of the issue the comment belongs to
     * @param comment the comment
     */
    @Transactional
    suspend fun insertComment(imsProject: IMSProject, jiraId: String, comment: JiraComment) {
        val issueData = findByImsProjectAndJiraId(imsProject.rawId!!, jiraId)!!
        if (!issueData.comments.containsKey(comment.id)) {
            issueData.comments.put(comment.id, comment)
            issuePileRepository.save(issueData).awaitSingle()
        }
    }
}
