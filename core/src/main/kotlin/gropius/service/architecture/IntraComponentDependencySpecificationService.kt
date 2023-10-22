package gropius.service.architecture

import com.expediagroup.graphql.generator.scalars.ID
import gropius.authorization.GropiusAuthorizationContext
import gropius.dto.input.architecture.CreateIntraComponentDependencySpecificationInput
import gropius.dto.input.architecture.IntraComponentDependencyParticipantInput
import gropius.dto.input.architecture.UpdateIntraComponentDependencySpecificationInput
import gropius.dto.input.common.DeleteNodeInput
import gropius.dto.input.ifPresent
import gropius.dto.input.orElse
import gropius.model.architecture.*
import gropius.model.user.permission.NodePermission
import gropius.repository.architecture.*
import gropius.repository.findAllById
import gropius.repository.findById
import gropius.service.common.NamedNodeService
import io.github.graphglue.authorization.Permission
import io.github.graphglue.model.property.NodeSetPropertyDelegate
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service

/**
 * Service for [IntraComponentDependencySpecification]s. Provides functions to create, update and delete
 *
 * @param repository the associated repository used for CRUD functionality
 * @param componentVersionRepository used to get [ComponentVersion]s by id
 * @param interfaceRepository used to get [Interface]s by id
 * @param interfacePartRepository used to get [InterfacePart]s by id
 * @param intraComponentDependencyParticipantRepository used to get [IntraComponentDependencyParticipant]s by id
 */
@Service
class IntraComponentDependencySpecificationService(
    repository: IntraComponentDependencySpecificationRepository,
    private val componentVersionRepository: ComponentVersionRepository,
    private val interfaceRepository: InterfaceRepository,
    private val interfacePartRepository: InterfacePartRepository,
    private val intraComponentDependencyParticipantRepository: IntraComponentDependencyParticipantRepository
) : NamedNodeService<IntraComponentDependencySpecification, IntraComponentDependencySpecificationRepository>(repository) {

    /**
     * Creates a new [IntraComponentDependencySpecification] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines the [IntraComponentDependencySpecification]
     * @return the saved created [IntraComponentDependencySpecification]
     */
    suspend fun createIntraComponentDependencySpecification(
        authorizationContext: GropiusAuthorizationContext, input: CreateIntraComponentDependencySpecificationInput
    ): IntraComponentDependencySpecification {
        input.validate()
        val componentVersion = componentVersionRepository.findById(input.componentVersion)
        checkPermission(
            componentVersion,
            Permission(NodePermission.ADMIN, authorizationContext),
            "create IntraComponentDependencySpecifications on the specified ComponentVersion"
        )
        val intraComponentDependencySpecification = IntraComponentDependencySpecification(input.name, input.description)
        createdExtensibleNode(intraComponentDependencySpecification, input)
        intraComponentDependencySpecification.componentVersion().value = componentVersion
        intraComponentDependencySpecification.incomingParticipants() += input.incomingParticipants.map {
            createIntraComponentDependencyParticipant(it, componentVersion)
        }
        intraComponentDependencySpecification.outgoingParticipants() += input.outgoingParticipants.map {
            createIntraComponentDependencyParticipant(it, componentVersion)
        }
        return repository.save(intraComponentDependencySpecification).awaitSingle()
    }

    /**
     * Updates a [IntraComponentDependencySpecification] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [IntraComponentDependencySpecification] to update and how
     * @return the updated [IntraComponentDependencySpecification]
     */
    suspend fun updateIntraComponentDependencySpecification(
        authorizationContext: GropiusAuthorizationContext, input: UpdateIntraComponentDependencySpecificationInput
    ): IntraComponentDependencySpecification {
        input.validate()
        val intraComponentDependencySpecification = repository.findById(input.id)
        checkPermission(
            intraComponentDependencySpecification,
            Permission(NodePermission.ADMIN, authorizationContext),
            "update the IntraComponentDependencySpecification"
        )
        updateNamedNode(intraComponentDependencySpecification, input)
        updateIntraComponentDependencySpecificationParticipants(input, intraComponentDependencySpecification)
        return repository.save(intraComponentDependencySpecification).awaitSingle()
    }

    /**
     * Updates the incoming and outgoing participants of [intraComponentDependencySpecification] based on [input]
     * Does not check the authorization status
     *
     * @param input defines which participants to add/remove
     * @param intraComponentDependencySpecification the [IntraComponentDependencySpecification] where to add/remove
     *   participants
     */
    private suspend fun updateIntraComponentDependencySpecificationParticipants(
        input: UpdateIntraComponentDependencySpecificationInput,
        intraComponentDependencySpecification: IntraComponentDependencySpecification
    ) {
        val componentVersion = intraComponentDependencySpecification.componentVersion().value
        input.addedIncomingParticipants.ifPresent { addedInputs ->
            intraComponentDependencySpecification.incomingParticipants() += addedInputs.map {
                createIntraComponentDependencyParticipant(it, componentVersion)
            }
        }
        input.addedOutgoingParticipants.ifPresent { addedInputs ->
            intraComponentDependencySpecification.outgoingParticipants() += addedInputs.map {
                createIntraComponentDependencyParticipant(it, componentVersion)
            }
        }
        input.removedIncomingParticipants.ifPresent {
            removeParticipants(intraComponentDependencySpecification.incomingParticipants(), it, "incomingParticipants")
        }
        input.removedOutgoingParticipants.ifPresent {
            removeParticipants(intraComponentDependencySpecification.outgoingParticipants(), it, "outgoingParticipants")
        }
    }

    /**
     * Removes [IntraComponentDependencyParticipant] from either incoming or outgoing participants
     * Validates that all participants to remove are actually in part of the property
     *
     * @param participants the set of all current participants
     * @param toRemove ids of [IntraComponentDependencyParticipant]s to remove
     * @param propertyName the name of the property containing [participants], used for error messages
     * @throws IllegalArgumentException in case the property is empty afterwards, or an id of a participant is not found in [participants]
     */
    private suspend fun removeParticipants(
        participants: NodeSetPropertyDelegate<IntraComponentDependencyParticipant>.NodeSetProperty,
        toRemove: Collection<ID>,
        propertyName: String
    ) {
        val participantsToRemove = intraComponentDependencyParticipantRepository.findAllById(toRemove)
        for (participant in participantsToRemove) {
            if (!participants.remove(participant)) {
                throw IllegalArgumentException(
                    "Participant not present on specified IntraComponentDependencySpecification.$propertyName"
                )
            }
        }
        intraComponentDependencyParticipantRepository.deleteAll(participantsToRemove)
        if (participants.isEmpty()) {
            throw IllegalArgumentException("$propertyName must not be empty")
        }
    }

    /**
     * Creates a [IntraComponentDependencyParticipant] based on the provided [input]
     * Does not check the authorization status
     * Does not save the created [IntraComponentDependencyParticipant]
     *
     * @param input defines the [IntraComponentDependencyParticipant] to create
     * @param componentVersion the [ComponentVersion] associated with the associated [IntraComponentDependencySpecification]
     * @return the generated [IntraComponentDependencyParticipant]
     * @throws IllegalArgumentException if the input is invalid
     */
    private suspend fun createIntraComponentDependencyParticipant(
        input: IntraComponentDependencyParticipantInput, componentVersion: ComponentVersion
    ): IntraComponentDependencyParticipant {
        val relatedInterface = interfaceRepository.findById(input.`interface`)
        val interfaceDefinition = relatedInterface.interfaceDefinition().value
        if (interfaceDefinition.componentVersion().value != componentVersion) {
            throw IllegalArgumentException("The specified Interface is not part of the same ComponentVersion")
        }
        val parts = interfacePartRepository.findAllById(input.includedParts.orElse(emptyList()))
        for (part in parts) {
            if (interfaceDefinition.interfaceSpecificationVersion().value !in part.activeOn()) {
                throw IllegalArgumentException("Specified includedParts must be active on the specified Interface")
            }
        }
        val intraComponentDependencyParticipant = IntraComponentDependencyParticipant()
        intraComponentDependencyParticipant.`interface`().value = relatedInterface
        intraComponentDependencyParticipant.includedParts() += parts
        return intraComponentDependencyParticipant
    }

    /**
     * Deletes a [IntraComponentDependencySpecification] by id
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [IntraComponentDependencySpecification] to delete
     */
    suspend fun deleteIntraComponentDependencySpecification(
        authorizationContext: GropiusAuthorizationContext, input: DeleteNodeInput
    ) {
        input.validate()
        val intraComponentDependencySpecification = repository.findById(input.id)
        checkPermission(
            intraComponentDependencySpecification,
            Permission(NodePermission.ADMIN, authorizationContext),
            "delete the IntraComponentDependencySpecification"
        )
        val allParticipants = intraComponentDependencySpecification.let {
            it.outgoingParticipants() + it.incomingParticipants()
        }
        intraComponentDependencyParticipantRepository.deleteAll(allParticipants)
        repository.delete(intraComponentDependencySpecification).awaitSingleOrNull()
    }

}