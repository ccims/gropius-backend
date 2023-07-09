package gropius.service.architecture

import gropius.authorization.GropiusAuthorizationContext
import gropius.dto.input.architecture.CreateComponentInput
import gropius.dto.input.architecture.UpdateComponentInput
import gropius.dto.input.common.DeleteNodeInput
import gropius.dto.input.ifPresent
import gropius.dto.input.isPresent
import gropius.model.architecture.Component
import gropius.model.architecture.ComponentVersion
import gropius.model.architecture.InterfaceSpecification
import gropius.model.template.ComponentTemplate
import gropius.model.user.permission.GlobalPermission
import gropius.model.user.permission.NodePermission
import gropius.repository.architecture.ComponentRepository
import gropius.repository.common.NodeRepository
import gropius.repository.findById
import gropius.repository.template.ComponentTemplateRepository
import gropius.service.template.TemplatedNodeService
import gropius.service.user.permission.ComponentPermissionService
import io.github.graphglue.authorization.Permission
import io.github.graphglue.model.Node
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service

/**
 * Service for [Component]s. Provides functions to create, update and delete
 *
 * @param repository the associated repository used for CRUD functionality
 * @param templatedNodeService service used to update templatedFields
 * @param componentTemplateRepository used to get [ComponentTemplate]
 * @param componentPermissionService used to create the initial permission for a created [Component]
 * @param interfaceSpecificationService used to create [InterfaceSpecification]s
 * @param componentVersionService used to create [ComponentVersion]s
 */
@Service
class ComponentService(
    repository: ComponentRepository,
    private val templatedNodeService: TemplatedNodeService,
    private val componentTemplateRepository: ComponentTemplateRepository,
    private val componentPermissionService: ComponentPermissionService,
    private val interfaceSpecificationService: InterfaceSpecificationService,
    private val componentVersionService: ComponentVersionService
) : TrackableService<Component, ComponentRepository>(repository) {

    /**
     * Creates a new [Component] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines the [Component]
     * @return the saved created [Component]
     */
    suspend fun createComponent(
        authorizationContext: GropiusAuthorizationContext, input: CreateComponentInput
    ): Component {
        input.validate()
        val user = getUser(authorizationContext)
        checkPermission(
            user, Permission(GlobalPermission.CAN_CREATE_COMPONENTS, authorizationContext), "create Components"
        )
        val template = componentTemplateRepository.findById(input.template)
        val templatedFields = templatedNodeService.validateInitialTemplatedFields(template, input)
        val component = Component(input.name, input.description, input.repositoryURL, templatedFields)
        component.template().value = template
        input.interfaceSpecifications.ifPresent { inputs ->
            component.interfaceSpecifications() += inputs.map {
                interfaceSpecificationService.createInterfaceSpecification(component, it)
            }
        }
        input.versions.ifPresent { inputs ->
            component.versions() += inputs.map {
                componentVersionService.createComponentVersion(component, it)
            }
        }
        createdExtensibleNode(component, input)
        componentPermissionService.createDefaultPermission(user, component)
        return repository.save(component).awaitSingle()
    }

    /**
     * Updates a [Component] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [Component] to update and how
     * @return the updated [Component]
     */
    suspend fun updateComponent(
        authorizationContext: GropiusAuthorizationContext, input: UpdateComponentInput
    ): Component {
        input.validate()
        val component = repository.findById(input.id)
        checkPermission(
            component, Permission(NodePermission.ADMIN, authorizationContext), "update the Component"
        )
        val nodesToSave = mutableSetOf<Node>(component)
        nodesToSave += updateComponentTemplate(input, component)
        templatedNodeService.updateTemplatedFields(component, input, input.template.isPresent)
        componentPermissionService.updatePermissionsOfNode(
            component, input.addedPermissions, input.removedPermissions, authorizationContext
        )
        updateTrackable(component, input)
        return nodeRepository.saveAll(nodesToSave).collectList().awaitSingle().first { it == component } as Component
    }

    /**
     * Updates the template of a [Component], if a new is defined
     * Does not check the authorization status
     *
     * @param input maybe defines a new template
     * @param component the [Component] to update
     * @return a set of updated nodes, must be saved
     */
    private suspend fun updateComponentTemplate(
        input: UpdateComponentInput,
        component: Component
    ): Set<Node> {
        input.template.ifPresent { templateId ->
            component.template().value = componentTemplateRepository.findById(templateId)
            val componentVersionTemplate = component.template().value.componentVersionTemplate().value
            component.versions().forEach {
                it.template().value = componentVersionTemplate
                templatedNodeService.updateTemplatedFields(it, input.componentVersionTemplatedFields, true)
            }
            val graphUpdater = ComponentGraphUpdater()
            graphUpdater.updateComponentTemplate(component)
            nodeRepository.deleteAll(graphUpdater.deletedNodes).awaitSingleOrNull()
            return graphUpdater.updatedNodes
        }
        return emptySet()
    }

    /**
     * Deletes a [Component] by id
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [Component] to delete
     */
    suspend fun deleteComponent(
        authorizationContext: GropiusAuthorizationContext, input: DeleteNodeInput
    ) {
        input.validate()
        val component = repository.findById(input.id)
        checkPermission(
            component, Permission(NodePermission.ADMIN, authorizationContext), "delete the Component"
        )
        val graphUpdater = ComponentGraphUpdater()
        graphUpdater.deleteComponent(component)
        beforeDeleteTrackable(component)
        nodeRepository.deleteAll(graphUpdater.deletedNodes).awaitSingleOrNull()
        nodeRepository.saveAll(graphUpdater.updatedNodes).collectList().awaitSingle()
    }

}