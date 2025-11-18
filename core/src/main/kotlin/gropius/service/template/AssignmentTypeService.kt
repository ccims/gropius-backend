package gropius.service.template

import gropius.authorization.GropiusAuthorizationContext
import gropius.dto.input.template.UpdateAssignmentTypeInput
import gropius.model.template.AssignmentType
import gropius.repository.findById
import gropius.repository.template.AssignmentTypeRepository
import gropius.service.common.NamedNodeService
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service

/**
 * Service for [AssignmentType]s. Provides functions to create, update and delete
 * [AssignmentType] entities.
 *
 * @param repository the associated repository used for CRUD functionality
 * @param baseTemplateService service used for permission checks
 */
@Service
class AssignmentTypeService(
    repository: AssignmentTypeRepository,
    private val baseTemplateService: BaseTemplateService<*, *>
) : NamedNodeService<AssignmentType, AssignmentTypeRepository>(repository) {

    /**
     * Updates an [AssignmentType] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [AssignmentType] to update and how
     * @return the updated [AssignmentType]
     */
    suspend fun updateAssignmentType(
        authorizationContext: GropiusAuthorizationContext, input: UpdateAssignmentTypeInput
    ): AssignmentType {
        input.validate()
        baseTemplateService.checkCreateTemplatePermission(authorizationContext)
        val assignmentType = repository.findById(input.id)
        updateNamedNode(assignmentType, input)
        return repository.save(assignmentType).awaitSingle()
    }

}
