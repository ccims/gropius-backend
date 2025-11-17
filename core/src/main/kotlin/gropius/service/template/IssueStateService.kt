package gropius.service.template

import gropius.authorization.GropiusAuthorizationContext
import gropius.dto.input.template.UpdateIssueStateInput
import gropius.model.template.IssueState
import gropius.repository.findById
import gropius.repository.template.IssueStateRepository
import gropius.service.common.NamedNodeService
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service

/**
 * Service for [IssueState]s. Provides functions to create, update and delete
 *
 * @param repository the associated repository used for CRUD functionality
 */
@Service
class IssueStateService(
    repository: IssueStateRepository,
    private val baseTemplateService: BaseTemplateService<*, *>
) : NamedNodeService<IssueState, IssueStateRepository>(repository) {

    /**
     * Updates an [IssueState] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [IssueState] to update and how
     * @return the updated [IssueState]
     */
    suspend fun updateIssueState(
        authorizationContext: GropiusAuthorizationContext, input: UpdateIssueStateInput
    ): IssueState {
        input.validate()
        baseTemplateService.checkCreateTemplatePermission(authorizationContext)
        val issueState = repository.findById(input.id)
        updateNamedNode(issueState, input)
        return repository.save(issueState).awaitSingle()
    }

}
