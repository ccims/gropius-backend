package gropius.service.template

import gropius.authorization.GropiusAuthorizationContext
import gropius.dto.input.template.UpdateInterfaceSpecificationVersionTemplateInput
import gropius.model.template.InterfaceSpecificationVersionTemplate
import gropius.repository.findById
import gropius.repository.template.InterfaceSpecificationVersionTemplateRepository
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service

/**
 * Service for [InterfaceSpecificationVersionTemplate]s. Provides functions to update
 *
 * @param repository the repository used for CRUD functionality
 */
@Service
class InterfaceSpecificationVersionTemplateService(
    repository: InterfaceSpecificationVersionTemplateRepository
) : BaseTemplateService<InterfaceSpecificationVersionTemplate, InterfaceSpecificationVersionTemplateRepository>(repository) {

    /**
     * Updates an [InterfaceSpecificationVersionTemplate] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [InterfaceSpecificationVersionTemplate] to update and how
     * @return the updated [InterfaceSpecificationVersionTemplate]
     */
    suspend fun updateInterfaceSpecificationVersionTemplate(
        authorizationContext: GropiusAuthorizationContext, input: UpdateInterfaceSpecificationVersionTemplateInput
    ): InterfaceSpecificationVersionTemplate {
        input.validate()
        checkCreateTemplatePermission(authorizationContext)
        val template = repository.findById(input.id)
        updateNamedNode(template, input)
        return repository.save(template).awaitSingle()
    }

}
