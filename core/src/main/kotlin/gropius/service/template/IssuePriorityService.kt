package gropius.service.template

import gropius.authorization.GropiusAuthorizationContext
import gropius.dto.input.ifPresent
import gropius.dto.input.template.UpdateIssuePriorityInput
import gropius.model.template.IssuePriority
import gropius.repository.findById
import gropius.repository.template.IssuePriorityRepository
import gropius.service.common.NamedNodeService
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service

/**
 * Service for [IssuePriority]s. Provides functions to create, update and delete
 *
 * @param repository the associated repository used for CRUD functionality
 */
@Service
class IssuePriorityService(
    repository: IssuePriorityRepository,
    private val baseTemplateService: BaseTemplateService<*, *>
) : NamedNodeService<IssuePriority, IssuePriorityRepository>(repository) {

    /**
     * Updates an [IssuePriority] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [IssuePriority] to update and how
     * @return the updated [IssuePriority]
     */
    suspend fun updateIssuePriority(
        authorizationContext: GropiusAuthorizationContext, input: UpdateIssuePriorityInput
    ): IssuePriority {
        input.validate()
        baseTemplateService.checkCreateTemplatePermission(authorizationContext)
        val issuePriority = repository.findById(input.id)
        updateNamedNode(issuePriority, input)
        input.value.ifPresent {
            issuePriority.value = it
        }
        return repository.save(issuePriority).awaitSingle()
    }

}
