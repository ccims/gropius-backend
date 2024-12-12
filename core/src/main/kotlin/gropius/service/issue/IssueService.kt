package gropius.service.issue

import com.expediagroup.graphql.generator.execution.OptionalInput
import com.expediagroup.graphql.generator.scalars.ID
import com.fasterxml.jackson.databind.JsonNode
import gropius.authorization.GropiusAuthorizationContext
import gropius.dto.input.common.DeleteNodeInput
import gropius.dto.input.common.JSONFieldInput
import gropius.dto.input.ifPresent
import gropius.dto.input.issue.*
import gropius.dto.input.orElse
import gropius.model.architecture.AffectedByIssue
import gropius.model.architecture.Trackable
import gropius.model.issue.Artefact
import gropius.model.issue.Issue
import gropius.model.issue.Label
import gropius.model.issue.timeline.*
import gropius.model.template.*
import gropius.model.user.GropiusUser
import gropius.model.user.User
import gropius.model.user.permission.NodePermission
import gropius.model.user.permission.TrackablePermission
import gropius.repository.GropiusRepository
import gropius.repository.architecture.AffectedByIssueRepository
import gropius.repository.architecture.TrackableRepository
import gropius.repository.common.NodeRepository
import gropius.repository.findAllById
import gropius.repository.findById
import gropius.repository.issue.ArtefactRepository
import gropius.repository.issue.IssueRepository
import gropius.repository.issue.LabelRepository
import gropius.repository.issue.timeline.*
import gropius.repository.template.*
import gropius.repository.user.UserRepository
import gropius.service.NodeBatchUpdateContext
import gropius.service.common.AuditedNodeService
import gropius.service.template.TemplatedNodeService
import gropius.util.JsonNodeMapper
import io.github.graphglue.authorization.Permission
import io.github.graphglue.model.Node
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.*
import kotlin.reflect.KMutableProperty0

/**
 * Service for [Issue]s. Provides function to update the deprecation status
 *
 * @param repository the associated repository used for CRUD functionality
 * @param labelRepository used to find [Label]s by id
 * @param timelineItemRepository used to save [TimelineItem]s
 * @param artefactRepository used to find [Artefact]s by id
 * @param trackableRepository used to find [Trackable]s by id
 * @param issuePriorityRepository used to find [IssuePriority]s by id
 * @param issueTypeRepository used to find [IssueType]s by id
 * @param issueStateRepository used to find [IssueState]s by id
 * @param affectedByIssueRepository used to find [AffectedByIssue]s by id
 * @param templatedNodeService used to validate and update templated fields
 * @param jsonNodeMapper used to serialize templated fields
 * @param userRepository used to find [User]s by id
 * @param assignmentRepository used to find [Assignment]s by id
 * @param assignmentTypeRepository used to find [AssignmentType]s by id
 * @param issueRelationRepository used to find [IssueRelation]s by id
 * @param issueRelationTypeRepository used to find [IssueRelationType]s by id
 * @param issueCommentRepository used to find [IssueComment]s by id
 * @param bodyRepository used to find [Body]s by id
 * @param commentRepository used to find [Comment]s by id
 * @param issueTemplateRepository used to find [IssueTemplate]s by id
 * @param nodeRepository used to delete [Node]s
 */
@Service
class IssueService(
    repository: IssueRepository,
    private val labelRepository: LabelRepository,
    private val timelineItemRepository: TimelineItemRepository,
    private val artefactRepository: ArtefactRepository,
    private val trackableRepository: TrackableRepository,
    private val issuePriorityRepository: IssuePriorityRepository,
    private val issueTypeRepository: IssueTypeRepository,
    private val issueStateRepository: IssueStateRepository,
    private val affectedByIssueRepository: AffectedByIssueRepository,
    private val templatedNodeService: TemplatedNodeService,
    private val jsonNodeMapper: JsonNodeMapper,
    private val userRepository: UserRepository,
    private val assignmentRepository: AssignmentRepository,
    private val assignmentTypeRepository: AssignmentTypeRepository,
    private val issueRelationRepository: IssueRelationRepository,
    private val issueRelationTypeRepository: IssueRelationTypeRepository,
    private val issueCommentRepository: IssueCommentRepository,
    private val bodyRepository: BodyRepository,
    private val commentRepository: CommentRepository,
    private val issueTemplateRepository: IssueTemplateRepository,
    private val nodeRepository: NodeRepository
) : AuditedNodeService<Issue, IssueRepository>(repository) {

    /**
     * Creates an [Issue].
     * Checks the authorization status, checks that the `type`, `state` and `templatedFields` are compatible with the
     * `template`
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines the created [Issue]
     * @return the saved crated [Issue]
     */
    suspend fun createIssue(authorizationContext: GropiusAuthorizationContext, input: CreateIssueInput): Issue {
        val trackables = trackableRepository.findAllById(input.trackables)
        for (trackable in trackables) {
            checkCreateIssuesPermission(trackable, authorizationContext)
        }
        val template = issueTemplateRepository.findById(input.template)
        val type = issueTypeRepository.findById(input.type)
        val state = issueStateRepository.findById(input.state)
        val byUser = getUser(authorizationContext)
        val updateContext = NodeBatchUpdateContext()
        val issue = createIssue(
            trackables,
            template,
            input.title,
            input.body,
            type,
            state,
            input.templatedFields,
            OffsetDateTime.now(),
            byUser,
            updateContext
        )
        createdAuditedNode(issue, byUser)
        return updateContext.save(issue, nodeRepository)
    }

    /**
     * Creates an [Issue] at [atTime] as [byUser] on [trackables].
     * Also creates the [Body] of the [Issue].
     * Does not check the authorization status.
     * Checks that [trackables] is not empty, checks that the [type], [state] and [templatedFields] are compatible with
     * [template].
     * Does not save the created [Issue], it is required to save the returned [Issue] afterwards.
     *
     * @param trackables the [Trackable]s on which the [Issue] is created
     * @param template the initial [template]
     * @param title the initial title
     * @param body the initial body, used to create the [Body]
     * @param type the initial type, must be compatible with [template]
     * @param state the initial state, must be compatible with [template]
     * @param templatedFields the initial templated fields, must be compatible with [template]
     * @param atTime the point in time when the [Issue] is created
     * @param byUser the [User] who creates the [Issue]
     * @param updateContext context used for the aggregation updater
     * @return the created [Issue]
     */
    suspend fun createIssue(
        trackables: List<Trackable>,
        template: IssueTemplate,
        title: String,
        body: String,
        type: IssueType,
        state: IssueState,
        templatedFields: List<JSONFieldInput>,
        atTime: OffsetDateTime,
        byUser: User,
        updateContext: NodeBatchUpdateContext
    ): Issue {
        if (trackables.isEmpty()) {
            throw IllegalStateException("An Issue must be created on at least one Trackable")
        }
        val fields = templatedNodeService.validateInitialTemplatedFields(template, templatedFields)
        val issue = Issue(atTime, atTime, fields, title, body, atTime, null, null, null, null)
        issue.template().value = template
        checkIssueTypeCompatibility(issue, type)
        checkIssueStateCompatibility(issue, state)
        issue.type().value = type
        issue.state().value = state
        createdAuditedNode(issue, byUser)
        val bodyItem = Body(atTime, atTime, atTime)
        bodyItem.bodyLastEditedBy().value = byUser
        createdTimelineItem(issue, bodyItem, atTime, byUser)
        issue.body().value = bodyItem
        for (trackable in trackables) {
            addIssueToTrackable(issue, trackable, atTime, byUser, updateContext)
        }
        return issue
    }

    /**
     * Changes the template of an [Issue], return the created [TemplateChangedEvent] or `null` if the [Issue]
     * was already pinned on the [Trackable].
     * Checks the authorization status, checks that all inputs are compatible with the new template.
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines the new [IssueTemplate] and of which [Issue] to change it
     * @return the saved created [TemplateChangedEvent] or `null` if no event was created
     */
    suspend fun changeIssueTemplate(
        authorizationContext: GropiusAuthorizationContext, input: ChangeIssueTemplateInput
    ): TemplateChangedEvent? {
        val issue = repository.findById(input.issue)
        checkManageIssuesPermission(issue, authorizationContext)
        val template = issueTemplateRepository.findById(input.template)
        return if (issue.template().value != template) {
            val updateContext = NodeBatchUpdateContext()
            val event = changeIssueTemplate(
                issue,
                issue.template().value,
                template,
                input.templatedFields.orElse(emptyList()),
                input.type.orElse(null)?.let { issueTypeRepository.findById(it) },
                input.state.orElse(null)?.let { issueStateRepository.findById(it) },
                input.priority.orElse(null)?.let { issuePriorityRepository.findById(it) },
                input.assignmentTypeMapping.toMapping(assignmentTypeRepository),
                input.issueRelationTypeMapping.toMapping(issueRelationTypeRepository),
                OffsetDateTime.now(),
                getUser(authorizationContext),
                updateContext
            )
            updateContext.save(event, nodeRepository)
        } else {
            null
        }
    }

    /**
     * Transforms a list of [TypeMappingInput] to a mapping using the provided [typeRepository]
     *
     * @param T the type of the returned types
     * @param typeRepository used to map [ID] to [T]
     * @return the generated mapping
     */
    private suspend fun <T : Node> OptionalInput<List<TypeMappingInput>>.toMapping(
        typeRepository: GropiusRepository<T, String>
    ): Map<T, T?> {
        ifPresent { inputs ->
            val allTypeIds = inputs.flatMap { listOf(it.newType, it.oldType) }.filterNotNull().toSet()
            val allTypesById = typeRepository.findAllById(allTypeIds).associateBy { it.graphQLId }
            return inputs.associate { allTypesById[it.oldType]!! to allTypesById[it.newType] }
        }
        return emptyMap()
    }

    /**
     * Changes the template of an [Issue], return the created [TemplateChangedEvent].
     * Also creates the [TemplateChangedEvent] if the template of the issue does not change.
     * Uses [templatedFields], [type], [state], [priority], [assignmentTypeMapping] and [issueRelationTypeMapping] to
     * enforce compatibility with the new template, however only updates fields which are incompatible with the new
     * template.
     * Does not check the authorization status.
     * Checks that all inputs are compatible with the new template, and checks that the templatedFields are valid
     * after the update.
     *
     * @param issue the [Issue] to update the template of
     * @param oldTemplate the old template of the [issue]
     * @param newTemplate the new template of the [issue]
     * @param templatedFields values to update incompatible templated fields
     * @param type new [IssueType] in case existing one is incompatible
     * @param state new [IssueState] in case existing one is incompatible
     * @param priority new [IssuePriority] in case existing one is incompatible
     * @param assignmentTypeMapping used to map incompatible [AssignmentType]s
     * @param issueRelationTypeMapping used to map incompatible [IssueRelationType]s
     * @param atTime the point in time when the modification happened, updates [Issue.lastUpdatedAt] if necessary
     * @param byUser the [User] who caused the update, updates [Issue.participants] if necessary
     * @param updateContext context used for the aggregation updater
     * @return the created [TemplateChangedEvent]
     */
    suspend fun changeIssueTemplate(
        issue: Issue,
        oldTemplate: IssueTemplate,
        newTemplate: IssueTemplate,
        templatedFields: List<JSONFieldInput>,
        type: IssueType?,
        state: IssueState?,
        priority: IssuePriority?,
        assignmentTypeMapping: Map<AssignmentType, AssignmentType?>,
        issueRelationTypeMapping: Map<IssueRelationType, IssueRelationType?>,
        atTime: OffsetDateTime,
        byUser: User,
        updateContext: NodeBatchUpdateContext
    ): TemplateChangedEvent {
        val event = TemplateChangedEvent(atTime, atTime)
        event.oldTemplate().value = oldTemplate
        event.newTemplate().value = newTemplate
        createdTimelineItem(issue, event, atTime, byUser)
        if (!existsNewerTimelineItem<TemplateChangedEvent>(issue, atTime) && issue.template().value != newTemplate) {
            issue.template().value = newTemplate
            var timeOffset = 1L
            updateTemplatedFieldsAfterTemplateUpdate(
                issue, event, templatedFields, atTime.plusNanos(timeOffset++), byUser
            )
            updateIssueFieldsAfterTemplateUpdate(
                issue, event, type, state, priority, atTime.plusNanos(timeOffset++), byUser
            )
            updateAssignmentsAfterTemplateUpdate(
                issue, event, assignmentTypeMapping, atTime.plusNanos(timeOffset++), byUser
            )
            updateIssueRelationsAfterTemplateUpdate(
                issue, event, issueRelationTypeMapping, atTime.plusNanos(timeOffset), byUser
            )
            val aggregationUpdater = IssueAggregationUpdater(updateContext)
            aggregationUpdater.changedIssueStateOrType(issue, state ?: issue.state().value, type ?: issue.type().value)
        }
        return event
    }

    /**
     * Updates types of [Assignment] on [issue] where the old type is incompatible with the new template of [issue]
     * based on [assignmentTypeMapping] and removes types with no replacement.
     * Adds the crated events to `childItems` on [event]
     *
     * @param issue the [Issue] of which the template was updated
     * @param event the event representing the template update
     * @param assignmentTypeMapping used to map old [AssignmentType]s to new [AssignmentType]s
     * @param atTime the point in time when the modification happened, updates [Issue.lastUpdatedAt] if necessary
     * @param byUser the [User] who caused the update, updates [Issue.participants] if necessary
     */
    private suspend fun updateAssignmentsAfterTemplateUpdate(
        issue: Issue,
        event: TemplateChangedEvent,
        assignmentTypeMapping: Map<AssignmentType, AssignmentType?>,
        atTime: OffsetDateTime,
        byUser: User
    ) {
        val newTemplate = issue.template().value
        for (assignment in issue.assignments()) {
            val assignmentType = assignment.type().value
            if (assignmentType != null && assignmentType !in newTemplate.assignmentTypes()) {
                event.childItems() += changeAssignmentType(
                    assignment, assignmentType, assignmentTypeMapping[assignmentType], atTime, byUser
                )
            }
        }
    }

    /**
     * Updates types of [IssueRelation] on [issue] where the old type is incompatible with the new template of [issue]
     * based on [issueRelationTypeMapping] and removes types with no replacement.
     * Adds the crated events to `childItems` on [event]
     *
     * @param issue the [Issue] of which the template was updated
     * @param event the event representing the template update
     * @param issueRelationTypeMapping used to map old [IssueRelationType]s to new [IssueRelationType]s
     * @param atTime the point in time when the modification happened, updates [Issue.lastUpdatedAt] if necessary
     * @param byUser the [User] who caused the update, updates [Issue.participants] if necessary
     */
    private suspend fun updateIssueRelationsAfterTemplateUpdate(
        issue: Issue,
        event: TemplateChangedEvent,
        issueRelationTypeMapping: Map<IssueRelationType, IssueRelationType?>,
        atTime: OffsetDateTime,
        byUser: User
    ) {
        val newTemplate = issue.template().value
        for (issueRelation in issue.outgoingRelations()) {
            val issueRelationType = issueRelation.type().value
            if (issueRelationType != null && issueRelationType !in newTemplate.relationTypes()) {
                event.childItems() += changeIssueRelationType(
                    issueRelation, issueRelationType, issueRelationTypeMapping[issueRelationType], atTime, byUser
                )
            }
        }
    }

    /**
     * Updates `type`, `state`, and `priority` on [issue] after a template update if the old value is incompatible
     * with the new template.
     * Adds the crated events to `childItems` on [event]
     *
     * @param issue the [Issue] of which the template was updated
     * @param event the event representing the template update
     * @param type new [IssueType] in case existing one is incompatible
     * @param state new [IssueState] in case existing one is incompatible
     * @param priority new [IssuePriority] in case existing one is incompatible
     * @param atTime the point in time when the modification happened, updates [Issue.lastUpdatedAt] if necessary
     * @param byUser the [User] who caused the update, updates [Issue.participants] if necessary
     */
    private suspend fun updateIssueFieldsAfterTemplateUpdate(
        issue: Issue,
        event: TemplateChangedEvent,
        type: IssueType?,
        state: IssueState?,
        priority: IssuePriority?,
        atTime: OffsetDateTime,
        byUser: User
    ) {
        val newTemplate = issue.template().value
        if (issue.type().value !in newTemplate.issueTypes()) {
            val existingType = type ?: throw IllegalStateException("Old IssueType not compatible")
            event.childItems() += changeIssueType(
                issue, issue.type().value, existingType, atTime, byUser, null
            )
        }
        if (issue.state().value !in newTemplate.issueStates()) {
            val existingState = state ?: throw IllegalStateException("Old IssueState not compatible")
            event.childItems() += changeIssueState(
                issue, issue.state().value, existingState, atTime, byUser, null
            )
        }
        if (issue.priority().value !in newTemplate.issuePriorities()) {
            event.childItems() += changeIssuePriority(
                issue, issue.priority().value, priority, atTime, byUser, null
            )
        }
    }

    /**
     * Updates templated fields on [issue] after a template update, uses [templatedFields] for new values for currently
     * incompatible fields and removes fields which are no longer defined by the template.
     * Adds the crated events to `childItems` on [event]
     *
     * @param issue the [Issue] of which the template was updated
     * @param event the event representing the template update
     * @param templatedFields new values for templated fields to update currently incompatible fields
     * @param atTime the point in time when the modification happened, updates [Issue.lastUpdatedAt] if necessary
     * @param byUser the [User] who caused the update, updates [Issue.participants] if necessary
     */
    private suspend fun updateTemplatedFieldsAfterTemplateUpdate(
        issue: Issue,
        event: TemplateChangedEvent,
        templatedFields: List<JSONFieldInput>,
        atTime: OffsetDateTime,
        byUser: User
    ) {
        val newTemplate = issue.template().value
        val templatedFieldsByName = templatedFields.associateBy { it.name }
        for (field in newTemplate.templateFieldSpecifications.keys) {
            if (!templatedNodeService.validateTemplatedField(issue, field)) {
                event.childItems() += changeIssueTemplatedField(
                    issue,
                    templatedFieldsByName[field] ?: throw IllegalStateException("No new value for $field provided"),
                    issue.templatedFields[field],
                    atTime,
                    byUser
                )
            }
        }
        issue.templatedFields.keys.filter { it !in newTemplate.templateFieldSpecifications }.forEach {
            event.childItems() += removeTemplatedField(issue, it, atTime, byUser)
        }
    }

    /**
     * Removes a templated field from an [Issue] at [atTime] as [byUser].
     * Should only be used during template update.
     * Does not check the authorization status.
     * Assumes that a templated field with name [field] exists.
     * Does neither save the created [RemovedTemplatedFieldEvent] or the [issue].
     * Requires to save the returned [RemovedTemplatedFieldEvent] or the [issue].
     *
     * @param issue the [Issue] from which the templated field is removed
     * @param field the name of the templated field to remove
     * @param atTime the point in time when the modification happened, updates [Issue.lastUpdatedAt] if necessary
     * @param byUser the [User] who caused the update, updates [Issue.participants] if necessary
     * @return the created [RemovedTemplatedFieldEvent]
     */
    private suspend fun removeTemplatedField(
        issue: Issue, field: String, atTime: OffsetDateTime, byUser: User
    ): RemovedTemplatedFieldEvent {
        val event = RemovedTemplatedFieldEvent(atTime, atTime, field, issue.templatedFields[field]!!)
        createdTimelineItem(issue, event, atTime, byUser)
        issue.templatedFields.remove(field)
        return event
    }

    /**
     * Deletes an [Issue].
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [Issue] to delete
     */
    suspend fun deleteIssue(authorizationContext: GropiusAuthorizationContext, input: DeleteNodeInput) {
        val issue = repository.findById(input.id)
        for (trackable in issue.trackables()) {
            checkPermission(
                issue,
                Permission(TrackablePermission.MODERATOR, authorizationContext),
                "delete Issues on a Trackable the Issue is on"
            )
        }
        val aggregationUpdater = IssueAggregationUpdater()
        aggregationUpdater.deletedIssue(issue)
        aggregationUpdater.deletedNodes += prepareIssueDeletion(issue)
        aggregationUpdater.save(nodeRepository)
    }

    /**
     * Gets all nodes to delete when this [Issue] is deleted, including the [Issue] itself.
     * Also performs other updates necessary when deleting an [Issue].
     *
     * @param node the [Issue] to delete
     * @return all nodes to delete when this [Issue] is deleted, including the [Issue] itself
     */
    suspend fun prepareIssueDeletion(node: Issue): Collection<Node> {
        val issuesToSave = mutableSetOf<Issue>()
        node.outgoingRelations().forEach {
            val issue = it.relatedIssue().value!!
            issue.incomingRelations().remove(it)
            issuesToSave += issue
        }
        node.incomingRelations().forEach {
            val issue = it.issue().value
            issue.outgoingRelations().remove(it)
            issuesToSave += issue
        }
        repository.saveAll(issuesToSave).collectList().awaitSingle()
        return node.timelineItems() + node
    }

    /**
     * Adds an [Issue] to a [Trackable], returns the created [AddedToTrackableEvent],
     * or `null` if the [Issue] was already on the [Trackable].
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [Issue] to add to which [Trackable]
     * @return the saved created [AddedToTrackableEvent] or `null` if no event was created
     */
    suspend fun addIssueToTrackable(
        authorizationContext: GropiusAuthorizationContext, input: AddIssueToTrackableInput
    ): AddedToTrackableEvent? {
        input.validate()
        val issue = repository.findById(input.issue)
        val trackable = trackableRepository.findById(input.trackable)
        checkManageIssuesPermission(trackable, authorizationContext)
        checkManageIssuesPermission(issue, authorizationContext)
        return if (trackable !in issue.trackables()) {
            val updateContext = NodeBatchUpdateContext()
            val event = addIssueToTrackable(
                issue, trackable, OffsetDateTime.now(), getUser(authorizationContext), updateContext
            )
            updateContext.save(event, nodeRepository)
        } else {
            null
        }
    }

    /**
     * Adds an [issue] to a [trackable] at [atTime] as [byUser] and adds a [AddedToTrackableEvent] to the timeline.
     * Creates the event even if the [issue] was already on the [trackable].
     * Only adds the [issue] to the [trackable] if no newer timeline item exists which removes it again.
     * Does not check the authorization status.
     * Does neither save the created [AddedToTrackableEvent] nor the [issue].
     * It is necessary to save the [issue] or returned [AddedToTrackableEvent] afterwards.
     *
     * @param issue the [Issue] to add to [trackable]
     * @param trackable the [Trackable] where the [issue] should be added
     * @param atTime the point in time when the modification happened, updates [Issue.lastUpdatedAt] if necessary
     * @param byUser the [User] who caused the update, updates [Issue.participants] if necessary
     * @param updateContext context used for the aggregation updater
     * @returns the created [AddedToTrackableEvent]
     */
    suspend fun addIssueToTrackable(
        issue: Issue, trackable: Trackable, atTime: OffsetDateTime, byUser: User, updateContext: NodeBatchUpdateContext
    ): AddedToTrackableEvent {
        val event = AddedToTrackableEvent(atTime, atTime)
        event.addedToTrackable().value = trackable
        createdTimelineItem(issue, event, atTime, byUser)
        if (!existsNewerTimelineItem<RemovedFromTrackableEvent>(
                issue, atTime
            ) { it.removedFromTrackable().value == trackable }
        ) {
            issue.trackables() += trackable
            val aggregationUpdater = IssueAggregationUpdater(updateContext)
            aggregationUpdater.addedIssueToTrackable(issue, trackable)
        }
        return event
    }

    /**
     * Removes an [Issue] from a [Trackable], returns the created [RemovedFromPinnedIssuesEvent],
     * or `null` if the [Issue] was not pinned on the [Trackable].
     * Also removes [Label]s and [Artefact]s if necessary, and unpins it on the specified [Trackable].
     * Checks the authorization status, checks that the [Issue] remains on at least one [Trackable].
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [Issue] to remove from which [Trackable]
     * @return the saved created [RemovedFromTrackableEvent] or `null` if no event was created
     */
    suspend fun removeIssueFromTrackable(
        authorizationContext: GropiusAuthorizationContext, input: RemoveIssueFromTrackableInput
    ): RemovedFromTrackableEvent? {
        input.validate()
        val issue = repository.findById(input.issue)
        val trackable = trackableRepository.findById(input.trackable)
        checkManageIssuesPermission(trackable, authorizationContext)
        return if (trackable in issue.trackables()) {
            val updateContext = NodeBatchUpdateContext()
            val event = removeIssueFromTrackable(
                issue, trackable, OffsetDateTime.now(), getUser(authorizationContext), updateContext
            )
            updateContext.save(event, nodeRepository)
        } else {
            null
        }
    }

    /**
     * Removes an [issue] from a [trackable] at [atTime] as [byUser] and adds a [RemovedFromTrackableEvent]
     * to the timeline.
     * Also removes [Label]s and [Artefact]s if necessary, and unpins it on the specified [Trackable].
     * Creates the event even if the [issue] was not on the [trackable].
     * Only removes the [issue] from the [trackable] if no newer timeline item exists which adds it again.
     * Does not check the authorization status.
     * Checks that the [issue] remains on at least one [Trackable].
     * Does neither save the created [RemovedFromTrackableEvent] nor the [issue].
     * It is necessary to save the [issue] or returned [RemovedFromTrackableEvent] afterwards.
     *
     * @param issue the [Issue] to remove from [trackable]
     * @param trackable the [Trackable] where [issue] should be removed
     * @param atTime the point in time when the modification happened, updates [Issue.lastUpdatedAt] if necessary
     * @param byUser the [User] who caused the update, updates [Issue.participants] if necessary
     * @param updateContext context used for the aggregation updater
     * @returns the created [RemovedFromTrackableEvent]
     */
    suspend fun removeIssueFromTrackable(
        issue: Issue, trackable: Trackable, atTime: OffsetDateTime, byUser: User, updateContext: NodeBatchUpdateContext
    ): RemovedFromTrackableEvent {
        val event = RemovedFromTrackableEvent(atTime, atTime)
        event.removedFromTrackable().value = trackable
        createdTimelineItem(issue, event, atTime, byUser)
        if (!existsNewerTimelineItem<AddedToTrackableEvent>(
                issue, atTime
            ) { it.addedToTrackable().value == trackable }
        ) {
            issue.trackables() -= trackable
            if (issue.trackables().isEmpty()) {
                throw IllegalStateException("An Issue must remain on at least Trackable")
            }
            var timeOffset = 1L
            if (trackable in issue.pinnedOn()) {
                event.childItems() += removeIssueFromPinnedIssues(
                    issue, trackable, atTime.plusNanos(timeOffset++), byUser
                )
            }
            event.childItems() += issue.artefacts().filter { it.trackable().value == trackable }
                .map { removeArtefactFromIssue(issue, it, atTime.plusNanos(timeOffset++), byUser) }
            event.childItems() += issue.labels().filter { Collections.disjoint(issue.trackables(), it.trackables()) }
                .map { removeLabelFromIssue(issue, it, atTime.plusNanos(timeOffset++), byUser) }
            val aggregationUpdater = IssueAggregationUpdater(updateContext)
            aggregationUpdater.removedIssueFromTrackable(issue, trackable)
        }
        return event
    }

    /**
     * Pins an [Issue] on a [Trackable], returns the created [AddedToPinnedIssuesEvent],
     * or `null` if the [Issue] was already pinned on the [Trackable].
     * Checks the authorization status, checks that the [Issue] can be pinned on the [Trackable]
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [Issue] to pin on which [Trackable]
     * @return the saved created [AddedToPinnedIssuesEvent] or `null` if no event was created
     */
    suspend fun addIssueToPinnedIssues(
        authorizationContext: GropiusAuthorizationContext, input: AddIssueToPinnedIssuesInput
    ): AddedToPinnedIssuesEvent? {
        input.validate()
        val issue = repository.findById(input.issue)
        val trackable = trackableRepository.findById(input.trackable)
        checkManageIssuesPermission(trackable, authorizationContext)
        return if (trackable !in issue.pinnedOn()) {
            timelineItemRepository.save(
                addIssueToPinnedIssues(issue, trackable, OffsetDateTime.now(), getUser(authorizationContext))
            ).awaitSingle()
        } else {
            null
        }
    }

    /**
     * Pins an [issue] on [trackable] at [atTime] as [byUser] and adds a [AddedToPinnedIssuesEvent]
     * to the timeline.
     * Creates the event even if the [issue] was already pinned on the [trackable].
     * Only adds the [issue] to the `pinnedIssues` on [trackable] if no newer timeline item exists which removes
     * it again.
     * Does not check the authorization status.
     * Checks if the [issue] can be pinned to this [trackable].
     * Does neither save the created [AddedToPinnedIssuesEvent] nor the [issue].
     * It is necessary to save the [issue] or returned [AddedToPinnedIssuesEvent] afterwards.
     *
     * @param issue the [Issue] to pin
     * @param trackable the [Trackable] where the [issue] should be pinned
     * @param atTime the point in time when the modification happened, updates [Issue.lastUpdatedAt] if necessary
     * @param byUser the [User] who caused the update, updates [Issue.participants] if necessary
     * @returns the created [AddedToPinnedIssuesEvent]
     * @throws IllegalArgumentException if [issue] cannot be pinned on [trackable]
     */
    suspend fun addIssueToPinnedIssues(
        issue: Issue, trackable: Trackable, atTime: OffsetDateTime, byUser: User
    ): AddedToPinnedIssuesEvent {
        if (trackable !in issue.trackables()) {
            throw IllegalArgumentException("The Issue cannot be pinned on the Trackable as it is not on the Trackable")
        }
        val event = AddedToPinnedIssuesEvent(atTime, atTime)
        event.pinnedOn().value = trackable
        createdTimelineItem(issue, event, atTime, byUser)
        if (!existsNewerTimelineItem<RemovedFromPinnedIssuesEvent>(
                issue, atTime
            ) { it.unpinnedOn().value == trackable }
        ) {
            issue.pinnedOn() += trackable
        }
        return event
    }

    /**
     * Unpins an [Issue] on a [Trackable], returns the created [RemovedFromPinnedIssuesEvent],
     * or `null` if the [Issue] was not pinned on the [Trackable].
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [Issue] to unpin on which [Trackable]
     * @return the saved created [RemovedFromPinnedIssuesEvent] or `null` if no event was created
     */
    suspend fun removeIssueFromPinnedIssues(
        authorizationContext: GropiusAuthorizationContext, input: RemoveIssueFromPinnedIssuesInput
    ): RemovedFromPinnedIssuesEvent? {
        input.validate()
        val issue = repository.findById(input.issue)
        val trackable = trackableRepository.findById(input.trackable)
        checkManageIssuesPermission(trackable, authorizationContext)
        return if (trackable in issue.pinnedOn()) {
            timelineItemRepository.save(
                removeIssueFromPinnedIssues(issue, trackable, OffsetDateTime.now(), getUser(authorizationContext))
            ).awaitSingle()
        } else {
            null
        }
    }

    /**
     * Unpins an [issue] on a [trackable] at [atTime] as [byUser] and adds a [RemovedFromPinnedIssuesEvent]
     * to the timeline.
     * Creates the event even if the [issue] was not pinned on the [trackable].
     * Only removes the [issue] from the `pinnedIssues` on [trackable] if no newer timeline item exists which
     * adds it again.
     * Does not check the authorization status.
     * Does neither save the created [RemovedFromPinnedIssuesEvent] nor the [issue].
     * It is necessary to save the [issue] or returned [RemovedFromPinnedIssuesEvent] afterwards.
     *
     * @param issue the [Issue] to unpin
     * @param trackable the [Trackable] where [issue] should be unpinned
     * @param atTime the point in time when the modification happened, updates [Issue.lastUpdatedAt] if necessary
     * @param byUser the [User] who caused the update, updates [Issue.participants] if necessary
     * @returns the created [RemovedFromPinnedIssuesEvent]
     */
    suspend fun removeIssueFromPinnedIssues(
        issue: Issue, trackable: Trackable, atTime: OffsetDateTime, byUser: User
    ): RemovedFromPinnedIssuesEvent {
        val event = RemovedFromPinnedIssuesEvent(atTime, atTime)
        event.unpinnedOn().value = trackable
        createdTimelineItem(issue, event, atTime, byUser)
        if (!existsNewerTimelineItem<AddedToPinnedIssuesEvent>(issue, atTime) { it.pinnedOn().value == trackable }) {
            issue.pinnedOn() -= trackable
        }
        return event
    }

    /**
     * Adds a [Label] to an [Issue], returns the created [AddedLabelEvent], or `null` if the [Label] was already
     * present on the [Issue].
     * Checks the authorization status, checks that the [Label] can be added to the [Issue]
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [Label] to add to which [Issue]
     * @return the saved created [AddedLabelEvent] or `null` if no event was created
     */
    suspend fun addLabelToIssue(
        authorizationContext: GropiusAuthorizationContext, input: AddLabelToIssueInput
    ): AddedLabelEvent? {
        input.validate()
        val issue = repository.findById(input.issue)
        val label = labelRepository.findById(input.label)
        checkManageIssuesPermission(issue, authorizationContext)
        checkPermission(label, Permission(NodePermission.READ, authorizationContext), "use the Label")
        return if (label !in issue.labels()) {
            timelineItemRepository.save(
                addLabelToIssue(issue, label, OffsetDateTime.now(), getUser(authorizationContext))
            ).awaitSingle()
        } else {
            null
        }
    }

    /**
     * Adds a [label] to an [issue] at [atTime] as [byUser] and adds a [AddedLabelEvent] to the timeline.
     * Creates the event even if the [label] was already on the [issue].
     * Only adds the [label] to the [issue] if no newer timeline item exists which removes it again.
     * Does not check the authorization status.
     * Checks if the [label] can be added to this [issue].
     * Does neither save the created [AddedLabelEvent] nor the [issue].
     * It is necessary to save the [issue] or returned [AddedLabelEvent] afterwards.
     *
     * @param issue the [Issue] where [label] is added to
     * @param label the [Label] to add
     * @param atTime the point in time when the modification happened, updates [Issue.lastUpdatedAt] if necessary
     * @param byUser the [User] who caused the update, updates [Issue.participants] if necessary
     * @returns the created [AddedLabelEvent]
     * @throws IllegalArgumentException if [label] cannot be added to [issue]
     */
    suspend fun addLabelToIssue(
        issue: Issue, label: Label, atTime: OffsetDateTime, byUser: User
    ): AddedLabelEvent {
        if (Collections.disjoint(issue.trackables(), label.trackables())) {
            throw IllegalArgumentException("The Label cannot be added to the Issue as no common Trackable exists")
        }
        val event = AddedLabelEvent(atTime, atTime)
        event.addedLabel().value = label
        createdTimelineItem(issue, event, atTime, byUser)
        if (!existsNewerTimelineItem<RemovedLabelEvent>(issue, atTime) { it.removedLabel().value == label }) {
            issue.labels() += label
        }
        return event
    }

    /**
     * Removes a [Label] from an [Issue], returns the created [RemovedLabelEvent], or `null` if the [Label] was not
     * present on the [Issue].
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [Label] to remove from which [Issue]
     * @return the saved created [RemovedLabelEvent] or `null` if no event was created
     */
    suspend fun removeLabelFromIssue(
        authorizationContext: GropiusAuthorizationContext, input: RemoveLabelFromIssueInput
    ): RemovedLabelEvent? {
        input.validate()
        val issue = repository.findById(input.issue)
        val label = labelRepository.findById(input.label)
        checkManageIssuesPermission(issue, authorizationContext)
        return if (label in issue.labels()) {
            timelineItemRepository.save(
                removeLabelFromIssue(issue, label, OffsetDateTime.now(), getUser(authorizationContext))
            ).awaitSingle()
        } else {
            null
        }
    }

    /**
     * Removes a [label] from an [issue] at [atTime] as [byUser] and adds a [RemovedLabelEvent] to the timeline.
     * Creates the event even if the [label] was not on the [issue].
     * Only removes the [label] from the [issue] if no newer timeline item exists which adds it again.
     * Does not check the authorization status.
     * Does neither save the created [RemovedLabelEvent] nor the [issue].
     * It is necessary to save the [issue] or returned [RemovedLabelEvent] afterwards.
     *
     * @param issue the [Issue] where [label] is removed from
     * @param label the [Label] to remove
     * @param atTime the point in time when the modification happened, updates [Issue.lastUpdatedAt] if necessary
     * @param byUser the [User] who caused the update, updates [Issue.participants] if necessary
     * @returns the created [RemovedLabelEvent]
     */
    suspend fun removeLabelFromIssue(
        issue: Issue, label: Label, atTime: OffsetDateTime, byUser: User
    ): RemovedLabelEvent {
        val event = RemovedLabelEvent(atTime, atTime)
        event.removedLabel().value = label
        createdTimelineItem(issue, event, atTime, byUser)
        if (!existsNewerTimelineItem<AddedLabelEvent>(issue, atTime) { it.addedLabel().value == label }) {
            issue.labels() -= label
        }
        return event
    }

    /**
     * Adds a [Artefact] to an [Issue], returns the created [AddedArtefactEvent], or `null` if the [Artefact] was already
     * present on the [Issue].
     * Checks the authorization status, checks that the [Artefact] can be added to the [Issue]
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [Artefact] to add to which [Issue]
     * @return the saved created [AddedArtefactEvent] or `null` if no event was created
     */
    suspend fun addArtefactToIssue(
        authorizationContext: GropiusAuthorizationContext, input: AddArtefactToIssueInput
    ): AddedArtefactEvent? {
        input.validate()
        val issue = repository.findById(input.issue)
        val artefact = artefactRepository.findById(input.artefact)
        checkManageIssuesPermission(issue, authorizationContext)
        checkPermission(artefact, Permission(NodePermission.READ, authorizationContext), "use the Artefact")
        return if (artefact !in issue.artefacts()) {
            timelineItemRepository.save(
                addArtefactToIssue(issue, artefact, OffsetDateTime.now(), getUser(authorizationContext))
            ).awaitSingle()
        } else {
            null
        }
    }

    /**
     * Adds a [artefact] to an [issue] at [atTime] as [byUser] and adds a [AddedArtefactEvent] to the timeline.
     * Creates the event even if the [artefact] was already on the [issue].
     * Only adds the [artefact] to the [issue] if no newer timeline item exists which removes it again.
     * Does not check the authorization status.
     * Check if the [artefact] can be added to this [issue].
     * Does neither save the created [AddedArtefactEvent] nor the [issue].
     * It is necessary to save the [issue] or returned [AddedArtefactEvent] afterwards.
     *
     * @param issue the [Issue] where [artefact] is added to
     * @param artefact the [Artefact] to add
     * @param atTime the point in time when the modification happened, updates [Issue.lastUpdatedAt] if necessary
     * @param byUser the [User] who caused the update, updates [Issue.participants] if necessary
     * @return the crated [AddedArtefactEvent]
     * @throws IllegalArgumentException if the [artefact] cannot be added to the [issue]
     */
    suspend fun addArtefactToIssue(
        issue: Issue, artefact: Artefact, atTime: OffsetDateTime, byUser: User
    ): AddedArtefactEvent {
        if (artefact.trackable().value !in issue.trackables()) {
            throw IllegalArgumentException("The Artefact is not part of a Trackable the Issue is on")
        }
        val event = AddedArtefactEvent(atTime, atTime)
        event.addedArtefact().value = artefact
        createdTimelineItem(issue, event, atTime, byUser)
        if (!existsNewerTimelineItem<RemovedArtefactEvent>(issue, atTime) { it.removedArtefact().value == artefact }) {
            issue.artefacts() += artefact
        }
        return event
    }

    /**
     * Removes a [Artefact] from an [Issue], returns the created [RemovedArtefactEvent], or `null` if the [Artefact] was not
     * present on the [Issue].
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [Artefact] to remove from which [Issue]
     * @return the saved created [RemovedArtefactEvent] or `null` if no event was created
     */
    suspend fun removeArtefactFromIssue(
        authorizationContext: GropiusAuthorizationContext, input: RemoveArtefactFromIssueInput
    ): RemovedArtefactEvent? {
        input.validate()
        val issue = repository.findById(input.issue)
        val artefact = artefactRepository.findById(input.artefact)
        checkManageIssuesPermission(issue, authorizationContext)
        return if (artefact in issue.artefacts()) {
            timelineItemRepository.save(
                removeArtefactFromIssue(issue, artefact, OffsetDateTime.now(), getUser(authorizationContext))
            ).awaitSingle()
        } else {
            null
        }
    }

    /**
     * Removes a [artefact] from an [issue] at [atTime] as [byUser] and adds a [RemovedArtefactEvent] to the timeline.
     * Creates the event even if the [artefact] was not on the [issue].
     * Only removes the [artefact] from the [issue] if no newer timeline item exists which adds it again.
     * Does not check the authorization status.
     * Does neither save the created [RemovedArtefactEvent] nor the [issue].
     * It is necessary to save the [issue] or returned [RemovedArtefactEvent] afterwards.
     *
     * @param issue the [Issue] where [artefact] is removed from
     * @param artefact the [Artefact] to remove
     * @param atTime the point in time when the modification happened, updates [Issue.lastUpdatedAt] if necessary
     * @param byUser the [User] who caused the update, updates [Issue.participants] if necessary
     * @returns the created [RemovedArtefactEvent]
     */
    suspend fun removeArtefactFromIssue(
        issue: Issue, artefact: Artefact, atTime: OffsetDateTime, byUser: User
    ): RemovedArtefactEvent {
        val event = RemovedArtefactEvent(atTime, atTime)
        event.removedArtefact().value = artefact
        createdTimelineItem(issue, event, atTime, byUser)
        if (!existsNewerTimelineItem<AddedArtefactEvent>(issue, atTime) { it.addedArtefact().value == artefact }) {
            issue.artefacts() -= artefact
        }
        return event
    }

    /**
     * Changes the `title` of an [Issue], returns the created [TitleChangedEvent] or null if the `title`
     * was not changed.
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines the new `title` and of which issue to change it
     * @return the saved created [TitleChangedEvent] or `null` if no event was created
     */
    suspend fun changeIssueTitle(
        authorizationContext: GropiusAuthorizationContext, input: ChangeIssueTitleInput
    ): TitleChangedEvent? {
        input.validate()
        val issue = repository.findById(input.issue)
        return changeIssueProperty(authorizationContext, issue, issue.title, input.title, ::changeIssueTitle)
    }

    /**
     * Changes the `title` of an [issue] at [atTime] as [byUser] and adds a [TitleChangedEvent] to the timeline.
     * Creates the event even if the `title` was not changed.
     * Only changes the `title` if no newer timeline item exists which changes it.
     * Does not check the authorization status.
     * Does neither save the created [TitleChangedEvent] nor the [issue].
     * It is necessary to save the [issue] or returned [TitleChangedEvent] afterwards.
     *
     * @param issue the [Issue] where the `title` should be changed
     * @param oldTitle the old `title`
     * @param newTitle the new `title`
     * @param atTime the point in time when the modification happened, updates [Issue.lastUpdatedAt] if necessary
     * @param byUser the [User] who caused the update, updates [Issue.participants] if necessary
     * @param updateContext currently unused
     * @returns the created [TitleChangedEvent]
     */
    suspend fun changeIssueTitle(
        issue: Issue,
        oldTitle: String,
        newTitle: String,
        atTime: OffsetDateTime,
        byUser: User,
        updateContext: NodeBatchUpdateContext
    ): TitleChangedEvent {
        val event = TitleChangedEvent(atTime, atTime, oldTitle, newTitle)
        changeIssueProperty(issue, newTitle, atTime, byUser, issue::title, event)
        return event
    }

    /**
     * Changes the `priority` of an [Issue], returns the created [PriorityChangedEvent] or null if the `priority`
     * was not changed.
     * Checks the authorization status, and check that the new [IssuePriority] can be used on the [Issue]
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines the new `priority` and of which issue to change it
     * @return the saved created [PriorityChangedEvent] or `null` if no event was created
     */
    suspend fun changeIssuePriority(
        authorizationContext: GropiusAuthorizationContext, input: ChangeIssuePriorityInput
    ): PriorityChangedEvent? {
        input.validate()
        val issue = repository.findById(input.issue)
        val priority = input.priority?.let { issuePriorityRepository.findById(it) }
        return changeIssueProperty(authorizationContext, issue, issue.priority().value, priority, ::changeIssuePriority)
    }

    /**
     * Changes the `priority` of an [issue] at [atTime] as [byUser] and adds a [PriorityChangedEvent] to the timeline.
     * Creates the event even if the `priority` was not changed.
     * Only changes the `priority` if no newer timeline item exists which changes it.
     * Does not check the authorization status.
     * Checks that the [newPriority] can be used on the [issue].
     * Does neither save the created [PriorityChangedEvent] nor the [issue].
     * It is necessary to save the [issue] or returned [PriorityChangedEvent] afterwards.
     *
     * @param issue the [Issue] where the `priority` should be changed
     * @param oldPriority the old `priority`
     * @param newPriority the new `priority`
     * @param atTime the point in time when the modification happened, updates [Issue.lastUpdatedAt] if necessary
     * @param byUser the [User] who caused the update, updates [Issue.participants] if necessary
     * @param updateContext currently unused
     * @returns the created [PriorityChangedEvent]
     */
    suspend fun changeIssuePriority(
        issue: Issue,
        oldPriority: IssuePriority?,
        newPriority: IssuePriority?,
        atTime: OffsetDateTime,
        byUser: User,
        updateContext: NodeBatchUpdateContext?
    ): PriorityChangedEvent {
        if (newPriority != null) {
            checkIssuePriorityCompatibility(issue, newPriority)
        }
        val event = PriorityChangedEvent(atTime, atTime)
        event.newPriority().value = newPriority
        event.oldPriority().value = oldPriority
        changeIssueProperty(issue, newPriority, atTime, byUser, issue.priority()::value, event)
        return event
    }

    /**
     * Checks that the `priority` of an [issue] can be changed to [newPriority]
     *
     * @param issue the [Issue] to check compatibility with, must have a set template
     * @param newPriority the new `priority` of the [issue]
     * @throws IllegalArgumentException if the [newPriority] is not compatible with the template of the [issue]
     */
    private suspend fun checkIssuePriorityCompatibility(issue: Issue, newPriority: IssuePriority) {
        if (issue.template().value !in newPriority.partOf()) {
            throw IllegalArgumentException(
                "IssuePriority cannot be used on the Issue as it is not provided by the template of the Issue"
            )
        }
    }

    /**
     * Changes the `state` of an [Issue], returns the created [StateChangedEvent] or null if the `state`
     * was not changed.
     * Checks the authorization status, and checks that the new [IssueState] can be used on the [Issue]
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines the new `state` and of which issue to change it
     * @return the saved created [StateChangedEvent] or `null` if no event was created
     */
    suspend fun changeIssueState(
        authorizationContext: GropiusAuthorizationContext, input: ChangeIssueStateInput
    ): StateChangedEvent? {
        input.validate()
        val issue = repository.findById(input.issue)
        val state = issueStateRepository.findById(input.state)
        return changeIssueProperty(authorizationContext, issue, issue.state().value, state, ::changeIssueState)
    }

    /**
     * Changes the `state` of an [issue] at [atTime] as [byUser] and adds a [StateChangedEvent] to the timeline.
     * Creates the event even if the `state` was not changed.
     * Only changes the `state` if no newer timeline item exists which changes it.
     * Does not check the authorization status.
     * Checks that the [newState] can be used with the [issue].
     * Does neither save the created [StateChangedEvent] nor the [issue].
     * It is necessary to save the [issue] or returned [StateChangedEvent] afterwards.
     *
     * @param issue the [Issue] where the `state` should be changed
     * @param oldState the old `state`
     * @param newState the new `state`
     * @param atTime the point in time when the modification happened, updates [Issue.lastUpdatedAt] if necessary
     * @param byUser the [User] who caused the update, updates [Issue.participants] if necessary
     * @param updateContext context used for the aggregation updater (if not provided, no aggregation is done)
     * @returns the created [StateChangedEvent]
     */
    suspend fun changeIssueState(
        issue: Issue,
        oldState: IssueState,
        newState: IssueState,
        atTime: OffsetDateTime,
        byUser: User,
        updateContext: NodeBatchUpdateContext?
    ): StateChangedEvent {
        checkIssueStateCompatibility(issue, newState)
        val event = StateChangedEvent(atTime, atTime)
        event.newState().value = newState
        event.oldState().value = oldState
        changeIssueProperty(issue, newState, atTime, byUser, issue.state()::value, event)
        if (updateContext != null) {
            val aggregationUpdater = IssueAggregationUpdater(updateContext)
            aggregationUpdater.changedIssueStateOrType(issue, oldState, issue.type().value)
        }
        return event
    }

    /**
     * Checks that the `state` of an [issue] can be changed to [newState]
     *
     * @param issue the [Issue] to check compatibility with, must have a set template
     * @param newState the new `state` of the [issue]
     * @throws IllegalArgumentException if the [newState] is not compatible with the template of the [issue]
     */
    private suspend fun checkIssueStateCompatibility(issue: Issue, newState: IssueState) {
        if (issue.template().value !in newState.partOf()) {
            throw IllegalArgumentException(
                "IssueState cannot be used on the Issue as it is not provided by the template of the Issue"
            )
        }
    }

    /**
     * Changes the `type` of an [Issue], returns the created [TypeChangedEvent] or null if the `type`
     * was not changed.
     * Checks the authorization status, and checks that the new [IssueType] can be used with the [Issue]
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines the new `type` and of which [Issue] to change it
     * @return the saved created [TypeChangedEvent] or `null` if no event was created
     */
    suspend fun changeIssueType(
        authorizationContext: GropiusAuthorizationContext, input: ChangeIssueTypeInput
    ): TypeChangedEvent? {
        input.validate()
        val issue = repository.findById(input.issue)
        val type = issueTypeRepository.findById(input.type)
        return changeIssueProperty(authorizationContext, issue, issue.type().value, type, ::changeIssueType)
    }

    /**
     * Changes the `type` of an [issue] at [atTime] as [byUser] and adds a [TypeChangedEvent] to the timeline.
     * Creates the event even if the `type` was not changed.
     * Only changes the `type` if no newer timeline item exists which changes it.
     * Does not check the authorization status.
     * Checks that the [newType] can be used with the [issue].
     * Does neither save the created [TypeChangedEvent] nor the [issue].
     * It is necessary to save the [issue] or returned [TypeChangedEvent] afterwards.
     *
     * @param issue the [Issue] where the `type` should be changed
     * @param oldType the old `type`
     * @param newType the new `type`
     * @param atTime the point in time when the modification happened, updates [Issue.lastUpdatedAt] if necessary
     * @param byUser the [User] who caused the update, updates [Issue.participants] if necessary
     * @param updateContext context used for the aggregation updater (if not provided, no aggregation is done)
     * @returns the created [TypeChangedEvent]
     */
    suspend fun changeIssueType(
        issue: Issue,
        oldType: IssueType,
        newType: IssueType,
        atTime: OffsetDateTime,
        byUser: User,
        updateContext: NodeBatchUpdateContext?
    ): TypeChangedEvent {
        checkIssueTypeCompatibility(issue, newType)
        val event = TypeChangedEvent(atTime, atTime)
        event.newType().value = newType
        event.oldType().value = oldType
        changeIssueProperty(issue, newType, atTime, byUser, issue.type()::value, event)
        if (updateContext != null) {
            val aggregationUpdater = IssueAggregationUpdater(updateContext)
            aggregationUpdater.changedIssueStateOrType(issue, issue.state().value, oldType)
        }
        return event
    }

    /**
     * Checks that the `type` of an [issue] can be changed to [newType]
     *
     * @param issue the [Issue] to check compatibility with, must have a set template
     * @param newType the new `type` of the [issue]
     * @throws IllegalArgumentException if the [newType] is not compatible with the template of the [issue]
     */
    private suspend fun checkIssueTypeCompatibility(issue: Issue, newType: IssueType) {
        if (issue.template().value !in newType.partOf()) {
            throw IllegalArgumentException(
                "IssueType cannot be used on the Issue as it is not provided by the template of the Issue"
            )
        }
    }

    /**
     * Adds an [AffectedByIssue] to an [Issue], returns the created [AddedAffectedEntityEvent],
     * or `null` if the [AffectedByIssue] was already on the [Issue].
     * Checks the authorization status.
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [AffectedByIssue] to add to which [Issue]
     * @return the saved created [AddedAffectedEntityEvent] or `null` if no event was created
     */
    suspend fun addAffectedEntityToIssue(
        authorizationContext: GropiusAuthorizationContext, input: AddAffectedEntityToIssueInput
    ): AddedAffectedEntityEvent? {
        input.validate()
        val issue = repository.findById(input.issue)
        checkManageIssuesPermission(issue, authorizationContext)
        val affectedEntity = affectedByIssueRepository.findById(input.affectedEntity)
        checkPermission(
            affectedEntity,
            Permission(TrackablePermission.AFFECT_ENTITIES_WITH_ISSUES, authorizationContext),
            "affect the entity with Issues"
        )
        return if (affectedEntity !in issue.affects()) {
            val updateContext = NodeBatchUpdateContext()
            val event = addAffectedEntityToIssue(
                issue, affectedEntity, OffsetDateTime.now(), getUser(authorizationContext), updateContext
            )
            updateContext.save(event, nodeRepository)
        } else {
            null
        }
    }

    /**
     * Adds an [affectedEntity] to an [issue] at [atTime] as [byUser] and adds a [AddedAffectedEntityEvent]
     * to the timeline.
     * Creates the event even if the [affectedEntity] was already affected by the [issue].
     * Only adds the [affectedEntity] to the `affects` on the [issue] if no newer timeline item exists which removes
     * it again.
     * Does not check the authorization status.
     * Does neither save the created [AddedToPinnedIssuesEvent] nor the [issue].
     * It is necessary to save the [issue] or returned [AddedAffectedEntityEvent] afterwards.
     *
     * @param issue the [Issue] which should affect [affectedEntity]
     * @param affectedEntity the [AffectedByIssue] which should be affected by the [issue]
     * @param atTime the point in time when the modification happened, updates [Issue.lastUpdatedAt] if necessary
     * @param byUser the [User] who caused the update, updates [Issue.participants] if necessary
     * @param updateContext context used for the aggregation updater
     * @returns the created [AddedAffectedEntityEvent]
     * @throws IllegalArgumentException if [issue] cannot affect the [affectedEntity]
     */
    suspend fun addAffectedEntityToIssue(
        issue: Issue,
        affectedEntity: AffectedByIssue,
        atTime: OffsetDateTime,
        byUser: User,
        updateContext: NodeBatchUpdateContext
    ): AddedAffectedEntityEvent {
        val event = AddedAffectedEntityEvent(atTime, atTime)
        event.addedAffectedEntity().value = affectedEntity
        createdTimelineItem(issue, event, atTime, byUser)
        if (!existsNewerTimelineItem<RemovedAffectedEntityEvent>(
                issue, atTime
            ) { it.removedAffectedEntity().value == affectedEntity }
        ) {
            issue.affects() += affectedEntity
            val aggregationUpdater = IssueAggregationUpdater(updateContext)
            aggregationUpdater.addedAffectedEntity(issue, affectedEntity)
        }
        return event
    }

    /**
     * Removes an [AffectedByIssue] from an [Issue], returns the created [RemovedAffectedEntityEvent],
     * or `null` if the [AffectedByIssue] was not affected by the [Issue].
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [AffectedByIssue] to remove from which [Issue]
     * @return the saved created [RemovedAffectedEntityEvent] or `null` if no event was created
     */
    suspend fun removeAffectedEntityFromIssue(
        authorizationContext: GropiusAuthorizationContext, input: RemoveAffectedEntityFromIssueInput
    ): RemovedAffectedEntityEvent? {
        input.validate()
        val issue = repository.findById(input.issue)
        val affectedEntity = affectedByIssueRepository.findById(input.affectedEntity)
        if (!(evaluatePermission(
                issue, Permission(TrackablePermission.MANAGE_ISSUES, authorizationContext)
            ) || evaluatePermission(
                affectedEntity, Permission(TrackablePermission.AFFECT_ENTITIES_WITH_ISSUES, authorizationContext)
            ))
        ) {
            throw IllegalArgumentException(
                "User has neither the permission to manage the Issue nor to affect the entity with issues"
            )
        }
        return if (affectedEntity in issue.affects()) {
            val updateContext = NodeBatchUpdateContext()
            val event = removeAffectedEntityFromIssue(
                issue, affectedEntity, OffsetDateTime.now(), getUser(authorizationContext), updateContext
            )
            updateContext.save(event, nodeRepository)
        } else {
            null
        }
    }

    /**
     * Removes an [affectedEntity] from an [issue] at [atTime] as [byUser] and adds a [RemovedAffectedEntityEvent]
     * to the timeline.
     * Creates the event even if the [affectedEntity] was not affected by the [issue].
     * Only removes the [affectedEntity] from the `affects` on the [issue] if no newer timeline item exists which adds
     * it again.
     * Does not check the authorization status.
     * Does neither save the created [RemovedAffectedEntityEvent] nor the [issue].
     * It is necessary to save the [issue] or returned [RemovedAffectedEntityEvent] afterwards.
     *
     * @param issue the [Issue] which no longer should affect [affectedEntity]
     * @param affectedEntity the [AffectedByIssue] which should be removed from the affected entities on the [issue]
     * @param atTime the point in time when the modification happened, updates [Issue.lastUpdatedAt] if necessary
     * @param byUser the [User] who caused the update, updates [Issue.participants] if necessary
     * @param updateContext context used for the aggregation updater
     * @returns the created [RemovedAffectedEntityEvent]
     */
    suspend fun removeAffectedEntityFromIssue(
        issue: Issue,
        affectedEntity: AffectedByIssue,
        atTime: OffsetDateTime,
        byUser: User,
        updateContext: NodeBatchUpdateContext
    ): RemovedAffectedEntityEvent {
        val event = RemovedAffectedEntityEvent(atTime, atTime)
        event.removedAffectedEntity().value = affectedEntity
        createdTimelineItem(issue, event, atTime, byUser)
        if (!existsNewerTimelineItem<AddedAffectedEntityEvent>(
                issue, atTime
            ) { it.addedAffectedEntity().value == affectedEntity }
        ) {
            issue.affects() -= affectedEntity
            val aggregationUpdater = IssueAggregationUpdater(updateContext)
            aggregationUpdater.removedAffectedEntity(issue, affectedEntity)
        }
        return event
    }

    /**
     * Changes the value of a templated field on an [Issue], returns the created [TemplatedFieldChangedEvent]
     * or null if the value of the templated field was not changed.
     * Checks the authorization status, and checks that the field exists and the value is compatible.
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines the name and new value of the templated field to update
     * @return the saved created [TemplatedFieldChangedEvent] or `null` if no event was created
     */
    suspend fun changeIssueTemplatedField(
        authorizationContext: GropiusAuthorizationContext, input: ChangeIssueTemplatedFieldInput
    ): TemplatedFieldChangedEvent? {
        input.validate()
        val issue = repository.findById(input.issue)
        checkManageIssuesPermission(issue, authorizationContext)
        val newSerializedValue = jsonNodeMapper.jsonNodeToDeterministicString(input.value as JsonNode?)
        val oldSerializedValue = issue.templatedFields[input.name]
        return if (newSerializedValue != oldSerializedValue) {
            timelineItemRepository.save(
                changeIssueTemplatedField(
                    issue, input, oldSerializedValue, OffsetDateTime.now(), getUser(authorizationContext)
                )
            ).awaitSingle()
        } else {
            null
        }
    }

    /**
     * Changes the value of a templated field on an [issue] at [atTime] as [byUser] and adds
     * a [TemplatedFieldChangedEvent] to the timeline.
     * Creates the event even if the templated field was not changed.
     * Only changes the value of the templated field if no newer timeline item exists which changes it.
     * Does not check the authorization status.
     * Checks that the templated field exists and that the value is compatible.
     * Does neither save the created [TemplatedFieldChangedEvent] nor the [issue].
     * It is necessary to save the [issue] or returned [TemplatedFieldChangedEvent] afterwards.
     *
     * @param issue the [Issue] where a templated field should be changed
     * @param field defines the name and new value of the templated field to update
     * @param oldValue the old value of the templated field in serialized form
     * @param atTime the point in time when the modification happened, updates [Issue.lastUpdatedAt] if necessary
     * @param byUser the [User] who caused the update, updates [Issue.participants] if necessary
     * @returns the created [TypeChangedEvent]
     */
    suspend fun changeIssueTemplatedField(
        issue: Issue, field: JSONFieldInput, oldValue: String?, atTime: OffsetDateTime, byUser: User
    ): TemplatedFieldChangedEvent {
        templatedNodeService.ensureTemplatedFieldExist(issue.template().value, field.name)
        val newValue = jsonNodeMapper.jsonNodeToDeterministicString(field.value as JsonNode?)
        val event = TemplatedFieldChangedEvent(atTime, atTime, field.name, oldValue, newValue)
        createdTimelineItem(issue, event, atTime, byUser)
        if (!existsNewerTimelineItem<TemplatedFieldChangedEvent>(issue, atTime) { it.fieldName == field.name }) {
            templatedNodeService.updateTemplatedField(issue, field)
            updateAuditedNode(issue, byUser, atTime)
        }
        return event
    }

    /**
     * Creates a new [Assignment], returns the created [Assignment].
     * Checks the authorization status, and checks that the chosen type is compatible with the template of the [Issue].
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines the [Issue], [User] and optional [AssignmentType] of the [Assignment]
     * @return the saved created [Assignment]
     */
    suspend fun createAssignment(
        authorizationContext: GropiusAuthorizationContext, input: CreateAssignmentInput
    ): Assignment {
        input.validate()
        val issue = repository.findById(input.issue)
        checkManageIssuesPermission(issue, authorizationContext)
        val user = userRepository.findById(input.user)
        val assignmentType = input.assignmentType?.let { assignmentTypeRepository.findById(it) }
        val byUser = getUser(authorizationContext)
        val atTime = OffsetDateTime.now()
        val assignment = createAssignment(issue, user, assignmentType, atTime, byUser)
        createdAuditedNode(assignment, byUser)
        return assignmentRepository.save(assignment).awaitSingle()
    }

    /**
     * Creates a new [Assignment] on an [issue] at [atTime] as [byUser].
     * Does not check the authorization status.
     * If present, checks that the [assignmentType] is compatible with the template of the [issue].
     * Does neither save the created [Assignment] nor the [issue].
     * It is necessary to save the [issue] or returned [Assignment] afterwards.
     *
     * @param issue the [Issue] to which the [user] should be assigned
     * @param user the [User] to assign to [issue]
     * @param assignmentType the optional type of the created [Assignment], must be compatible with the template of [issue]
     * @param atTime the point in time when the modification happened, updates [Issue.lastUpdatedAt] if necessary
     * @param byUser the [User] who caused the update, updates [Issue.participants] if necessary
     * @returns the created [Assignment]
     */
    suspend fun createAssignment(
        issue: Issue, user: User, assignmentType: AssignmentType?, atTime: OffsetDateTime, byUser: User
    ): Assignment {
        if (assignmentType != null) {
            checkAssignmentTypeCompatibility(issue, assignmentType)
        }
        val event = Assignment(atTime, atTime)
        event.user().value = user
        event.type().value = assignmentType
        event.initialType().value = assignmentType
        createdTimelineItem(issue, event, atTime, byUser)
        issue.assignments() += event
        issue.participants() += user
        return event
    }

    /**
     * Checks that the `type` of an [Assignment] on [issue] can be changed to [newType]
     *
     * @param issue the [Issue] to check compatibility with, must have a set template
     * @param newType the new `type` of an [Assignment] on the [issue]
     * @throws IllegalArgumentException if the [newType] is not compatible with the template of the [issue]
     */
    private suspend fun checkAssignmentTypeCompatibility(issue: Issue, newType: AssignmentType) {
        if (issue.template().value !in newType.partOf()) {
            throw IllegalArgumentException(
                "AssignmentType cannot be used on the Assignment as it is not provided by the template of the Issue of the Assignment"
            )
        }
    }

    /**
     * Changes the `type` of an [Assignment], returns the created [AssignmentTypeChangedEvent] or null if the `type`
     * was not changed.
     * Checks the authorization status, and checks that the new [AssignmentType] can be used with the [Issue] the
     * [Assignment] is on.
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines the new `type` and of which [Assignment] to change it
     * @return the saved created [AssignmentTypeChangedEvent] or `null` if no event was created
     */
    suspend fun changeAssignmentType(
        authorizationContext: GropiusAuthorizationContext, input: ChangeAssignmentTypeInput
    ): AssignmentTypeChangedEvent? {
        input.validate()
        val assignment = assignmentRepository.findById(input.assignment)
        checkManageIssuesPermission(assignment.issue().value, authorizationContext)
        val newType = input.type?.let { assignmentTypeRepository.findById(it) }
        val oldType = assignment.type().value
        return if (oldType != newType) {
            timelineItemRepository.save(
                changeAssignmentType(
                    assignment, oldType, newType, OffsetDateTime.now(), getUser(authorizationContext)
                )
            ).awaitSingle()
        } else {
            null
        }
    }

    /**
     * Changes the `type` of an [assignment] at [atTime] as [byUser] and adds a [AssignmentTypeChangedEvent]
     * to the timeline.
     * Creates the event even if the `type` was not changed.
     * Only changes the `type` if no newer timeline item exists which changes it.
     * Does not check the authorization status.
     * Checks that the [newType] can be used with the [Issue] the [assignment] is on.
     * Does neither save the created [AssignmentTypeChangedEvent] nor the [assignment] nor the [Issue].
     * It is necessary to save the [assignment], the returned [AssignmentTypeChangedEvent] or the [Issue]
     * the [assignment] is on afterwards.
     *
     * @param assignment the [Assignment] where the `type` should be changed
     * @param oldType the old `type`
     * @param newType the new `type`
     * @param atTime the point in time when the modification happened, updates [Issue.lastUpdatedAt] if necessary
     * @param byUser the [User] who caused the update, updates [Issue.participants] if necessary
     * @returns the created [AssignmentTypeChangedEvent]
     */
    suspend fun changeAssignmentType(
        assignment: Assignment, oldType: AssignmentType?, newType: AssignmentType?, atTime: OffsetDateTime, byUser: User
    ): AssignmentTypeChangedEvent {
        val issue = assignment.issue().value
        if (newType != null) {
            checkAssignmentTypeCompatibility(issue, newType)
        }
        val event = AssignmentTypeChangedEvent(atTime, atTime)
        event.assignment().value = assignment
        event.oldType().value = oldType
        event.newType().value = newType
        createdTimelineItem(issue, event, atTime, byUser)
        if (!existsNewerTimelineItem<AssignmentTypeChangedEvent>(issue, atTime) {
                it.assignment().value == assignment
            } && assignment.type().value != newType) {
            assignment.type().value = newType
        }
        return event
    }

    /**
     * Removes an [Assignment] from its [Issue], returns the created [RemovedAssignmentEvent], or `null` if
     * the [Assignment] was already removed from its [Issue].
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [Assignment] to remove
     * @return the saved created [RemovedAssignmentEvent] or `null` if no event was created
     */
    suspend fun removeAssignment(
        authorizationContext: GropiusAuthorizationContext, input: RemoveAssignmentInput
    ): RemovedAssignmentEvent? {
        input.validate()
        val assignment = assignmentRepository.findById(input.assignment)
        val issue = assignment.issue().value
        checkManageIssuesPermission(issue, authorizationContext)
        return if (assignment in issue.assignments()) {
            timelineItemRepository.save(
                removeAssignment(assignment, OffsetDateTime.now(), getUser(authorizationContext))
            ).awaitSingle()
        } else {
            null
        }
    }

    /**
     * Removes an [assignment] from its [Issue] at [atTime] as [byUser] and adds a [RemovedAssignmentEvent]
     * to the timeline.
     * Creates the event even if the [assignment] was already removed from its [Issue].
     * Does not check the authorization status.
     * Does neither save the created [RemovedAssignmentEvent] nor the [Issue] of the [assignment].
     * It is necessary to save the [Issue] of the [assignment] or returned [RemovedAssignmentEvent] afterwards.
     *
     * @param assignment the [Assignment] to remove from its [Issue]
     * @param atTime the point in time when the modification happened, updates [Issue.lastUpdatedAt] if necessary
     * @param byUser the [User] who caused the update, updates [Issue.participants] if necessary
     * @returns the created [RemovedAssignmentEvent]
     */
    suspend fun removeAssignment(
        assignment: Assignment, atTime: OffsetDateTime, byUser: User
    ): RemovedAssignmentEvent {
        val issue = assignment.issue().value
        val event = RemovedAssignmentEvent(atTime, atTime)
        event.removedAssignment().value = assignment
        createdTimelineItem(issue, event, atTime, byUser)
        issue.assignments() -= assignment
        return event
    }

    /**
     * Creates a new [IssueRelation], returns the created [IssueRelation].
     * Checks the authorization status, and checks that the chosen type is compatible with the template of the [Issue].
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines the [Issue], [User] and optional [IssueRelationType] of the [IssueRelation]
     * @return the saved created [IssueRelation]
     */
    suspend fun createIssueRelation(
        authorizationContext: GropiusAuthorizationContext, input: CreateIssueRelationInput
    ): IssueRelation {
        input.validate()
        val issue = repository.findById(input.issue)
        val relatedIssue = repository.findById(input.relatedIssue)
        require(issue != relatedIssue) { "An Issue cannot be related to itself" }
        checkManageIssuesPermission(issue, authorizationContext)
        checkPermission(
            relatedIssue,
            Permission(NodePermission.READ, authorizationContext),
            "create IssueRelations ending at relatedIssue"
        )
        val issueRelationType = input.issueRelationType?.let { issueRelationTypeRepository.findById(it) }
        val byUser = getUser(authorizationContext)
        val updateContext = NodeBatchUpdateContext()
        val issueRelation = createIssueRelation(issue, relatedIssue, issueRelationType, OffsetDateTime.now(), byUser, updateContext)
        createdAuditedNode(issueRelation, byUser)
        return updateContext.save(issueRelation, nodeRepository)
    }

    /**
     * Creates a new [IssueRelation] from [issue] to [relatedIssue] at [atTime] as [byUser] and adds it to the timeline
     * of [issue]. Also creates a [RelatedByIssueEvent] and adds it to the timeline of [relatedIssue].
     * Does not check the authorization status.
     * If present, checks that the [issueRelationType] is compatible with the template of the [issue].
     * Does neither save the created [IssueRelation] nor the [issue].
     * It is necessary to save the [issue] or returned [IssueRelation] afterwards.
     *
     * @param issue the [Issue] from which the created [IssueRelation] starts
     * @param relatedIssue the [Issue] where the created [IssueRelation] ends
     * @param issueRelationType the optional type of the created [IssueRelation], must be compatible with the template of [issue]
     * @param atTime the point in time when the modification happened, updates [Issue.lastUpdatedAt] if necessary
     * @param byUser the [User] who caused the update, updates [Issue.participants] if necessary
     * @param updateContext context used for the aggregation updater
     * @returns the created [IssueRelation]
     */
    suspend fun createIssueRelation(
        issue: Issue,
        relatedIssue: Issue,
        issueRelationType: IssueRelationType?,
        atTime: OffsetDateTime,
        byUser: User,
        updateContext: NodeBatchUpdateContext
    ): IssueRelation {
        if (issueRelationType != null) {
            checkIssueRelationTypeCompatibility(issue, issueRelationType)
        }
        val relation = IssueRelation(atTime, atTime)
        relation.relatedIssue().value = relatedIssue
        relation.type().value = issueRelationType
        relation.initialType().value = issueRelationType
        createdTimelineItem(issue, relation, atTime, byUser)
        issue.outgoingRelations() += relation
        val relatedEvent = RelatedByIssueEvent(atTime, atTime)
        relatedEvent.relation().value = relation
        createdTimelineItemOnRelatedIssue(relatedIssue, relatedEvent, atTime, byUser)
        relatedIssue.incomingRelations() += relation

        val aggregationUpdater = IssueAggregationUpdater(updateContext)
        aggregationUpdater.createdIssueRelation(relation)

        return relation
    }

    /**
     * Checks that the `type` of an [IssueRelation] on [issue] can be changed to [newType]
     *
     * @param issue the [Issue] to check compatibility with, must have a set template
     * @param newType the new `type` of an [IssueRelation] on the [issue]
     * @throws IllegalArgumentException if the [newType] is not compatible with the template of the [issue]
     */
    private suspend fun checkIssueRelationTypeCompatibility(issue: Issue, newType: IssueRelationType) {
        if (issue.template().value !in newType.partOf()) {
            throw IllegalArgumentException(
                "IssueRelationType cannot be used on the IssueRelation as it is not provided by the template of the Issue of the IssueRelation"
            )
        }
    }

    /**
     * Changes the `type` of an [IssueRelation], returns the created [OutgoingRelationTypeChangedEvent] or null if
     * the `type` was not changed.
     * Checks the authorization status, and checks that the new [IssueRelationType] can be used with the [Issue] the
     * [IssueRelation] starts at.
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines the new `type` and of which [IssueRelation] to change it
     * @return the saved created [OutgoingRelationTypeChangedEvent] or `null` if no event was created
     */
    suspend fun changeIssueRelationType(
        authorizationContext: GropiusAuthorizationContext, input: ChangeIssueRelationTypeInput
    ): OutgoingRelationTypeChangedEvent? {
        input.validate()
        val issueRelation = issueRelationRepository.findById(input.issueRelation)
        checkManageIssuesPermission(issueRelation.issue().value, authorizationContext)
        val newType = input.type?.let { issueRelationTypeRepository.findById(it) }
        val oldType = issueRelation.type().value
        return if (oldType != newType) {
            val event = changeIssueRelationType(
                issueRelation, oldType, newType, OffsetDateTime.now(), getUser(authorizationContext)
            )
            nodeRepository.saveAll(listOf(event, issueRelation.relatedIssue().value!!)).collectList().awaitSingle()
                .first { it is OutgoingRelationTypeChangedEvent } as OutgoingRelationTypeChangedEvent
        } else {
            null
        }
    }

    /**
     * Changes the `type` of an [issueRelation] at [atTime] as [byUser] and adds a [OutgoingRelationTypeChangedEvent]
     * to the timeline on the start [Issue], and an  [IncomingRelationTypeChangedEvent] to the timeline on the related
     * [Issue].
     * Creates the event even if the `type` was not changed.
     * Only changes the `type` if no newer timeline item exists which changes it, this is only checked on the start
     * [Issue].
     * Does not check the authorization status.
     * Checks that the [newType] can be used with the [Issue] the [issueRelation] is on.
     * Does neither save the created [OutgoingRelationTypeChangedEvent] nor the [issueRelation] nor the [Issue].
     * It is necessary to save the [issueRelation], the returned [OutgoingRelationTypeChangedEvent] or the [Issue]
     * the [issueRelation] is on afterwards.
     * Caution: it is also necessary to save the related [Issue] manually!
     *
     * @param issueRelation the [IssueRelation] where the `type` should be changed
     * @param oldType the old `type`
     * @param newType the new `type`
     * @param atTime the point in time when the modification happened, updates [Issue.lastUpdatedAt] if necessary
     * @param byUser the [User] who caused the update, updates [Issue.participants] if necessary
     * @returns the created [OutgoingRelationTypeChangedEvent]
     */
    suspend fun changeIssueRelationType(
        issueRelation: IssueRelation,
        oldType: IssueRelationType?,
        newType: IssueRelationType?,
        atTime: OffsetDateTime,
        byUser: User
    ): OutgoingRelationTypeChangedEvent {
        val issue = issueRelation.issue().value
        if (newType != null) {
            checkIssueRelationTypeCompatibility(issue, newType)
        }
        val event = OutgoingRelationTypeChangedEvent(atTime, atTime)
        event.issueRelation().value = issueRelation
        event.oldType().value = oldType
        event.newType().value = newType
        createdTimelineItem(issue, event, atTime, byUser)
        val relatedEvent = IncomingRelationTypeChangedEvent(atTime, atTime)
        relatedEvent.issueRelation().value = issueRelation
        relatedEvent.oldType().value = oldType
        relatedEvent.newType().value = newType
        createdTimelineItemOnRelatedIssue(issueRelation.relatedIssue().value!!, relatedEvent, atTime, byUser)
        if (!existsNewerTimelineItem<OutgoingRelationTypeChangedEvent>(issue, atTime) {
                it.issueRelation().value == issueRelation
            } && issueRelation.type().value != newType) {
            issueRelation.type().value = newType
        }
        return event
    }

    /**
     * Removes an [IssueRelation] from its [Issue], returns the created [RemovedOutgoingRelationEvent], or `null` if
     * the [IssueRelation] was already removed from its [Issue].
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [IssueRelation] to remove
     * @return the saved created [RemovedOutgoingRelationEvent] or `null` if no event was created
     */
    suspend fun removeIssueRelation(
        authorizationContext: GropiusAuthorizationContext, input: RemoveIssueRelationInput
    ): RemovedOutgoingRelationEvent? {
        input.validate()
        val issueRelation = issueRelationRepository.findById(input.issueRelation)
        val issue = issueRelation.issue().value
        checkManageIssuesPermission(issue, authorizationContext)
        return if (issueRelation in issue.outgoingRelations()) {
            val updateContext = NodeBatchUpdateContext()
            val event = removeIssueRelation(issueRelation, OffsetDateTime.now(), getUser(authorizationContext), updateContext)
            updateContext.internalUpdatedNodes += issueRelation.relatedIssue().value!!
            updateContext.save(event, nodeRepository)
        } else {
            null
        }
    }

    /**
     * Removes an [issueRelation] from its [Issue] at [atTime] as [byUser] and adds a [RemovedOutgoingRelationEvent]
     * to the timeline. Also creates a [RemovedIncomingRelationEvent] and adds it to the timeline of the related [Issue].
     * Creates the event even if the [issueRelation] was already removed from its [Issue].
     * Does not check the authorization status.
     * Does neither save the created [RemovedOutgoingRelationEvent] nor the [Issue] of the [issueRelation].
     * It is necessary to save the [Issue] of the [issueRelation] or returned [RemovedOutgoingRelationEvent] afterwards.
     * Caution: it is also necessary to save the related [Issue] manually!
     *
     * @param issueRelation the [IssueRelation] to remove from its [Issue]
     * @param atTime the point in time when the modification happened, updates [Issue.lastUpdatedAt] if necessary
     * @param byUser the [User] who caused the update, updates [Issue.participants] if necessary
     * @param updateContext context used for the aggregation updater
     * @returns the created [RemovedOutgoingRelationEvent]
     */
    suspend fun removeIssueRelation(
        issueRelation: IssueRelation, atTime: OffsetDateTime, byUser: User, updateContext: NodeBatchUpdateContext
    ): RemovedOutgoingRelationEvent {
        val aggregationUpdater = IssueAggregationUpdater(updateContext)
        val issue = issueRelation.issue(aggregationUpdater.cache).value
        val relatedIssue = issueRelation.relatedIssue(aggregationUpdater.cache).value
        val event = RemovedOutgoingRelationEvent(atTime, atTime)
        event.removedRelation().value = issueRelation
        createdTimelineItem(issue, event, atTime, byUser)
        val relatedEvent = RemovedIncomingRelationEvent(atTime, atTime)
        relatedEvent.removedRelation().value = issueRelation
        createdTimelineItemOnRelatedIssue(relatedIssue!!, relatedEvent, atTime, byUser)
        issue.outgoingRelations() -= issueRelation
        relatedIssue.incomingRelations() -= issueRelation

        aggregationUpdater.deletedIssueRelation(issueRelation)

        return event
    }

    /**
     * Creates a new [IssueComment], returns the created [IssueComment].
     * Checks the authorization status, and checks that the referenced [Artefact] can be used on the [Issue].
     * Also, checks that the [Comment] it answers, if present,  is on the same [Issue]
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines the [Issue], [User] and optional [IssueRelationType] of the [IssueRelation]
     * @return the saved created [IssueComment]
     */
    suspend fun createIssueComment(
        authorizationContext: GropiusAuthorizationContext, input: CreateIssueCommentInput
    ): IssueComment {
        input.validate()
        val issue = repository.findById(input.issue)
        checkPermission(issue, Permission(TrackablePermission.COMMENT, authorizationContext), "create Comments")
        val artefacts = artefactRepository.findAllById((input.referencedArtefacts.orElse(emptyList())))
        for (artefact in artefacts) {
            checkPermission(artefact, Permission(NodePermission.READ, authorizationContext), "use the Artefact")
        }
        val answers = input.answers?.let { commentRepository.findById(it) }
        val byUser = getUser(authorizationContext)
        val issueComment = createIssueComment(issue, answers, input.body, artefacts, OffsetDateTime.now(), byUser)
        createdAuditedNode(issueComment, byUser)
        return timelineItemRepository.save(issueComment).awaitSingle()
    }

    /**
     * Creates a new IssueComment on [issue] at [atTime] as [byUser].
     * Does not check the authorization status.
     * Checks for each [Artefact] in [referencedArtefacts] that it is on a [Trackable] the [issue] is on.
     * If present, checks that the [Comment] it [answers] is on the same [issue].
     * Does neither save the created [IssueComment] nor the [issue].
     * It is necessary to save the [issue] or returned [IssueRelation] afterwards.
     *
     * @param issue the [Issue] from which the created [IssueRelation] starts
     * @param answers the [Comment] the created [IssueComment] answers
     * @param body the body of the created [IssueComment]
     * @param referencedArtefacts [Artefact]s the created [IssueComment] should reference
     * @param atTime the point in time when the modification happened, updates [Issue.lastUpdatedAt] if necessary
     * @param byUser the [User] who caused the update, updates [Issue.participants] if necessary
     * @returns the created [IssueComment]
     */
    suspend fun createIssueComment(
        issue: Issue,
        answers: Comment?,
        body: String,
        referencedArtefacts: List<Artefact>,
        atTime: OffsetDateTime,
        byUser: User
    ): IssueComment {
        if (answers != null && answers.issue().value != issue) {
            throw IllegalStateException("An IssueComment must answer a Comment on the same Issue")
        }
        val issueComment = IssueComment(atTime, atTime, body, atTime, false)
        issueComment.bodyLastEditedBy().value = byUser
        issueComment.answers().value = answers
        issue.issueComments() += issueComment
        createdTimelineItem(issue, issueComment, atTime, byUser)
        addArtefactsToIssueComment(issueComment, referencedArtefacts)
        return issueComment
    }

    /**
     * Adds [referencedArtefacts] to the `referencedArtefacts` of [issueComment]
     * Does not mark the [issueComment] as updated
     *
     * @param issueComment the [IssueComment] where to add the [referencedArtefacts]
     * @param referencedArtefacts the [Artefact]s to add
     */
    private suspend fun addArtefactsToIssueComment(issueComment: IssueComment, referencedArtefacts: List<Artefact>) {
        val issue = issueComment.issue().value
        for (artefact in referencedArtefacts) {
            if (artefact.trackable().value !in issue.trackables()) {
                throw IllegalStateException("Artefact cannot be referenced as it is not on any of the Trackables the Issue is on")
            }
        }
        issueComment.referencedArtefacts() += referencedArtefacts
    }

    /**
     * Updates an [IssueComment], returns the updated [IssueComment].
     * Checks the authorization status, and checks that the added referenced [Artefact] can be used on the [Issue].
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines the [IssueComment] to update
     * @return the saved updated [IssueComment]
     */
    suspend fun updateIssueComment(
        authorizationContext: GropiusAuthorizationContext, input: UpdateIssueCommentInput
    ): IssueComment {
        input.validate()
        val issueComment = issueCommentRepository.findById(input.id)
        val user = getUser(authorizationContext)
        checkCommentEditPermission(issueComment, user, authorizationContext)
        val addedArtefacts = artefactRepository.findAllById(input.addedReferencedArtefacts.orElse(emptyList()))
        val removedArtefacts = artefactRepository.findAllById(input.removedReferencedArtefacts.orElse(emptyList()))
        for (artefact in addedArtefacts + removedArtefacts) {
            checkPermission(artefact, Permission(NodePermission.READ, authorizationContext), "use the Artefact")
        }
        val byUser = getUser(authorizationContext)
        val atTime = OffsetDateTime.now()
        updateIssueComment(issueComment, input.body.orElse(null), addedArtefacts, removedArtefacts, atTime, byUser)
        updateAuditedNode(issueComment, byUser, atTime)
        return timelineItemRepository.save(issueComment).awaitSingle()
    }

    /**
     * Updates an [issueComment] at [atTime] as [byUser].
     * Does not check the authorization status.
     * Checks for each [Artefact] in [addedArtefacts] that it can be used with the [Issue] the [issueComment] is on
     * Does not save the updated [IssueComment].
     * It is necessary to save the updated [issueComment] afterwards.
     *
     * @param issueComment the [IssueComment] to update
     * @param newBodyValue if not `null`, the new body of the [issueComment]
     * @param addedArtefacts referenced [Artefact]s to add
     * @param removedArtefacts referenced [Artefact]s to remove
     * @param atTime the point in time when the modification happened, updates [Issue.lastUpdatedAt] if necessary
     * @param byUser the [User] who caused the update, updates [Issue.participants] if necessary
     */
    suspend fun updateIssueComment(
        issueComment: IssueComment,
        newBodyValue: String?,
        addedArtefacts: List<Artefact>,
        removedArtefacts: List<Artefact>,
        atTime: OffsetDateTime,
        byUser: User
    ) {
        issueComment.referencedArtefacts() -= removedArtefacts.toSet()
        addArtefactsToIssueComment(issueComment, addedArtefacts)
        if (newBodyValue != null && newBodyValue != issueComment.body && atTime >= issueComment.bodyLastEditedAt) {
            issueComment.body = newBodyValue
            issueComment.bodyLastEditedAt = atTime
            issueComment.bodyLastEditedBy().value = byUser
        }
        updateAuditedNode(issueComment, byUser, atTime)
    }

    /**
     * Checks that a [user] has the permission to update a [comment]
     *
     * @param comment the [Comment] to check for the permission on
     * @param user the [User] who needs the permission
     * @param authorizationContext required to check for the permission
     * @throws IllegalStateException if the [user] does not have the permission
     */
    private suspend fun checkCommentEditPermission(
        comment: Comment, user: GropiusUser, authorizationContext: GropiusAuthorizationContext
    ) {
        if (comment.createdBy().value != user) {
            checkPermission(
                comment.issue().value,
                Permission(TrackablePermission.MODERATOR, authorizationContext),
                "update the IssueComment"
            )
        } else {
            checkPermission(
                comment, Permission(NodePermission.READ, authorizationContext), "update the IssueComment"
            )
        }
    }

    /**
     * Updates an [IssueComment], returns the updated [IssueComment].
     * Checks the authorization status, and checks that the added referenced [Artefact] can be used on the [Issue].
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines the [IssueComment] to update
     * @return the saved updated [IssueComment]
     */
    suspend fun updateBody(
        authorizationContext: GropiusAuthorizationContext, input: UpdateBodyInput
    ): Body {
        input.validate()
        val body = bodyRepository.findById(input.id)
        val user = getUser(authorizationContext)
        checkCommentEditPermission(body, user, authorizationContext)
        val byUser = getUser(authorizationContext)
        val atTime = OffsetDateTime.now()
        updateBody(body, input.body.orElse(null), atTime, byUser)
        updateAuditedNode(body, byUser, atTime)
        repository.save(body.issue().value).awaitSingle()
        return timelineItemRepository.save(body).awaitSingle()
    }

    /**
     * Updates an [body] at [atTime] as [byUser].
     * Does not check the authorization status.
     * Does not save the updated [Body].
     * It is necessary to save the updated [body] afterwards.
     *
     * @param body the [Body] to update
     * @param newBodyValue if not `null`, the new body of the [body]
     * @param atTime the point in time when the modification happened, updates [Issue.lastUpdatedAt] if necessary
     * @param byUser the [User] who caused the update, updates [Issue.participants] if necessary
     */
    suspend fun updateBody(
        body: Body, newBodyValue: String?, atTime: OffsetDateTime, byUser: User
    ) {
        val issue = body.issue().value
        if (newBodyValue != null && newBodyValue != issue.bodyBody && atTime >= body.bodyLastEditedAt) {
            issue.bodyBody = newBodyValue
            body.bodyLastEditedAt = atTime
            body.bodyLastEditedBy().value = byUser
        }
        updateAuditedNode(body, byUser, atTime)
    }

    /**
     * Deletes an [IssueComment], returns the deleted [IssueComment].
     * Checks the authorization status.
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines the [IssueComment] to delete
     * @return the saved deleted [IssueComment]
     */
    suspend fun deleteIssueComment(
        authorizationContext: GropiusAuthorizationContext, input: DeleteNodeInput
    ): IssueComment {
        input.validate()
        val issueComment = issueCommentRepository.findById(input.id)
        val user = getUser(authorizationContext)
        checkCommentEditPermission(issueComment, user, authorizationContext)
        deleteIssueComment(issueComment, OffsetDateTime.now(), getUser(authorizationContext))
        return timelineItemRepository.save(issueComment).awaitSingle()
    }

    /**
     * Deletes an [issueComment] at [atTime] as [byUser].
     * Does not check the authorization status.
     * Does not save the deleted [IssueComment].
     * It is necessary to save the deleted [issueComment] afterwards.
     *
     * @param issueComment the [IssueComment] to delete
     * @param atTime the point in time when the modification happened, updates [Issue.lastUpdatedAt] if necessary
     * @param byUser the [User] who caused the update, updates [Issue.participants] if necessary
     */
    suspend fun deleteIssueComment(
        issueComment: IssueComment, atTime: OffsetDateTime, byUser: User
    ) {
        issueComment.isCommentDeleted = true
        issueComment.referencedArtefacts().clear()
        updateAuditedNode(issueComment, byUser, atTime)
    }

    /**
     * Checks that the user has [TrackablePermission.MANAGE_ISSUES] on [issue]
     *
     * @param issue the [Issue] where the permission must be granted
     * @param authorizationContext necessary for checking for the permission
     * @throws IllegalArgumentException if the permission is not granted
     */
    private suspend fun checkManageIssuesPermission(issue: Issue, authorizationContext: GropiusAuthorizationContext) {
        checkPermission(issue, Permission(TrackablePermission.MANAGE_ISSUES, authorizationContext), "manage the Issue")
    }

    /**
     * Checks that the user has [TrackablePermission.MANAGE_ISSUES] on [trackable]
     *
     * @param trackable the [Trackable] where the permission must be granted
     * @param authorizationContext necessary for checking for the permission
     * @throws IllegalArgumentException if the permission is not granted
     */
    private suspend fun checkManageIssuesPermission(
        trackable: Trackable, authorizationContext: GropiusAuthorizationContext
    ) {
        checkPermission(
            trackable,
            Permission(TrackablePermission.MANAGE_ISSUES, authorizationContext),
            "manage Issues on the Trackable"
        )
    }

    /**
     * Checks that the user has [TrackablePermission.CREATE_ISSUES] on [trackable]
     *
     * @param trackable the [Trackable] where the permission must be granted
     * @param authorizationContext necessary for checking for the permission
     * @throws IllegalArgumentException if the permission is not granted
     */
    private suspend fun checkCreateIssuesPermission(
        trackable: Trackable, authorizationContext: GropiusAuthorizationContext
    ) {
        checkPermission(
            trackable,
            Permission(TrackablePermission.MANAGE_ISSUES, authorizationContext),
            "create Issues on the Trackable"
        )
    }

    /**
     * Changes a property of [issue], returns the created event or null if the property was not changed.
     * Checks the authorization status ([TrackablePermission.MANAGE_ISSUES] on the [issue])
     *
     * @param T the type of the property
     * @param E the type of the timeline item
     * @param authorizationContext used to check for the required permission
     * @param issue the [Issue] to update
     * @param currentValue the current value of the property
     * @param newValue the new value of the property
     * @param internalFunction used to create the returned event and apply the change
     * @return the saved created event or `null` if no event was created
     */
    private suspend fun <T, E : TimelineItem> changeIssueProperty(
        authorizationContext: GropiusAuthorizationContext,
        issue: Issue,
        currentValue: T,
        newValue: T,
        internalFunction: suspend (issue: Issue, oldValue: T, newValue: T, atTime: OffsetDateTime, byUser: User, updateContext: NodeBatchUpdateContext) -> E,
    ): E? {
        checkManageIssuesPermission(issue, authorizationContext)
        return if (currentValue != newValue) {
            val updateContext = NodeBatchUpdateContext()
            val event = internalFunction(
                issue, currentValue, newValue, OffsetDateTime.now(), getUser(authorizationContext), updateContext
            )
            updateContext.save(event, nodeRepository)
        } else {
            null
        }
    }

    /**
     * Changes a property of an [issue] at [atTime] as [byUser] and adds the [event] to the timeline.
     * Only changes the property if no newer timeline item exists which changes it.
     * Does not check the authorization status.
     * Does neither save the [event] nor the [issue].
     * Calls [createdTimelineItem] on the [event].
     *
     * @param T the type of the property
     * @param E the type of the timeline item
     * @param issue the [Issue] where the `spentTime` should be changed
     * @param newValue the new value of the property
     * @param atTime the point in time when the modification happened, updates [Issue.lastUpdatedAt] if necessary
     * @param property the property on the [issue]
     * @param event the already created [TimelineItem] for the change
     * @param byUser the [User] who caused the update, updates [Issue.participants] if necessary
     */
    private suspend inline fun <T, reified E : TimelineItem> changeIssueProperty(
        issue: Issue, newValue: T, atTime: OffsetDateTime, byUser: User, property: KMutableProperty0<T>, event: E
    ) {
        createdTimelineItem(issue, event, atTime, byUser)
        if (!existsNewerTimelineItem<E>(issue, atTime) && (property.get() != newValue)) {
            property.set(newValue)
        }
    }

    /**
     * Called after a [TimelineItem] was created
     * Adds it to the [issue], calls [createdAuditedNode] and updates [Issue.lastUpdatedAt] and [Issue.participants].
     * Also calls [updateAuditedNode] on the [issue].
     * Also sets [TimelineItem.issue], to allow to save [timelineItem] instead of [issue].
     *
     * @param issue associated [Issue] to which [timelineItem] should be added
     * @param timelineItem the created [TimelineItem]
     * @param atTime point in time at which [timelineItem] was created
     * @param byUser the [User] who created [timelineItem]
     */
    private suspend fun createdTimelineItem(
        issue: Issue, timelineItem: TimelineItem, atTime: OffsetDateTime, byUser: User
    ) {
        createdAuditedNode(timelineItem, byUser)
        updateAuditedNode(issue, byUser, atTime)
        timelineItem.issue().value = issue
        issue.timelineItems() += timelineItem
        issue.participants() += byUser
        issue.lastUpdatedAt = maxOf(issue.lastUpdatedAt, atTime)
    }

    /**
     * Called after a [TimelineItem] was created on a related [Issue]
     * Adds it to the [issue], calls [createdAuditedNode] and updates [Issue.lastModifiedAt] and [Issue.lastUpdatedAt]
     * Also sets [TimelineItem.issue], to allow to save [timelineItem] instead of [issue].
     * Due to confidentiality reasons, [Issue.participants], [Issue.lastModifiedBy] are not updated.
     *
     * @param issue associated [Issue] to which [timelineItem] should be added
     * @param timelineItem the created [TimelineItem]
     * @param atTime point in time at which [timelineItem] was created
     * @param byUser the [User] who created [timelineItem]
     */
    private suspend fun createdTimelineItemOnRelatedIssue(
        issue: Issue, timelineItem: TimelineItem, atTime: OffsetDateTime, byUser: User
    ) {
        createdAuditedNode(timelineItem, byUser)
        timelineItem.issue().value = issue
        issue.timelineItems() += timelineItem
        issue.lastUpdatedAt = maxOf(issue.lastUpdatedAt, atTime)
        issue.lastModifiedAt = maxOf(issue.lastModifiedAt, atTime)
    }

    /**
     * Checks if a [TimelineItem] with type [T] exists on [issue] created after [time] matching [itemFilter]
     *
     * @param T the type of [TimelineItem] to look for
     * @param issue contains the timeline to check
     * @param time only consider items after this time
     * @param itemFilter used to further filter for acceptable [TimelineItem]s
     * @return the result of the check
     */
    private suspend inline fun <reified T : TimelineItem> existsNewerTimelineItem(
        issue: Issue, time: OffsetDateTime, itemFilter: (T) -> Boolean = { true }
    ): Boolean {
        return issue.timelineItems().any {
            (it is T) && (it.createdAt > time) && itemFilter(it)
        }
    }

}