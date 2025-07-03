package gropius.service.issue

import gropius.authorization.GropiusAuthorizationContext
import gropius.dto.input.common.DeleteNodeInput
import gropius.dto.input.issue.CreateIssueBoardInput
import gropius.dto.input.issue.UpdateIssueBoardInput
import gropius.model.issue.IssueBoard
import gropius.model.user.permission.TrackablePermission
import gropius.repository.architecture.TrackableRepository
import gropius.repository.common.NodeRepository
import gropius.repository.findById
import gropius.repository.issue.IssueBoardRepository
import gropius.service.common.NamedNodeService
import io.github.graphglue.authorization.Permission
import io.github.graphglue.model.Node
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service

@Service
class IssueBoardService(
    repository: IssueBoardRepository,
    private val trackableRepository: TrackableRepository,
    private val nodeRepository: NodeRepository


):NamedNodeService<IssueBoard,IssueBoardRepository>(repository){


    /**
     * Creates a new [IssueBoard] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines the [IssueBoard]
     * @return the saved created [IssueBoard]
     */
    suspend fun createIssueBoard(authorizationContext: GropiusAuthorizationContext, input: CreateIssueBoardInput): IssueBoard {
        input.validate()
        val trackable = trackableRepository.findById(input.trackable)

            checkPermission(
                trackable,
                Permission(TrackablePermission.MANAGE_ISSUE_BOARDS, authorizationContext),
                "create Issue Boards on ${trackable.rawId}"
            )

        val issueBoard = IssueBoard(input.name, input.description)
        issueBoard.trackable().value=trackable
        return repository.save(issueBoard).awaitSingle()
    }


    /**
     * Updates an [IssueBoard] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [IssueBoard] to update and how
     * @return the updated [IssueBoard]
     */
    suspend fun updateIssueBoard(authorizationContext: GropiusAuthorizationContext, input: UpdateIssueBoardInput): IssueBoard {
        input.validate()
        val issueBoard = repository.findById(input.id)
        checkPermission(
            issueBoard,
            Permission(TrackablePermission.MANAGE_ISSUE_BOARDS, authorizationContext),
            "manage Issue Boards in its Trackable"
        )

        updateNamedNode(issueBoard, input)
        return repository.save(issueBoard).awaitSingle()
    }


    /**
     * Removes an [IssueBoard] from [repository]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [IssueBoard] to delete
     */
    suspend fun deleteIssueBoard(authorizationContext: GropiusAuthorizationContext, input: DeleteNodeInput) {
        input.validate()
        val issueBoard = repository.findById(input.id)
        val trackable = issueBoard.trackable().value

            checkPermission(
                trackable,
                Permission(TrackablePermission.MANAGE_ISSUE_BOARDS, authorizationContext),
                "manage Issue Boards in a Trackable"
            )

        val toDelete = mutableSetOf<Node>()
        toDelete+= issueBoard.issueBoardColumns()
        toDelete+= issueBoard.issueBoardItems()
        toDelete+= issueBoard
       nodeRepository.deleteAll(toDelete).awaitSingleOrNull()
    }



}

