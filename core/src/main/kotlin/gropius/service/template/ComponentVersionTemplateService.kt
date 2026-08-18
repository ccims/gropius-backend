package gropius.service.template

import gropius.authorization.GropiusAuthorizationContext
import gropius.dto.input.template.UpdateComponentVersionTemplateInput
import gropius.model.template.ComponentVersionTemplate
import gropius.repository.findById
import gropius.repository.template.ComponentVersionTemplateRepository
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service

/**
 * Service for [ComponentVersionTemplate]s. Provides functions to update
 *
 * @param repository the repository used for CRUD functionality
 */
@Service
class ComponentVersionTemplateService(
    repository: ComponentVersionTemplateRepository
) : BaseTemplateService<ComponentVersionTemplate, ComponentVersionTemplateRepository>(repository) {

    /**
     * Updates a [ComponentVersionTemplate] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [ComponentVersionTemplate] to update and how
     * @return the updated [ComponentVersionTemplate]
     */
    suspend fun updateComponentVersionTemplate(
        authorizationContext: GropiusAuthorizationContext, input: UpdateComponentVersionTemplateInput
    ): ComponentVersionTemplate {
        input.validate()
        checkCreateTemplatePermission(authorizationContext)
        val template = repository.findById(input.id)
        updateNamedNode(template, input)
        return repository.save(template).awaitSingle()
    }

}
