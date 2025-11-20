package gropius.schema.mutation

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import graphql.schema.DataFetchingEnvironment
import gropius.authorization.gropiusAuthorizationContext
import gropius.dto.input.common.DeleteNodeInput
import gropius.dto.input.issue.*
import gropius.dto.payload.DeleteNodePayload
import gropius.graphql.AutoPayloadType
import gropius.model.architecture.Trackable
import gropius.model.issue.*
import gropius.model.issue.timeline.*
import gropius.service.issue.*
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * Contains all issue-related mutations
 *
 * @param issueService used for Issue-related mutations
 * @param labelService used for Label-related mutations
 * @param artefactService used for Artefact-related mutations
 */
@Component
@Transactional(propagation = Propagation.REQUIRES_NEW)
class IssueMutations(
    private val issueService: IssueService,
    private val labelService: LabelService,
    private val artefactService: ArtefactService,
    private val issueBoardService: IssueBoardService,
    private val issueBoardColumnService: IssueBoardColumnService,
    private val issueBoardItemService: IssueBoardItemService
) : Mutation {

    @GraphQLDescription(
        """Creates a new Issue on at least one Trackable, requires CREATE_ISSUES on all Trackables it should be created on.
        Additionally, checks that the `type`, `state` and `templatedFields` are compatible with the `template`.
        """
    )
    @AutoPayloadType("The created Issue")
    suspend fun createIssue(
        @GraphQLDescription("Defines the created Issue")
        input: CreateIssueInput, dfe: DataFetchingEnvironment
    ): Issue {
        return issueService.createIssue(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription("Deletes the specified Issue, requires MODERATOR on all of the Trackables the Issue is on.")
    suspend fun deleteIssue(
        @GraphQLDescription("Defines which Issue to delete")
        input: DeleteNodeInput, dfe: DataFetchingEnvironment
    ): DeleteNodePayload {
        issueService.deleteIssue(dfe.gropiusAuthorizationContext, input)
        return DeleteNodePayload(input.id)
    }

    @GraphQLDescription(
        """Changes the Template of an Issue. Requires MANAGE_ISSUES on any of the Trackables the Issue is on.
        Incompatible old values of type, state, priority, templated fields, and priorities of Assignments and outgoing
        IssueRelations are updated with provided new values, requires that those new values are compatible with the new
        template. If the old value already is compatible with the new template, fields are not changed!
        In case of priority and types of Assignments and outgoing IssueRelations, if the old value is incompatible and
        no new value was provided, the old value is removed.
        Only creates an event if the new template is not equal to the current template.
        Events for other changes can be found in the childItems of the returned event.
        """
    )
    @AutoPayloadType("The created event, if present")
    suspend fun changeIssueTemplate(
        @GraphQLDescription("Defines the new IssueTemplate, the Issue and other fields to update")
        input: ChangeIssueTemplateInput, dfe: DataFetchingEnvironment
    ): TemplateChangedEvent? {
        return issueService.changeIssueTemplate(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Creates a new Label on at least one Trackable. Requires MANAGE_LABELS on all provided Trackables.
        """
    )
    @AutoPayloadType("The created Label")
    suspend fun createLabel(
        @GraphQLDescription("Defines the created Label")
        input: CreateLabelInput, dfe: DataFetchingEnvironment
    ): Label {
        return labelService.createLabel(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription("Updates the specified Label, requires MANAGE_LABELS on any Trackable the Label is on")
    @AutoPayloadType("The updated Label")
    suspend fun updateLabel(
        @GraphQLDescription("Defines which Label to update and how to update it")
        input: UpdateLabelInput, dfe: DataFetchingEnvironment
    ): Label {
        return labelService.updateLabel(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Adds a Label to a Trackable, requires MANAGE_LABELS on the Trackable and EXPORT_LABEL on any Trackable
        the Label is on        
        """
    )
    @AutoPayloadType("The Trackable to which the Label was added")
    suspend fun addLabelToTrackable(
        @GraphQLDescription("Defines the Label and Trackable")
        input: AddLabelToTrackableInput, dfe: DataFetchingEnvironment
    ): Trackable {
        return labelService.addLabelToTrackable(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Removes a Label from a Trackable, requires MANAGE_LABELS on the Trackable.
        Removes the Label from all Issues where the Label cannot be anymore, as the Trackable of the Label and the Issue
        are now disjoint.
        """
    )
    @AutoPayloadType("The Trackable from which the Label was removed")
    suspend fun removeLabelFromTrackable(
        @GraphQLDescription("Defines the Label and Trackable")
        input: RemoveLabelFromTrackableInput, dfe: DataFetchingEnvironment
    ): Trackable {
        return labelService.removeLabelFromTrackable(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Deletes the Label, requires MANAGE_LABELS on all Trackables it is on.
        Removes it from all Issues. Note that the Label will still be visible in the timeline of Issues.
        """
    )
    suspend fun deleteLabel(
        @GraphQLDescription("Defines which Label to delete")
        input: DeleteNodeInput, dfe: DataFetchingEnvironment
    ): DeleteNodePayload {
        labelService.deleteLabel(dfe.gropiusAuthorizationContext, input)
        return DeleteNodePayload(input.id)
    }

    @GraphQLDescription("Creates a new Artefact on a Trackable. Requires MANAGE_ARTEFACTS on the provided Trackable.")
    @AutoPayloadType("The created Artefact")
    suspend fun createArtefact(
        @GraphQLDescription("Defines the created Artefact")
        input: CreateArtefactInput, dfe: DataFetchingEnvironment
    ): Artefact {
        return artefactService.createArtefact(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Updates the specified Artefact, requires MANAGE_ARTEFACTS on the Trackable the Artefact is part of
        """
    )
    @AutoPayloadType("The updated Artefact")
    suspend fun updateArtefact(
        @GraphQLDescription("Defines which Artefact to update and how to update it")
        input: UpdateArtefactInput, dfe: DataFetchingEnvironment
    ): Artefact {
        return artefactService.updateArtefact(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Deletes the Artefact, requires MANAGE_ARTEFACTS on the Trackable it is part of. Removes it from all Issues.
        """
    )
    suspend fun deleteArtefact(
        @GraphQLDescription("Defines which Artefact to delete")
        input: DeleteNodeInput, dfe: DataFetchingEnvironment
    ): DeleteNodePayload {
        artefactService.deleteArtefact(dfe.gropiusAuthorizationContext, input)
        return DeleteNodePayload(input.id)
    }

    @GraphQLDescription(
        """Adds a Label to an Issue, requires MANAGE_ISSUES on any Trackable the issue is on and READ on the Label.
        Additionally, the Label must be on at least on Trackable the Issue is on.
        If the Label is already on the Issue, no event is created.
        """
    )
    @AutoPayloadType("The created event, if present")
    suspend fun addLabelToIssue(
        @GraphQLDescription("Defines the Label and Issue")
        input: AddLabelToIssueInput, dfe: DataFetchingEnvironment
    ): AddedLabelEvent? {
        return issueService.addLabelToIssue(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Removes a Label from an Issue, requires MANAGE_ISSUES on any Trackable the issue is on.
        If the Label is not on the Issue, no event is created.
        """
    )
    @AutoPayloadType("The created event, if present")
    suspend fun removeLabelFromIssue(
        @GraphQLDescription("Defines the Label and Issue")
        input: RemoveLabelFromIssueInput, dfe: DataFetchingEnvironment
    ): RemovedLabelEvent? {
        return issueService.removeLabelFromIssue(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Adds a Artefact to an Issue, requires MANAGE_ISSUES on any Trackable the issue is on and READ on the Artefact.
        Additionally, the Artefact must be part of a Trackable the Issue is on.
        If the Artefact is already on the Issue, no event is created.
        """
    )
    @AutoPayloadType("The created event, if present")
    suspend fun addArtefactToIssue(
        @GraphQLDescription("Defines the Artefact and Issue")
        input: AddArtefactToIssueInput, dfe: DataFetchingEnvironment
    ): AddedArtefactEvent? {
        return issueService.addArtefactToIssue(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Removes a Artefact from an Issue, requires MANAGE_ISSUES on any Trackable the issue is on.
        If the Artefact is not on the Issue, no event is created.
        """
    )
    @AutoPayloadType("The created event, if present")
    suspend fun removeArtefactFromIssue(
        @GraphQLDescription("Defines the Artefact and Issue")
        input: RemoveArtefactFromIssueInput, dfe: DataFetchingEnvironment
    ): RemovedArtefactEvent? {
        return issueService.removeArtefactFromIssue(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Adds an Issue to a Trackable, requires MANAGE_ISSUES on the Trackable the Issue should be added to,
        and EXPORT_ISSUES on any Trackable the Issue currently is on.
        If the Issue is already on the Trackable, no event is created.
        """
    )
    @AutoPayloadType("The created event, if present")
    suspend fun addIssueToTrackable(
        @GraphQLDescription("Defines the Issue and Trackable")
        input: AddIssueToTrackableInput, dfe: DataFetchingEnvironment
    ): AddedToTrackableEvent? {
        return issueService.addIssueToTrackable(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Removes an Issue from a Trackable, requires MANAGE_ISSUES on the Trackable where the Issue should
        be removed from.
        Additionally requires that the Issue is on at least one Trackable afterwards.
        If the Issue is not on the Trackable, no event is created.
        Also removes any Artefacts, Labels and AffectedByIssue which cannot be any more on the Issue, 
        and unpins the issue on the defined Trackable if it was pinned.
        The created events can be found in the childItems of the returned RemovedFromTrackableEvent.
        """
    )
    @AutoPayloadType("The created event, if present")
    suspend fun removeIssueFromTrackable(
        @GraphQLDescription("Defines the Issue and Trackable")
        input: RemoveIssueFromTrackableInput, dfe: DataFetchingEnvironment
    ): RemovedFromTrackableEvent? {
        return issueService.removeIssueFromTrackable(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Pins an Issue on a Trackable, requires MANAGE_ISSUES on the Trackable the Issue should be pinned on.
        Additionally, the Issue must already be on the Trackable.
        If the Issue is already pinned on the Trackable, no event is created.
        """
    )
    @AutoPayloadType("The created event, if present")
    suspend fun addIssueToPinnedIssues(
        @GraphQLDescription("Defines the Issue and Trackable")
        input: AddIssueToPinnedIssuesInput, dfe: DataFetchingEnvironment
    ): AddedToPinnedIssuesEvent? {
        return issueService.addIssueToPinnedIssues(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Unpins the Issue on a Trackable, requires MANAGE_ISSUES on the Trackable where the Issue should
        be unpinned.
        If the Issue is not pinned on the Trackable, no event is created.
        """
    )
    @AutoPayloadType("The created event, if present")
    suspend fun removeIssueFromPinnedIssues(
        @GraphQLDescription("Defines the Issue and Trackable")
        input: RemoveIssueFromPinnedIssuesInput, dfe: DataFetchingEnvironment
    ): RemovedFromPinnedIssuesEvent? {
        return issueService.removeIssueFromPinnedIssues(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Changes the `priority` of an Issue requires MANAGE_ISSUES on any of the Trackables the Issue is on.
        Additionally, the IssuePriority must be defined by the Template of the Issue.
        If the `priority` is equal to the already existing `priority`, no event is created.
        """
    )
    @AutoPayloadType("The created event, if present")
    suspend fun changeIssuePriority(
        @GraphQLDescription("Defines the Issue and new `priority`")
        input: ChangeIssuePriorityInput, dfe: DataFetchingEnvironment
    ): PriorityChangedEvent? {
        return issueService.changeIssuePriority(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Changes the `state` of an Issue requires MANAGE_ISSUES on any of the Trackables the Issue is on.
        Additionally, the IssueState must be defined by the Template of the Issue.
        If the `state` is equal to the already existing `state`, no event is created.
        """
    )
    @AutoPayloadType("The created event, if present")
    suspend fun changeIssueState(
        @GraphQLDescription("Defines the Issue and new `state`")
        input: ChangeIssueStateInput, dfe: DataFetchingEnvironment
    ): StateChangedEvent? {
        return issueService.changeIssueState(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Changes the `title` of an Issue requires MANAGE_ISSUES on any of the Trackables the Issue is on.
        If the `title` is equal to the already existing `title`, no event is created.
        """
    )
    @AutoPayloadType("The created event, if present")
    suspend fun changeIssueTitle(
        @GraphQLDescription("Defines the Issue and new `title`")
        input: ChangeIssueTitleInput, dfe: DataFetchingEnvironment
    ): TitleChangedEvent? {
        return issueService.changeIssueTitle(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Changes the `type` of an Issue requires MANAGE_ISSUES on any of the Trackables the Issue is on.
        Additionally, the IssueType must be defined by the Template of the Issue.
        If the `type` is equal to the already existing `type`, no event is created.
        """
    )
    @AutoPayloadType("The created event, if present")
    suspend fun changeIssueType(
        @GraphQLDescription("Defines the Issue and new `type`")
        input: ChangeIssueTypeInput, dfe: DataFetchingEnvironment
    ): TypeChangedEvent? {
        return issueService.changeIssueType(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Adds an AffectedByIssue to an Issue, requires MANAGE_ISSUES on any of the Trackables the Issue is on,
        and AFFECT_ENTITIES_WITH_ISSUES on the Trackable associated with the AffectedByIssue.
        If the Issue already affects the AffectedByIssue, no event is created.
        """
    )
    @AutoPayloadType("The created event, if present")
    suspend fun addAffectedEntityToIssue(
        @GraphQLDescription("Defines the Issue and Trackable")
        input: AddAffectedEntityToIssueInput, dfe: DataFetchingEnvironment
    ): AddedAffectedEntityEvent? {
        return issueService.addAffectedEntityToIssue(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Removes an AffectedByIssue from an Issue, requires MANAGE_ISSUES on any of the Trackables the Issue is on,
        or AFFECT_ENTITIES_WITH_ISSUES on the Trackable associated with the AffectedByIssue.
        If the Issue does not affect the AffectedByIssue, no event is created.
        """
    )
    @AutoPayloadType("The created event, if present")
    suspend fun removeAffectedEntityFromIssue(
        @GraphQLDescription("Defines the Issue and Trackable")
        input: RemoveAffectedEntityFromIssueInput, dfe: DataFetchingEnvironment
    ): RemovedAffectedEntityEvent? {
        return issueService.removeAffectedEntityFromIssue(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Changes the value of a templated field on an Issue, requires MANAGE_ISSUES on any of the Trackables 
        the Issue is on.
        Additionally, the field with the name must be defined by the template of the Issue, and the provided new value
        must be compatible.
        If the new value is equal to the current value of the templated field, no event is created.
        """
    )
    @AutoPayloadType("The created event, if present")
    suspend fun changeIssueTemplatedField(
        @GraphQLDescription("Defines the Issue, the templated field and its new value")
        input: ChangeIssueTemplatedFieldInput, dfe: DataFetchingEnvironment
    ): TemplatedFieldChangedEvent? {
        return issueService.changeIssueTemplatedField(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Assigns a User to an Issue by creating an Assignment, requires MANAGE_ISSUES on any of the Trackables the
        Issue is on.
        Additionally, if present, the type must be compatible with the template of the Issue.            
        """
    )
    @AutoPayloadType("The created Assignment")
    suspend fun createAssignment(
        @GraphQLDescription("Defines the Issue, User, and optional AssignmentType")
        input: CreateAssignmentInput, dfe: DataFetchingEnvironment
    ): Assignment {
        return issueService.createAssignment(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Changes the type of an Assignment, requires MANAGE_ISSUES on any of the Trackables the Issue the Assignment
        is part of is on.
        Additionally, if present, the new type must be compatible with the template of the Issue.
        If the current type of the Assignment is equal to the new one, no event is created.
        """
    )
    @AutoPayloadType("The created event, if present")
    suspend fun changeAssignmentType(
        @GraphQLDescription("Defines the Assignment to update and its new AssignmentType")
        input: ChangeAssignmentTypeInput, dfe: DataFetchingEnvironment
    ): AssignmentTypeChangedEvent? {
        return issueService.changeAssignmentType(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Removes an Assignment from an Issue, require MANAGE_ISSUES on any of the Trackables the Issue the Assignment
        is part of is on.
        If the Assignment was already removed, no event is created.
        """
    )
    @AutoPayloadType("The created event, if present")
    suspend fun removeAssignment(
        @GraphQLDescription("Defines the Assignment to remove")
        input: RemoveAssignmentInput, dfe: DataFetchingEnvironment
    ): RemovedAssignmentEvent? {
        return issueService.removeAssignment(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Creates an IssueRelation, requires MANAGE_ISSUES on any of the Trackables the Issue is on.
        Additionally, if present, the type must be compatible with the template of the Issue.            
        """
    )
    @AutoPayloadType("The created IssueRelation")
    suspend fun createIssueRelation(
        @GraphQLDescription("Defines the Issue, related Issue, and optional IssueRelationType")
        input: CreateIssueRelationInput, dfe: DataFetchingEnvironment
    ): IssueRelation {
        return issueService.createIssueRelation(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Changes the type of an IssueRelation, requires MANAGE_ISSUES on any of the Trackables the Issue the 
        IssueRelation is part of is on.
        Additionally, if present, the new type must be compatible with the template of the Issue.
        If the current type of the IssueRelation is equal to the new one, no event is created.
        """
    )
    @AutoPayloadType("The created event, if present")
    suspend fun changeIssueRelationType(
        @GraphQLDescription("Defines the IssueRelation to update and its new IssueRelationType")
        input: ChangeIssueRelationTypeInput, dfe: DataFetchingEnvironment
    ): OutgoingRelationTypeChangedEvent? {
        return issueService.changeIssueRelationType(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Removes an IssueRelation from an Issue, require MANAGE_ISSUES on any of the Trackables the Issue the
        IssueRelation starts at is on.
        If the IssueRelation was already removed, no event is created.
        """
    )
    @AutoPayloadType("The created event, if present")
    suspend fun removeIssueRelation(
        @GraphQLDescription("Defines the IssueRelation to remove")
        input: RemoveIssueRelationInput, dfe: DataFetchingEnvironment
    ): RemovedOutgoingRelationEvent? {
        return issueService.removeIssueRelation(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Creates an IssueComment, requires COMMENT on any of the Trackables the Issue is on.
        Requires READ on referenced Artefacts, additionally, those must be part of a Trackable the Issue is on.
        If present, the Comment it answers must be on the same Issue.
        """
    )
    @AutoPayloadType("The created IssueComment")
    suspend fun createIssueComment(
        @GraphQLDescription("Defines the IssueComment")
        input: CreateIssueCommentInput, dfe: DataFetchingEnvironment
    ): IssueComment {
        return issueService.createIssueComment(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Updates an IssueComment. If the user created the IssueComment, requires READ on any of the Trackables the
        Issue is on. Otherwise, requires MODERATOR on any of the Trackables the Issue is on.
        Requires READ on referenced Artefacts, additionally, added ones must be part of a Trackable the Issue is on.
        """
    )
    @AutoPayloadType("The updated IssueComment")
    suspend fun updateIssueComment(
        @GraphQLDescription("Defines how to update which IssueComment")
        input: UpdateIssueCommentInput, dfe: DataFetchingEnvironment
    ): IssueComment {
        return issueService.updateIssueComment(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Updates an IssueComment. If the user created the IssueComment, requires READ on any of the Trackables the
        Issue is on. Otherwise, requires MODERATOR on any of the Trackables the Issue is on.
        """
    )
    @AutoPayloadType("The updated Body")
    suspend fun updateBody(
        @GraphQLDescription("Defines how to update which Body")
        input: UpdateBodyInput, dfe: DataFetchingEnvironment
    ): Body {
        return issueService.updateBody(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Deletes an IssueComment. If the user created the IssueComment, requires READ on any of the Trackables the
        Issue is on. Otherwise, requires MODERATOR on any of the Trackables the Issue is on.
        The IssueComment then is marked as deleted, its body set to "", and the referenced Artefacts are cleared.
        It is no longer possible to update the IssueComment.
        """
    )
    @AutoPayloadType("The updated IssueComment")
    suspend fun deleteIssueComment(
        @GraphQLDescription("Defines how to update which IssueComment")
        input: DeleteNodeInput, dfe: DataFetchingEnvironment
    ): IssueComment {
        return issueService.deleteIssueComment(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Creates a new Issue Board on one trackable. Requires MANAGE_ISSUE_BOARDS on the provided trackable.
        """
    )
    @AutoPayloadType("The created Issue Board")
    suspend fun createIssueBoard(
        @GraphQLDescription("Defines the created Issue Board")
        input: CreateIssueBoardInput, dfe: DataFetchingEnvironment
    ): IssueBoard {
        return issueBoardService.createIssueBoard(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription("Updates the specified Issue Board, Requires MANAGE_ISSUE_BOARDS on the provided trackable.")
    @AutoPayloadType("The updated Issue Board")
    suspend fun updateIssueBoard(
        @GraphQLDescription("Defines which Issue Board to update and how to update it")
        input: UpdateIssueBoardInput, dfe: DataFetchingEnvironment
    ): IssueBoard {
        return issueBoardService.updateIssueBoard(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Deletes the Issue Board, requires MANAGE_ISSUE_BOARDS on the provided trackable.
        """
    )
    suspend fun deleteIssueBoard(
        @GraphQLDescription("Defines which Issue Board to delete")
        input: DeleteNodeInput, dfe: DataFetchingEnvironment
    ): DeleteNodePayload {
        issueBoardService.deleteIssueBoard(dfe.gropiusAuthorizationContext, input)
        return DeleteNodePayload(input.id)
    }

    @GraphQLDescription(
        """Creates a new Issue Board Column on one trackable. Requires MANAGE_ISSUE_BOARDS on the parent trackable.
        """
    )
    @AutoPayloadType("The created Issue Board Column")
    suspend fun createIssueBoardColumn(
        @GraphQLDescription("Defines the created Issue Board Column")
        input: CreateIssueBoardColumnInput, dfe: DataFetchingEnvironment
    ): IssueBoardColumn {
        return issueBoardColumnService.createIssueBoardColumn(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription("Updates the specified Issue Board Column, Requires MANAGE_ISSUE_BOARDS on the parent trackable.")
    @AutoPayloadType("The updated Issue Board Column")
    suspend fun updateIssueBoardColumn(
        @GraphQLDescription("Defines which Issue Board Column to update and how to update it")
        input: UpdateIssueBoardColumnInput, dfe: DataFetchingEnvironment
    ): IssueBoardColumn {
        return issueBoardColumnService.updateIssueBoardColumn(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Deletes the Issue Board Column, requires MANAGE_ISSUE_BOARDS on the parent trackable.
        """
    )
    suspend fun deleteIssueBoardColumn(
        @GraphQLDescription("Defines which Issue Board Column to delete")
        input: DeleteNodeInput, dfe: DataFetchingEnvironment
    ): DeleteNodePayload {
        issueBoardColumnService.deleteIssueBoardColumn(dfe.gropiusAuthorizationContext, input)
        return DeleteNodePayload(input.id)
    }

    @GraphQLDescription(
        """Adds an Issue State to a Board Column, requires MANAGE_ISSUE_BOARDS on the parent Trackable 
        of the column the Issue State should be added to, and READ on the Issue State.
        Returns the updated column (unchanged if the state was already assigned).
        """
    )
    suspend fun addIssueStateToBoardColumn(
        @GraphQLDescription("Defines the board column ID and the issue state ID to add")
        input: AddIssueStateToBoardColumnInput,
        dfe: DataFetchingEnvironment
    ): IssueBoardColumn {
        return issueBoardColumnService.addIssueStateToBoardColumn(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Removes an Issue State from a Board Column, requires MANAGE_ISSUE_BOARDS on the parent Trackable 
           of the column the Issue State should be removed from, and READ on the Issue State.
           Returns the updated column.
        """
    )
    suspend fun removeIssueStateFromBoardColumn(
        @GraphQLDescription("Defines the board column ID and the issue state ID to remove")
        input: RemoveIssueStateFromBoardColumnInput,
        dfe: DataFetchingEnvironment
    ): IssueBoardColumn {
        return issueBoardColumnService.removeIssueStateFromBoardColumn(
            dfe.gropiusAuthorizationContext,
            input
        )
    }

    @GraphQLDescription(
        """Creates a new Issue Board Item on an Issue Board. Requires MANAGE_ISSUE_BOARDS on the parent trackable.
        """
    )
    @AutoPayloadType("The created Issue Board Item")
    suspend fun createIssueBoardItem(
        @GraphQLDescription("Defines the created Issue Board Item")
        input: CreateIssueBoardItemInput, dfe: DataFetchingEnvironment
    ): IssueBoardItem {
        return issueBoardItemService.createIssueBoardItem(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription("Updates the specified Issue Board Item, Requires MANAGE_ISSUE_BOARDS on the parent trackable.")
    @AutoPayloadType("The updated Issue Board Item")
    suspend fun updateIssueBoardItem(
        @GraphQLDescription("Defines which Issue Board Item to update and how to update it")
        input: UpdateIssueBoardItemInput, dfe: DataFetchingEnvironment
    ): IssueBoardItem {
        return issueBoardItemService.updateIssueBoardItem(dfe.gropiusAuthorizationContext, input)
    }


    @GraphQLDescription(
        """Deletes the Issue Board Item, requires MANAGE_ISSUE_BOARDS on the parent trackable.
        """
    )
    suspend fun deleteIssueBoardItem(
        @GraphQLDescription("Defines which Issue Board Item to delete")
        input: DeleteNodeInput, dfe: DataFetchingEnvironment
    ): DeleteNodePayload {
        issueBoardItemService.deleteIssueBoardItem(dfe.gropiusAuthorizationContext, input)
        return DeleteNodePayload(input.id)
    }

}