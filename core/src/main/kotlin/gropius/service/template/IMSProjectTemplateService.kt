package gropius.service.template

import gropius.authorization.GropiusAuthorizationContext
import gropius.dto.input.template.UpdateIMSProjectTemplateInput
import gropius.model.template.IMSProjectTemplate
import gropius.repository.findById
import gropius.repository.template.IMSProjectTemplateRepository
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service

/**
 * Service for [IMSProjectTemplate]s. Provides functions to update
 *
 * @param repository the repository used for CRUD functionality
 */
@Service
class IMSProjectTemplateService(
    repository: IMSProjectTemplateRepository
) : BaseTemplateService<IMSProjectTemplate, IMSProjectTemplateRepository>(repository) {

    /**
     * Updates an [IMSProjectTemplate] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [IMSProjectTemplate] to update and how
     * @return the updated [IMSProjectTemplate]
     */
    suspend fun updateIMSProjectTemplate(
        authorizationContext: GropiusAuthorizationContext, input: UpdateIMSProjectTemplateInput
    ): IMSProjectTemplate {
        input.validate()
        checkCreateTemplatePermission(authorizationContext)
        val template = repository.findById(input.id)
        updateNamedNode(template, input)
        return repository.save(template).awaitSingle()
    }

}
