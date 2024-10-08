package gropius.service.architecture

import gropius.authorization.GropiusAuthorizationContext
import gropius.dto.input.architecture.CreateInterfaceSpecificationInput
import gropius.dto.input.architecture.InterfaceSpecificationInput
import gropius.dto.input.architecture.UpdateInterfaceSpecificationInput
import gropius.dto.input.common.DeleteNodeInput
import gropius.dto.input.ifPresent
import gropius.dto.input.isPresent
import gropius.model.architecture.Component
import gropius.model.architecture.InterfacePart
import gropius.model.architecture.InterfaceSpecification
import gropius.model.architecture.InterfaceSpecificationVersion
import gropius.model.template.InterfaceSpecificationTemplate
import gropius.model.user.permission.NodePermission
import gropius.repository.architecture.ComponentRepository
import gropius.repository.architecture.InterfaceSpecificationRepository
import gropius.repository.common.NodeRepository
import gropius.repository.findById
import gropius.repository.template.InterfaceSpecificationTemplateRepository
import gropius.service.NodeBatchUpdateContext
import gropius.service.template.TemplatedNodeService
import io.github.graphglue.authorization.Permission
import io.github.graphglue.model.Node
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service

/**
 * Service for [InterfaceSpecification]s. Provides functions to create, update and delete
 *
 * @param repository the associated repository used for CRUD functionality
 * @param componentRepository used to get [Component]s
 * @param interfaceSpecificationVersionService used to create [InterfaceSpecificationVersion]s
 * @param interfacePartService used to create [InterfacePart]s
 * @param templatedNodeService used to update templatedFields
 * @param interfaceSpecificationTemplateRepository used to get [InterfaceSpecificationTemplate]s
 * @param nodeRepository used to save/delete any node
 */
@Service
class InterfaceSpecificationService(
    repository: InterfaceSpecificationRepository,
    private val componentRepository: ComponentRepository,
    private val interfaceSpecificationVersionService: InterfaceSpecificationVersionService,
    private val interfacePartService: InterfacePartService,
    private val templatedNodeService: TemplatedNodeService,
    private val interfaceSpecificationTemplateRepository: InterfaceSpecificationTemplateRepository,
    private val nodeRepository: NodeRepository,
) : NamedAffectedByIssueService<InterfaceSpecification, InterfaceSpecificationRepository>(repository) {

    /**
     * Creates a new [InterfaceSpecification] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines the [InterfaceSpecification]
     * @return the saved created [InterfaceSpecification]
     */
    suspend fun createInterfaceSpecification(
        authorizationContext: GropiusAuthorizationContext, input: CreateInterfaceSpecificationInput
    ): InterfaceSpecification {
        input.validate()
        val component = componentRepository.findById(input.component)
        checkPermission(
            component,
            Permission(NodePermission.ADMIN, authorizationContext),
            "create InterfaceSpecifications on the Component"
        )
        val interfaceSpecification = createInterfaceSpecification(component, input)
        return repository.save(interfaceSpecification).awaitSingle()
    }

    /**
     * Creates a new [InterfaceSpecification] based on the provided [input] on [component]
     * Does not check the authorization status, does not save the created nodes
     * Validates the [input]
     *
     * @param component the [Component] the created [InterfaceSpecification] is part of
     * @param input defines the [InterfaceSpecification]
     * @return the created [InterfaceSpecification]
     */
    suspend fun createInterfaceSpecification(
        component: Component, input: InterfaceSpecificationInput
    ): InterfaceSpecification {
        input.validate()
        val template = interfaceSpecificationTemplateRepository.findById(input.template)
        val templatedFields = templatedNodeService.validateInitialTemplatedFields(template, input)
        val interfaceSpecification = InterfaceSpecification(input.name, input.description, templatedFields)
        interfaceSpecification.template().value = template
        interfaceSpecification.component().value = component
        input.versions.ifPresent { inputs ->
            interfaceSpecification.versions() += inputs.map {
                interfaceSpecificationVersionService.createInterfaceSpecificationVersion(
                    interfaceSpecification, it
                )
            }
        }
        return interfaceSpecification
    }

    /**
     * Updates a [InterfaceSpecification] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [InterfaceSpecification] to update and how
     * @return the updated [InterfaceSpecification]
     */
    suspend fun updateInterfaceSpecification(
        authorizationContext: GropiusAuthorizationContext, input: UpdateInterfaceSpecificationInput
    ): InterfaceSpecification {
        input.validate()
        val interfaceSpecification = repository.findById(input.id)
        checkPermission(
            interfaceSpecification,
            Permission(NodePermission.ADMIN, authorizationContext),
            "update the InterfaceSpecification"
        )
        val updateContext = NodeBatchUpdateContext()
        updateInterfaceSpecificationTemplate(input, interfaceSpecification, updateContext)
        templatedNodeService.updateTemplatedFields(interfaceSpecification, input, input.template.isPresent)
        updateNamedAffectedByIssue(interfaceSpecification, input)
        return updateContext.save(interfaceSpecification, nodeRepository)
    }

    /**
     * Updates the template of an [interfaceSpecification], if provided via the [input]
     * Does not check the authorization status
     *
     * @param input defines how to update the template
     * @param interfaceSpecification the [InterfaceSpecification] to update
     * @param updateContext the context used to update the nodes
     * @return the updated nodes to save
     */
    private suspend fun updateInterfaceSpecificationTemplate(
        input: UpdateInterfaceSpecificationInput, interfaceSpecification: InterfaceSpecification, updateContext: NodeBatchUpdateContext
    ): Set<Node> {
        input.template.ifPresent { templateId ->
            val template = interfaceSpecificationTemplateRepository.findById(templateId)
            interfaceSpecification.template().value = template
            updateInterfaceSpecificationVersionTemplate(interfaceSpecification, input, template)
            val graphUpdater = ComponentGraphUpdater(updateContext)
            graphUpdater.updateInterfaceSpecificationTemplate(interfaceSpecification)
            return graphUpdater.updatedNodes
        }
        return emptySet()
    }

    /**
     * Updates the [InterfaceSpecificationVersion]s of the [interfaceSpecification] after a template update
     * Does not check the authorization status
     *
     * @param interfaceSpecification updated, contains the [InterfaceSpecificationVersion]s to update
     * @param input defines how to update [InterfaceSpecificationVersion]s of the [interfaceSpecification]
     * @param template the new template of the [InterfaceSpecification]
     */
    private suspend fun updateInterfaceSpecificationVersionTemplate(
        interfaceSpecification: InterfaceSpecification,
        input: UpdateInterfaceSpecificationInput,
        template: InterfaceSpecificationTemplate
    ) {
        val interfaceSpecificationVersionTemplate = template.interfaceSpecificationVersionTemplate().value
        interfaceSpecification.versions().forEach { interfaceSpecificationVersion ->
            interfaceSpecificationVersion.template().value = interfaceSpecificationVersionTemplate
            templatedNodeService.updateTemplatedFields(
                interfaceSpecificationVersion, input.interfaceSpecificationVersionTemplatedFields, true
            )
        }
    }

    /**
     * Deletes a [InterfaceSpecification] by id
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [InterfaceSpecification] to delete
     */
    suspend fun deleteInterfaceSpecification(
        authorizationContext: GropiusAuthorizationContext, input: DeleteNodeInput
    ) {
        input.validate()
        val interfaceSpecification = repository.findById(input.id)
        checkPermission(
            interfaceSpecification,
            Permission(NodePermission.ADMIN, authorizationContext),
            "delete the InterfaceSpecification"
        )
        val graphUpdater = ComponentGraphUpdater()
        graphUpdater.deleteInterfaceSpecification(interfaceSpecification)
        graphUpdater.save(nodeRepository)
    }

}