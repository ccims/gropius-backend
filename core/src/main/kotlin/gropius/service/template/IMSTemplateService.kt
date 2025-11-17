package gropius.service.template

import gropius.authorization.GropiusAuthorizationContext
import gropius.dto.input.template.UpdateIMSTemplateInput
import gropius.model.template.IMSTemplate
import gropius.repository.findById
import gropius.repository.template.IMSTemplateRepository
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service

/**
 * Service for [IMSTemplate]s. Provides functions to create, update and delete
 *
 * @param repository the associated repository used for CRUD functionality
 */
@Service
class IMSTemplateService(
    repository: IMSTemplateRepository
) : AbstractTemplateService<IMSTemplate, IMSTemplateRepository>(repository) {

    /**
     * Updates an [IMSTemplate] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [IMSTemplate] to update and how
     * @return the updated [IMSTemplate]
     */
    suspend fun updateIMSTemplate(
        authorizationContext: GropiusAuthorizationContext, input: UpdateIMSTemplateInput
    ): IMSTemplate {
        input.validate()
        checkCreateTemplatePermission(authorizationContext)
        val template = repository.findById(input.id)
        updateNamedNode(template, input)
        return repository.save(template).awaitSingle()
    }

}
