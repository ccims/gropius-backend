package gropius.service.template

import gropius.authorization.GropiusAuthorizationContext
import gropius.dto.input.template.UpdateInterfacePartTemplateInput
import gropius.model.template.InterfacePartTemplate
import gropius.repository.findById
import gropius.repository.template.InterfacePartTemplateRepository
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service

/**
 * Service for [InterfacePartTemplate]s. Provides functions to update
 *
 * @param repository the repository used for CRUD functionality
 */
@Service
class InterfacePartTemplateService(
    repository: InterfacePartTemplateRepository
) : BaseTemplateService<InterfacePartTemplate, InterfacePartTemplateRepository>(repository) {

    /**
     * Updates an [InterfacePartTemplate] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [InterfacePartTemplate] to update and how
     * @return the updated [InterfacePartTemplate]
     */
    suspend fun updateInterfacePartTemplate(
        authorizationContext: GropiusAuthorizationContext, input: UpdateInterfacePartTemplateInput
    ): InterfacePartTemplate {
        input.validate()
        checkCreateTemplatePermission(authorizationContext)
        val template = repository.findById(input.id)
        updateNamedNode(template, input)
        return repository.save(template).awaitSingle()
    }

}
