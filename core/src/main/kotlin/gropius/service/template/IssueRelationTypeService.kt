package gropius.service.template

import gropius.authorization.GropiusAuthorizationContext
import gropius.dto.input.ifPresent
import gropius.dto.input.template.UpdateIssueRelationTypeInput
import gropius.model.template.IssueRelationType
import gropius.repository.findById
import gropius.repository.template.IssueRelationTypeRepository
import gropius.service.common.NamedNodeService
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service

/**
 * Service for [IssueRelationType]s. Provides functions to create, update and delete
 *
 * @param repository the associated repository used for CRUD functionality
 */
@Service
class IssueRelationTypeService(
    repository: IssueRelationTypeRepository,
    private val baseTemplateService: BaseTemplateService<*, *>
) : NamedNodeService<IssueRelationType, IssueRelationTypeRepository>(repository) {

    /**
     * Updates an [IssueRelationType] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [IssueRelationType] to update and how
     * @return the updated [IssueRelationType]
     */
    suspend fun updateIssueRelationType(
        authorizationContext: GropiusAuthorizationContext, input: UpdateIssueRelationTypeInput
    ): IssueRelationType {
        input.validate()
        baseTemplateService.checkCreateTemplatePermission(authorizationContext)
        val issueRelationType = repository.findById(input.id)
        updateNamedNode(issueRelationType, input)
        input.inverseName.ifPresent {
            issueRelationType.inverseName = it
        }
        return repository.save(issueRelationType).awaitSingle()
    }

}
