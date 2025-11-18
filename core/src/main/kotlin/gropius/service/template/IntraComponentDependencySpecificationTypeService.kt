package gropius.service.template

import gropius.authorization.GropiusAuthorizationContext
import gropius.dto.input.template.UpdateIntraComponentDependencySpecificationTypeInput
import gropius.model.template.IntraComponentDependencySpecificationType
import gropius.repository.findById
import gropius.repository.template.IntraComponentDependencySpecificationTypeRepository
import gropius.service.common.NamedNodeService
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service

/**
 * Service for [IntraComponentDependencySpecificationType]s. Provides functions to create, update and delete
 *
 * @param repository the associated repository used for CRUD functionality
 */
@Service
class IntraComponentDependencySpecificationTypeService(
    repository: IntraComponentDependencySpecificationTypeRepository,
    private val baseTemplateService: BaseTemplateService<*, *>
) : NamedNodeService<IntraComponentDependencySpecificationType, IntraComponentDependencySpecificationTypeRepository>(repository) {

    /**
     * Updates an [IntraComponentDependencySpecificationType] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [IntraComponentDependencySpecificationType] to update and how
     * @return the updated [IntraComponentDependencySpecificationType]
     */
    suspend fun updateIntraComponentDependencySpecificationType(
        authorizationContext: GropiusAuthorizationContext, input: UpdateIntraComponentDependencySpecificationTypeInput
    ): IntraComponentDependencySpecificationType {
        input.validate()
        baseTemplateService.checkCreateTemplatePermission(authorizationContext)
        val type = repository.findById(input.id)
        updateNamedNode(type, input)
        return repository.save(type).awaitSingle()
    }

}
