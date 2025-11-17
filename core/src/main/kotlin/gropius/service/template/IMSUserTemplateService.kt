package gropius.service.template

import gropius.authorization.GropiusAuthorizationContext
import gropius.dto.input.template.UpdateIMSUserTemplateInput
import gropius.model.template.IMSUserTemplate
import gropius.repository.findById
import gropius.repository.template.IMSUserTemplateRepository
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service

/**
 * Service for [IMSUserTemplate]s. Provides functions to update
 *
 * @param repository the repository used for CRUD functionality
 */
@Service
class IMSUserTemplateService(
    repository: IMSUserTemplateRepository
) : BaseTemplateService<IMSUserTemplate, IMSUserTemplateRepository>(repository) {

    /**
     * Updates an [IMSUserTemplate] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [IMSUserTemplate] to update and how
     * @return the updated [IMSUserTemplate]
     */
    suspend fun updateIMSUserTemplate(
        authorizationContext: GropiusAuthorizationContext, input: UpdateIMSUserTemplateInput
    ): IMSUserTemplate {
        input.validate()
        checkCreateTemplatePermission(authorizationContext)
        val template = repository.findById(input.id)
        updateNamedNode(template, input)
        return repository.save(template).awaitSingle()
    }

}
