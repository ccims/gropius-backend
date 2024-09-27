package gropius.service.architecture

import gropius.authorization.GropiusAuthorizationContext
import gropius.dto.input.architecture.CreateInterfaceSpecificationVersionInput
import gropius.dto.input.architecture.InterfaceSpecificationVersionInput
import gropius.dto.input.architecture.UpdateInterfaceSpecificationVersionInput
import gropius.dto.input.common.DeleteNodeInput
import gropius.dto.input.ifPresent
import gropius.model.architecture.Component
import gropius.model.architecture.InterfacePart
import gropius.model.architecture.InterfaceSpecification
import gropius.model.architecture.InterfaceSpecificationVersion
import gropius.model.user.permission.NodePermission
import gropius.repository.architecture.ComponentRepository
import gropius.repository.architecture.InterfaceSpecificationRepository
import gropius.repository.architecture.InterfaceSpecificationVersionRepository
import gropius.repository.common.NodeRepository
import gropius.repository.findById
import gropius.service.common.NodeService
import gropius.service.template.TemplatedNodeService
import io.github.graphglue.authorization.Permission
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service

/**
 * Service for [InterfaceSpecificationVersion]s. Provides functions to create, update and delete
 *
 * @param repository the associated repository used for CRUD functionality
 * @param componentRepository used to get [Component]s
 * @param interfacePartService used to get [InterfacePart]s by id
 * @param templatedNodeService used to update templatedFields
 * @param nodeRepository used to save/delete any node
 * @param interfaceSpecificationRepository used get [InterfaceSpecification] by id
 */
@Service
class InterfaceSpecificationVersionService(
    repository: InterfaceSpecificationVersionRepository,
    private val componentRepository: ComponentRepository,
    private val interfacePartService: InterfacePartService,
    private val templatedNodeService: TemplatedNodeService,
    private val nodeRepository: NodeRepository,
    private val interfaceSpecificationRepository: InterfaceSpecificationRepository
) : NodeService<InterfaceSpecificationVersion, InterfaceSpecificationVersionRepository>(
    repository
) {

    /**
     * Creates a new [InterfaceSpecificationVersion] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines the [InterfaceSpecificationVersion]
     * @return the saved created [InterfaceSpecificationVersion]
     */
    suspend fun createInterfaceSpecificationVersion(
        authorizationContext: GropiusAuthorizationContext, input: CreateInterfaceSpecificationVersionInput
    ): InterfaceSpecificationVersion {
        input.validate()
        val interfaceSpecification = interfaceSpecificationRepository.findById(input.interfaceSpecification)
        checkPermission(
            interfaceSpecification,
            Permission(NodePermission.ADMIN, authorizationContext),
            "create InterfaceSpecificationVersions on the InterfaceSpecification"
        )
        val interfaceSpecificationVersion = createInterfaceSpecificationVersion(interfaceSpecification, input)
        interfaceSpecificationVersion.interfaceSpecification().value = interfaceSpecification
        return repository.save(interfaceSpecificationVersion).awaitSingle()
    }

    /**
     * Creates a new [InterfaceSpecificationVersion] based on the provided [input] on [interfaceSpecification]
     * Does not check the authorization status, does not save the created nodes
     * Validates the [input]
     *
     * @param interfaceSpecification the [InterfaceSpecification] the created [InterfaceSpecificationVersion] is part of
     * @param input defines the [InterfaceSpecificationVersion]
     * @return the created [InterfaceSpecificationVersion]
     */
    suspend fun createInterfaceSpecificationVersion(
        interfaceSpecification: InterfaceSpecification, input: InterfaceSpecificationVersionInput
    ): InterfaceSpecificationVersion {
        input.validate()
        val template = interfaceSpecification.template().value.interfaceSpecificationVersionTemplate().value
        val templatedFields = templatedNodeService.validateInitialTemplatedFields(template, input)
        val interfaceSpecificationVersion =
            InterfaceSpecificationVersion(input.version, input.tags, templatedFields)
        interfaceSpecificationVersion.template().value = template

        input.parts.ifPresent { inputs ->
            interfaceSpecificationVersion.parts() += inputs.map {
                interfacePartService.createInterfacePart(
                    interfaceSpecification, it
                )
            }
        }
        return interfaceSpecificationVersion
    }

    /**
     * Updates a [InterfaceSpecificationVersion] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [InterfaceSpecificationVersion] to update and how
     * @return the updated [InterfaceSpecificationVersion]
     */
    suspend fun updateInterfaceSpecificationVersion(
        authorizationContext: GropiusAuthorizationContext, input: UpdateInterfaceSpecificationVersionInput
    ): InterfaceSpecificationVersion {
        input.validate()
        val interfaceSpecificationVersion = repository.findById(input.id)
        checkPermission(
            interfaceSpecificationVersion,
            Permission(NodePermission.ADMIN, authorizationContext),
            "update the InterfaceSpecificationVersion"
        )
        input.version.ifPresent { interfaceSpecificationVersion.version = it }
        input.tags.ifPresent { interfaceSpecificationVersion.tags = it }
        templatedNodeService.updateTemplatedFields(interfaceSpecificationVersion, input, false)
        return repository.save(interfaceSpecificationVersion).awaitSingle()
    }

    /**
     * Deletes a [InterfaceSpecificationVersion] by id
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [InterfaceSpecificationVersion] to delete
     */
    suspend fun deleteInterfaceSpecificationVersion(
        authorizationContext: GropiusAuthorizationContext, input: DeleteNodeInput
    ) {
        input.validate()
        val interfaceSpecificationVersion = repository.findById(input.id)
        checkPermission(
            interfaceSpecificationVersion,
            Permission(NodePermission.ADMIN, authorizationContext),
            "delete the InterfaceSpecificationVersion"
        )
        val graphUpdater = ComponentGraphUpdater()
        graphUpdater.deleteInterfaceSpecificationVersion(interfaceSpecificationVersion)
        graphUpdater.save(nodeRepository)
    }

}