package gropius.service.architecture

import gropius.authorization.GropiusAuthorizationContext
import gropius.dto.input.architecture.CreateInterfacePartInput
import gropius.dto.input.architecture.InterfacePartInput
import gropius.dto.input.architecture.UpdateInterfacePartInput
import gropius.dto.input.common.DeleteNodeInput
import gropius.model.architecture.InterfacePart
import gropius.model.architecture.InterfaceSpecification
import gropius.model.architecture.InterfaceSpecificationVersion
import gropius.model.user.permission.NodePermission
import gropius.repository.architecture.InterfacePartRepository
import gropius.repository.architecture.InterfaceSpecificationRepository
import gropius.repository.architecture.InterfaceSpecificationVersionRepository
import gropius.repository.common.NodeRepository
import gropius.repository.findById
import gropius.service.issue.IssueAggregationUpdater
import gropius.service.template.TemplatedNodeService
import io.github.graphglue.authorization.Permission
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service

/**
 * Service for [InterfacePart]s. Provides functions to create, update and delete
 *
 * @param repository the associated repository used for CRUD functionality
 * @param interfaceSpecificationRepository used get [InterfaceSpecification] by id
 * @param interfaceSpecificationVersionRepository used get [InterfaceSpecificationVersion] by id
 * @param templatedNodeService used to update templatedFields
 * @param nodeRepository used to update/delete nodes
 */
@Service
class InterfacePartService(
    repository: InterfacePartRepository,
    private val interfaceSpecificationRepository: InterfaceSpecificationRepository,
    private val interfaceSpecificationVersionRepository: InterfaceSpecificationVersionRepository,
    private val templatedNodeService: TemplatedNodeService,
    private val nodeRepository: NodeRepository
) : NamedAffectedByIssueService<InterfacePart, InterfacePartRepository>(repository) {

    /**
     * Creates a new [InterfacePart] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines the [InterfacePart]
     * @return the saved created [InterfacePart]
     */
    suspend fun createInterfacePart(
        authorizationContext: GropiusAuthorizationContext, input: CreateInterfacePartInput
    ): InterfacePart {
        input.validate()
        val interfaceSpecificationVersion = interfaceSpecificationVersionRepository.findById(input.interfaceSpecificationVersion)
        val interfaceSpecification = interfaceSpecificationVersion.interfaceSpecification().value
        checkPermission(
            interfaceSpecification,
            Permission(NodePermission.ADMIN, authorizationContext),
            "create InterfaceParts on the InterfaceSpecification"
        )
        val interfacePart = createInterfacePart(interfaceSpecification, input)
        interfacePart.partOf().value = interfaceSpecificationVersion
        return repository.save(interfacePart).awaitSingle()
    }

    /**
     * Creates a new [InterfacePart] based on the provided [input]
     * Does not check the authorization status, does not save the created nodes
     * Validates the [input]
     *
     * @param interfaceSpecification the [InterfaceSpecification] the created [InterfacePart] is part of
     * @param input defines the [InterfacePart]
     * @return the created [InterfacePart]
     */
    suspend fun createInterfacePart(
        interfaceSpecification: InterfaceSpecification, input: InterfacePartInput
    ): InterfacePart {
        input.validate()
        val template = interfaceSpecification.template().value.interfacePartTemplate().value
        val templatedFields = templatedNodeService.validateInitialTemplatedFields(template, input)
        val interfacePart = InterfacePart(input.name, input.description, templatedFields)
        interfacePart.template().value = template
        return interfacePart
    }

    /**
     * Updates a [InterfacePart] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [InterfacePart] to update and how
     * @return the updated [InterfacePart]
     */
    suspend fun updateInterfacePart(
        authorizationContext: GropiusAuthorizationContext, input: UpdateInterfacePartInput
    ): InterfacePart {
        input.validate()
        val interfacePart = repository.findById(input.id)
        checkPermission(
            interfacePart, Permission(NodePermission.ADMIN, authorizationContext), "update the InterfacePart"
        )
        templatedNodeService.updateTemplatedFields(interfacePart, input, false)
        updateNamedAffectedByIssue(interfacePart, input)
        return repository.save(interfacePart).awaitSingle()
    }

    /**
     * Deletes a [InterfacePart] by id
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [InterfacePart] to delete
     */
    suspend fun deleteInterfacePart(
        authorizationContext: GropiusAuthorizationContext, input: DeleteNodeInput
    ) {
        input.validate()
        val interfacePart = repository.findById(input.id)
        checkPermission(
            interfacePart, Permission(NodePermission.ADMIN, authorizationContext), "delete the InterfacePart"
        )
        val issueAggregationUpdater = IssueAggregationUpdater()
        issueAggregationUpdater.deletedNodes += interfacePart
        issueAggregationUpdater.deletedInterfacePart(interfacePart)
        issueAggregationUpdater.save(nodeRepository)
    }

}