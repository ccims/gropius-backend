package gropius.service.template

import gropius.authorization.GropiusAuthorizationContext
import gropius.dto.input.ifPresent
import gropius.dto.input.template.UpdateIssueTypeInput
import gropius.model.template.IssueType
import gropius.repository.findById
import gropius.repository.template.IssueTypeRepository
import gropius.service.common.NamedNodeService
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service

/**
 * Service for [IssueType]s. Provides functions to create, update and delete
 *
 * @param repository the associated repository used for CRUD functionality
 */
@Service
class IssueTypeService(
    repository: IssueTypeRepository,
    private val baseTemplateService: BaseTemplateService<*, *>
) : NamedNodeService<IssueType, IssueTypeRepository>(repository) {

    /**
     * Updates an [IssueType] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [IssueType] to update and how
     * @return the updated [IssueType]
     */
    suspend fun updateIssueType(
        authorizationContext: GropiusAuthorizationContext, input: UpdateIssueTypeInput
    ): IssueType {
        input.validate()
        baseTemplateService.checkCreateTemplatePermission(authorizationContext)
        val issueType = repository.findById(input.id)
        updateNamedNode(issueType, input)
        input.iconPath.ifPresent {
            issueType.iconPath = it
        }
        return repository.save(issueType).awaitSingle()
    }

}
