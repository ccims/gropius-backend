package gropius.service.issue

import gropius.authorization.GropiusAuthorizationContext
import gropius.dto.input.common.DeleteNodeInput
import gropius.dto.input.ifPresent
import gropius.dto.input.issue.AddIssueStateToBoardColumnInput
import gropius.dto.input.issue.CreateIssueBoardColumnInput
import gropius.dto.input.issue.RemoveIssueStateFromBoardColumnInput
import gropius.dto.input.issue.UpdateIssueBoardColumnInput
import gropius.model.issue.IssueBoardColumn
import gropius.model.issue.IssueBoard
import gropius.model.template.IssueState
import gropius.model.user.permission.TrackablePermission
import gropius.repository.findById
import gropius.repository.issue.IssueBoardColumnRepository
import gropius.repository.issue.IssueBoardRepository
import gropius.repository.template.IssueStateRepository
import gropius.service.common.NamedNodeService
import io.github.graphglue.authorization.Permission
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service

/**
 * Service for [IssueBoardColumn]s. Provides functions to create, update and delete
 *
 * @param repository the associated repository used for CRUD functionality
 * @param issueStateRepository used to find [IssueState]s by id
 * @param issueBoardRepository used to find [IssueBoard]s by id
 */
@Service
class IssueBoardColumnService(
    repository: IssueBoardColumnRepository,
    private val issueStateRepository: IssueStateRepository,
    private val issueBoardRepository: IssueBoardRepository
) : NamedNodeService<IssueBoardColumn, IssueBoardColumnRepository>(repository) {
    /**
     * Creates a new [IssueBoardColumn] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines the [IssueBoardColumn]
     * @return the saved created [IssueBoardColumn]
     */
    suspend fun createIssueBoardColumn(
        authorizationContext: GropiusAuthorizationContext,
        input: CreateIssueBoardColumnInput
    ): IssueBoardColumn {
        input.validate()
        val issueBoard = issueBoardRepository.findById(input.issueBoard)
        val trackable = issueBoard.trackable().value

        checkPermission(
            trackable,
            Permission(TrackablePermission.MANAGE_ISSUE_BOARDS, authorizationContext),
            "create Issue Board Columns on ${trackable.rawId}"
        )

        val issueBoardColumn = IssueBoardColumn(input.name, input.description, input.position)
        issueBoardColumn.issueBoard().value = issueBoard
        return repository.save(issueBoardColumn).awaitSingle()
    }

    /**
     * Updates an [IssueBoardColumn] based on the provided [input]
     * checks MANAGE_ISSUE_BOARDS on the parent trackable
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [IssueBoardColumn] to update and how
     * @return the updated [IssueBoardColumn]
     */
    suspend fun updateIssueBoardColumn(
        authorizationContext: GropiusAuthorizationContext,
        input: UpdateIssueBoardColumnInput
    ): IssueBoardColumn {
        input.validate()
        val issueBoardColumn = repository.findById(input.id)
        val issueBoard = issueBoardColumn.issueBoard().value
        checkPermission(
            issueBoard,
            Permission(TrackablePermission.MANAGE_ISSUE_BOARDS, authorizationContext),
            "manage Issue Boards in its Trackable"
        )

        updateNamedNode(issueBoardColumn, input)
        input.position.ifPresent { issueBoardColumn.position = it }
        return repository.save(issueBoardColumn).awaitSingle()
    }

    /**
     * Deletes an [IssueBoardColumn]
     * removes it from its parent IssueBoard’s columns
     * checks MANAGE_ISSUE_BOARDS on the parent trackable
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [IssueBoardColumn] to delete
     */
    suspend fun deleteIssueBoardColumn(authorizationContext: GropiusAuthorizationContext, input: DeleteNodeInput) {
        input.validate()
        val column = repository.findById(input.id)
        val board = column.issueBoard().value
        checkPermission(
            board,
            Permission(TrackablePermission.MANAGE_ISSUE_BOARDS, authorizationContext),
            "manage Issue Boards in a Trackable"
        )

        repository.delete(column).awaitSingleOrNull()
    }

    /**
     * Adds an [IssueState] to an [IssueBoardColumn].
     * Checks MANAGE_ISSUE_BOARDS on the parent trackable.
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [IssueState] to add to which [IssueBoardColumn]
     * @return the saved updated [IssueBoardColumn]
     */
    suspend fun addIssueStateToBoardColumn(
        authorizationContext: GropiusAuthorizationContext,
        input: AddIssueStateToBoardColumnInput
    ): IssueBoardColumn {
        input.validate()
        val column = repository.findById(input.column)
        val state = issueStateRepository.findById(input.state)
        val issueBoard = column.issueBoard().value

        checkPermission(
            issueBoard,
            Permission(TrackablePermission.MANAGE_ISSUE_BOARDS, authorizationContext),
            "manage Issue Boards in a Trackable"
        )

        val isAssigned = issueBoard.issueBoardColumns().any { column -> state in column.issueStates() }
        if (state !in column.issueStates()) {
            if (isAssigned) {
                throw IllegalStateException("Issue State is already assigned to another column of this Issue Board")
            }
            column.issueStates().add(state)
            return repository.save(column).awaitSingle()
        }
        return column
    }

    /**
     * Removes an [IssueState] from an [IssueBoardColumn].
     * Checks MANAGE_ISSUE_BOARDS on the parent trackable.
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [IssueState] to remove from which [IssueBoardColumn]
     * @return the saved updated [IssueBoardColumn]
     */
    suspend fun removeIssueStateFromBoardColumn(
        authorizationContext: GropiusAuthorizationContext,
        input: RemoveIssueStateFromBoardColumnInput
    ): IssueBoardColumn {
        input.validate()
        val column = repository.findById(input.column)
        val state = issueStateRepository.findById(input.state)
        val issueBoard = column.issueBoard().value
        checkPermission(
            issueBoard,
            Permission(TrackablePermission.MANAGE_ISSUE_BOARDS, authorizationContext),
            "manage Issue Boards in a Trackable"
        )

        if (state in column.issueStates()) {
            column.issueStates().remove(state)
            return repository.save(column).awaitSingle()
        }
        return column
    }
}
