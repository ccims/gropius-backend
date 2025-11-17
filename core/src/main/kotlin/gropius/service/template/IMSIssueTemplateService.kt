package gropius.service.template

import gropius.authorization.GropiusAuthorizationContext
import gropius.dto.input.template.UpdateIMSIssueTemplateInput
import gropius.model.template.IMSIssueTemplate
import gropius.repository.findById
import gropius.repository.template.IMSIssueTemplateRepository
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service

/**
 * Service for [IMSIssueTemplate]s. Provides functions to update
 *
 * @param repository the repository used for CRUD functionality
 */
@Service
class IMSIssueTemplateService(
    repository: IMSIssueTemplateRepository
) : BaseTemplateService<IMSIssueTemplate, IMSIssueTemplateRepository>(repository) {

    /**
     * Updates an [IMSIssueTemplate] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [IMSIssueTemplate] to update and how
     * @return the updated [IMSIssueTemplate]
     */
    suspend fun updateIMSIssueTemplate(
        authorizationContext: GropiusAuthorizationContext, input: UpdateIMSIssueTemplateInput
    ): IMSIssueTemplate {
        input.validate()
        checkCreateTemplatePermission(authorizationContext)
        val template = repository.findById(input.id)
        updateNamedNode(template, input)
        return repository.save(template).awaitSingle()
    }

}
