package gropius.service.architecture

import gropius.authorization.GropiusAuthorizationContext
import gropius.dto.input.architecture.BulkCreateComponentInput
import gropius.dto.input.architecture.CreateComponentInput
import gropius.dto.input.architecture.UpdateComponentInput
import gropius.dto.input.common.DeleteNodeInput
import gropius.dto.input.ifPresent
import gropius.dto.input.isPresent
import gropius.dto.input.toMapping
import gropius.model.architecture.Component
import gropius.model.architecture.ComponentVersion
import gropius.model.architecture.InterfaceSpecification
import gropius.model.template.ComponentTemplate
import gropius.model.template.IntraComponentDependencySpecificationType
import gropius.model.user.GropiusUser
import gropius.model.user.permission.GlobalPermission
import gropius.model.user.permission.NodePermission
import gropius.repository.architecture.ComponentRepository
import gropius.repository.findById
import gropius.repository.template.ComponentTemplateRepository
import gropius.repository.template.IntraComponentDependencySpecificationTypeRepository
import gropius.service.NodeBatchUpdateContext
import gropius.service.template.TemplatedNodeService
import gropius.service.user.permission.ComponentPermissionService
import io.github.graphglue.authorization.Permission
import kotlinx.coroutines.reactor.awaitSingle
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
 * @param intraComponentDependencySpecificationTypeRepository used to get [IntraComponentDependencySpecificationType]s
 */
@Service
class ComponentService(
    repository: ComponentRepository,
    private val templatedNodeService: TemplatedNodeService,
    private val componentTemplateRepository: ComponentTemplateRepository,
    private val componentPermissionService: ComponentPermissionService,
    private val interfaceSpecificationService: InterfaceSpecificationService,
    private val componentVersionService: ComponentVersionService,
    private val intraComponentDependencySpecificationTypeRepository: IntraComponentDependencySpecificationTypeRepository
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
        return createComponentInternal(input, user)
    }

    /**
     * Creates multiple new [Component]s based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines the [Component]s
     * @return the saved created [Component]s
     */
    suspend fun bulkCreateComponent(
        authorizationContext: GropiusAuthorizationContext, input: BulkCreateComponentInput
    ): List<Component> {
        input.validate()
        val user = getUser(authorizationContext)
        checkPermission(
            user, Permission(GlobalPermission.CAN_CREATE_COMPONENTS, authorizationContext), "create Components"
        )
        return input.components.map { createComponentInternal(it, user) }
    }

    /**
     * Creates a new [Component] based on the provided [input]
     * Does not check the authorization status
     *
     * @param input defines the [Component]
     * @param user the user who created the [Component]
     * @return the saved created [Component]
     */
    private suspend fun createComponentInternal(input: CreateComponentInput, user: GropiusUser): Component {
        val template = componentTemplateRepository.findById(input.template)
        val templatedFields = templatedNodeService.validateInitialTemplatedFields(template, input)
        val component = Component(input.name, input.description, input.repositoryURL, templatedFields)
        component.template().value = template
        input.interfaceSpecifications.ifPresent { inputs ->
            component.interfaceSpecifications() += inputs.map {
                interfaceSpecificationService.createInterfaceSpecification(component, it)
            }
        }
        componentPermissionService.createDefaultPermission(user, component)
        val savedComponent = nodeRepository.save(component).awaitSingle()
        val updateContext = NodeBatchUpdateContext()
        input.versions.ifPresent { inputs ->
            savedComponent.versions() += inputs.map {
                componentVersionService.createComponentVersion(savedComponent, it, updateContext)
            }
        }
        return updateContext.save(savedComponent, nodeRepository)
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
        val updateContext = NodeBatchUpdateContext()
        updateComponentTemplate(input, component, updateContext)
        templatedNodeService.updateTemplatedFields(component, input, input.template.isPresent)
        componentPermissionService.updatePermissionsOfNode(
            component, input.addedPermissions, input.removedPermissions, authorizationContext
        )
        updateTrackable(component, input)
        return updateContext.save(component, nodeRepository)
    }

    /**
     * Updates the template of a [Component], if a new is defined
     * Does not check the authorization status
     *
     * @param input maybe defines a new template
     * @param component the [Component] to update
     * @param updateContext the context used to update the nodes
     * @return a set of updated nodes, must be saved
     */
    private suspend fun updateComponentTemplate(
        input: UpdateComponentInput, component: Component, updateContext: NodeBatchUpdateContext
    ) {
        input.template.ifPresent { templateId ->
            component.template().value = componentTemplateRepository.findById(templateId)
            val componentVersionTemplate = component.template().value.componentVersionTemplate().value
            component.versions().forEach {
                it.template().value = componentVersionTemplate
                templatedNodeService.updateTemplatedFields(it, input.componentVersionTemplatedFields, true)
            }
            val typeMapping = input.intraComponentDependencySpecificationTypeMapping.toMapping(
                intraComponentDependencySpecificationTypeRepository
            ) {
                checkTypeCompatibility(component.template().value, it)
            }
            updateIntraComponentDependencySpecificationsAfterTemplateUpdate(component, typeMapping, updateContext)
            val graphUpdater = ComponentGraphUpdater(updateContext)
            graphUpdater.updateComponentTemplate(component)
        }
    }

    /**
     * Checks if the [IntraComponentDependencySpecificationType]s are compatible with the new template
     *
     * @param template the new template
     * @param type the [IntraComponentDependencySpecificationType] to check
     * @throws IllegalArgumentException if the type is not compatible with the template
     */
    private suspend fun checkTypeCompatibility(
        template: ComponentTemplate, type: IntraComponentDependencySpecificationType
    ) {
        if (template !in type.partOf()) {
            throw IllegalArgumentException(
                "IntraComponentDependencySpecificationType cannot be used with this ComponentVersion as it is not provided by the template of the Component"
            )
        }
    }

    /**
     * Updates the [IntraComponentDependencySpecificationType]s where the old type is incompatible with the new template
     * based on [typeMapping] and removes types with no replacement
     * Adds the updated nodes to [updateContext]
     *
     * @param component the [Component] to update
     * @param typeMapping the mapping of old to new [IntraComponentDependencySpecificationType]s
     * @param updateContext the context used to update the nodes
     */
    private suspend fun updateIntraComponentDependencySpecificationsAfterTemplateUpdate(
        component: Component,
        typeMapping: Map<IntraComponentDependencySpecificationType, IntraComponentDependencySpecificationType?>,
        updateContext: NodeBatchUpdateContext
    ) {
        val newTemplate = component.template().value
        for (version in component.versions()) {
            for (intraComponentDependencySpecification in version.intraComponentDependencySpecifications()) {
                val type = intraComponentDependencySpecification.type().value
                if (type != null && type !in newTemplate.intraComponentDependencySpecificationTypes()) {
                    intraComponentDependencySpecification.type().value = typeMapping[type]
                    updateContext.internalUpdatedNodes += intraComponentDependencySpecification
                }
            }
        }
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
        graphUpdater.save(nodeRepository)
    }

}