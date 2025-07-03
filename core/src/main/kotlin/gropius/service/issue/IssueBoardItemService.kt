package gropius.service.issue

import gropius.authorization.GropiusAuthorizationContext
import gropius.dto.input.issue.CreateIssueBoardItemInput
import gropius.model.issue.IssueBoardItem
import gropius.model.user.permission.TrackablePermission
import gropius.repository.findById
import gropius.repository.issue.IssueBoardItemRepository
import gropius.repository.issue.IssueBoardRepository
import gropius.repository.issue.IssueRepository
import io.github.graphglue.authorization.Permission
import kotlinx.coroutines.reactor.awaitSingle
import gropius.dto.input.common.DeleteNodeInput
import gropius.dto.input.ifPresent
import gropius.dto.input.issue.UpdateIssueBoardItemInput
import gropius.service.common.NodeService
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service

@Service
class IssueBoardItemService(
   repository: IssueBoardItemRepository,
   private val issueBoardRepository: IssueBoardRepository,
   private val issueRepository: IssueRepository

): NodeService<IssueBoardItem, IssueBoardItemRepository>(repository){

   /**
    * Creates a new [IssueBoardItem] based on the provided [input]
    * Checks the authorization status
    *
    * @param authorizationContext used to check for the required permission
    * @param input defines the [IssueBoardItem]
    * @return the saved created [IssueBoardItem]
    */
   suspend fun createIssueBoardItem(
      authorizationContext: GropiusAuthorizationContext,
      input: CreateIssueBoardItemInput
   ): IssueBoardItem {
      input.validate()
      val issueBoard = issueBoardRepository.findById(input.issueBoard)
      val issue  = issueRepository.findById(input.issue)

      val trackable = issueBoard.trackable().value
      checkPermission(
         trackable,
         Permission(TrackablePermission.MANAGE_ISSUES, authorizationContext),
         "create issue board items"
      )
      if(trackable !in issue.trackables()){
         throw IllegalArgumentException("trackable is not present in the issue's trackables")
      }

      val item = IssueBoardItem(
         position = input.position
      ).apply {
        issueBoard().value = issueBoard
         issue().value = issue
      }
      return repository.save(item).awaitSingle()
   }


   /**
    * Updates an [IssueBoardItem] based on the provided [input]
    * Checks the authorization status
    *
    * @param authorizationContext used to check for the required permission
    * @param input defines which [IssueBoardItem] to update and how
    * @return the updated [IssueBoardItem]
    */
   suspend fun updateIssueBoardItem(authorizationContext: GropiusAuthorizationContext, input: UpdateIssueBoardItemInput): IssueBoardItem {
      input.validate()
      val item = repository.findById(input.id)
      val issueBoard = item.issueBoard().value
      val trackable = issueBoard.trackable().value
      checkPermission(
         trackable,
         Permission(TrackablePermission.MANAGE_ISSUES, authorizationContext),
         "move Issue Board Items"
      )

      input.position.ifPresent{item.position = it}
      return repository.save(item).awaitSingle()
   }


   /**
    * Removes an [IssueBoardItem] from [repository]
    * Checks the authorization status
    *
    * @param authorizationContext used to check for the required permission
    * @param input defines which [IssueBoardItem] to delete
    */
   suspend fun deleteIssueBoardItem(authorizationContext: GropiusAuthorizationContext,input: DeleteNodeInput){
      input.validate()
      val item = repository.findById(input.id)
      val issueBoard = item.issueBoard().value
      val trackable = issueBoard.trackable().value

      checkPermission(
         trackable,
         Permission(TrackablePermission.MANAGE_ISSUES, authorizationContext),
         "delete IssueBoard items"
      )

      repository.delete(item).awaitSingleOrNull()
   }


}