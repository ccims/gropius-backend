package gropius.service.misc

import gropius.authorization.GropiusAuthorizationContext
import gropius.dto.input.common.DeleteNodeInput
import gropius.dto.input.ifPresent
import gropius.dto.input.misc.CreateLegalInformationInput
import gropius.dto.input.misc.UpdateLegalInformationInput
import gropius.model.misc.LegalInformation
import gropius.repository.findById
import gropius.repository.misc.LegalInformationRepository
import gropius.service.common.NodeService
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service

/**
 * Service for [LegalInformation]s. Provides function to create, update and delete
 *
 * @param repository the associated repository used for CRUD functionality
 */
@Service
class LegalInformationService(
    repository: LegalInformationRepository
) : NodeService<LegalInformation, LegalInformationRepository>(repository) {

    /**
     * Creates a new [LegalInformation] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required isAdmin
     * @param input defines the [LegalInformation]
     * @return the saved created [LegalInformation]
     */
    suspend fun createLegalInformation(
        authorizationContext: GropiusAuthorizationContext, input: CreateLegalInformationInput
    ): LegalInformation {
        input.validate()
        checkIsAdmin(authorizationContext, "create LegalInformation")
        val legalInformation = LegalInformation(input.label, input.text, input.priority)
        return repository.save(legalInformation).awaitSingle()
    }

    /**
     * Updates a [LegalInformation] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required isAdmin
     * @param input defines which [LegalInformation] to update and how
     * @return the updated [LegalInformation]
     */
    suspend fun updateLegalInformation(
        authorizationContext: GropiusAuthorizationContext, input: UpdateLegalInformationInput
    ): LegalInformation {
        input.validate()
        checkIsAdmin(authorizationContext, "update LegalInformation")
        val legalInformation = repository.findById(input.id)
        input.label.ifPresent {
            legalInformation.label = it
        }
        input.text.ifPresent {
            legalInformation.text = it
        }
        input.priority.ifPresent {
            legalInformation.priority = it
        }
        return repository.save(legalInformation).awaitSingle()
    }

    /**
     * Deletes a [LegalInformation] by id
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required isAdmin
     * @param input defines which [LegalInformation] to delete
     */
    suspend fun deleteLegalInformation(
        authorizationContext: GropiusAuthorizationContext, input: DeleteNodeInput
    ) {
        input.validate()
        checkIsAdmin(authorizationContext, "delete LegalInformation")
        val legalInformation = repository.findById(input.id)
        repository.delete(legalInformation).awaitSingleOrNull()
    }

}