package gropius.schema.mutation

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.expediagroup.graphql.server.operations.Mutation
import graphql.schema.DataFetchingEnvironment
import gropius.authorization.gropiusAuthorizationContext
import gropius.dto.input.architecture.*
import gropius.dto.input.architecture.layout.CreateViewInput
import gropius.dto.input.architecture.layout.UpdateViewInput
import gropius.dto.input.common.DeleteNodeInput
import gropius.dto.payload.AddComponentVersionToProjectPayload
import gropius.dto.payload.DeleteNodePayload
import gropius.graphql.AutoPayloadType
import gropius.model.architecture.*
import gropius.model.architecture.layout.View
import gropius.service.architecture.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * Contains all architecture-related mutations
 *
 * @param componentService used for Component-related mutations
 * @param projectService used for Project-related mutations
 * @param interfaceSpecificationService used for InterfaceSpecification-related mutations
 * @param interfaceSpecificationVersionService used for InterfaceSpecificationVersion-related mutations
 * @param interfacePartService used for InterfacePart-related mutations
 * @param componentVersionService used for ComponentVersion-related mutations
 * @param imsService used for IMS-related mutations
 * @param imsProjectService used for IMSProject-related mutations
 * @param intraComponentDependencySpecificationService used for IntraComponentDependencySpecificationService-related mutations
 * @param syncPermissionTargetService used for SyncPermissionTarget-related mutations
 * @param viewService used for View-related mutations
 */
@org.springframework.stereotype.Component
@Transactional(propagation = Propagation.REQUIRES_NEW)
class ArchitectureMutations(
    private val componentService: ComponentService,
    private val projectService: ProjectService,
    private val interfaceSpecificationService: InterfaceSpecificationService,
    private val interfaceSpecificationVersionService: InterfaceSpecificationVersionService,
    private val interfacePartService: InterfacePartService,
    private val componentVersionService: ComponentVersionService,
    private val relationService: RelationService,
    private val imsService: IMSService,
    private val imsProjectService: IMSProjectService,
    private val intraComponentDependencySpecificationService: IntraComponentDependencySpecificationService,
    private val syncPermissionTargetService: SyncPermissionTargetService,
    private val viewService: ViewService
) : Mutation {

    @GraphQLDescription(
        """Creates a new Component, requires CAN_CREATE_COMPONENTS.
        Automatically generates a default ComponentPermission which grants the authenticated user READ and ADMIN
        """
    )
    @AutoPayloadType("The created Component")
    suspend fun createComponent(
        @GraphQLDescription("Defines the created Component")
        input: CreateComponentInput, dfe: DataFetchingEnvironment
    ): Component {
        return componentService.createComponent(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Creates multiple Components, requires CAN_CREATE_COMPONENTS.
        Automatically generates a default ComponentPermission which grants the authenticated user READ and ADMIN
        """
    )
    @AutoPayloadType("The created Components", "components")
    suspend fun bulkCreateComponent(
        @GraphQLDescription("Defines the created Components")
        input: BulkCreateComponentInput, dfe: DataFetchingEnvironment
    ): List<Component> {
        return componentService.bulkCreateComponent(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription("Updates the specified Component, requires ADMIN on the component to update")
    @AutoPayloadType("The updated Component")
    suspend fun updateComponent(
        @GraphQLDescription("Defines which Component to update and how to update it")
        input: UpdateComponentInput, dfe: DataFetchingEnvironment
    ): Component {
        return componentService.updateComponent(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription("Deletes the specified Component, requires ADMIN on the component to delete")
    suspend fun deleteComponent(
        @GraphQLDescription("Defines which Component to delete")
        input: DeleteNodeInput, dfe: DataFetchingEnvironment
    ): DeleteNodePayload {
        componentService.deleteComponent(dfe.gropiusAuthorizationContext, input)
        return DeleteNodePayload(input.id)
    }

    @GraphQLDescription(
        """Creates a new Project, requires CAN_CREATE_PROJECTS.
        Automatically generates a default ProjectPermission which grants the authorized user READ and ADMIN
        """
    )
    @AutoPayloadType("The created Project")
    suspend fun createProject(
        @GraphQLDescription("Defines the created Project")
        input: CreateProjectInput, dfe: DataFetchingEnvironment
    ): Project {
        return projectService.createProject(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription("Updates the specified Project, requires ADMIN on the project to update")
    @AutoPayloadType("The updated Project")
    suspend fun updateProject(
        @GraphQLDescription("Defines which Project to update and how to update it")
        input: UpdateProjectInput, dfe: DataFetchingEnvironment
    ): Project {
        return projectService.updateProject(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription("Deletes the specified Project, requires ADMIN on the project to delete")
    suspend fun deleteProject(
        @GraphQLDescription("Defines which Project to delete")
        input: DeleteNodeInput, dfe: DataFetchingEnvironment
    ): DeleteNodePayload {
        projectService.deleteProject(dfe.gropiusAuthorizationContext, input)
        return DeleteNodePayload(input.id)
    }

    @GraphQLDescription("Creates a new View, requires MANAGE_VIEWS on the project owning the view.")
    @AutoPayloadType("The created View")
    suspend fun createView(
        @GraphQLDescription("Defines the created View")
        input: CreateViewInput, dfe: DataFetchingEnvironment
    ): View {
        return viewService.createView(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription("Updates the specified View, requires MANAGE_VIEWS on the project owning the view.")
    @AutoPayloadType("The updated View")
    suspend fun updateView(
        @GraphQLDescription("Defines which View to update and how to update it")
        input: UpdateViewInput, dfe: DataFetchingEnvironment
    ): View {
        return viewService.updateView(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription("Deletes the specified View, requires MANAGE_VIEWS on the project owning the view.")
    suspend fun deleteView(
        @GraphQLDescription("Defines which View to delete")
        input: DeleteNodeInput, dfe: DataFetchingEnvironment
    ): DeleteNodePayload {
        viewService.deleteView(dfe.gropiusAuthorizationContext, input)
        return DeleteNodePayload(input.id)
    }

    @GraphQLDescription("Creates a new InterfaceSpecification, requires ADMIN on the Component.")
    @AutoPayloadType("The created InterfaceSpecification")
    suspend fun createInterfaceSpecification(
        @GraphQLDescription("Defines the created InterfaceSpecification")
        input: CreateInterfaceSpecificationInput, dfe: DataFetchingEnvironment
    ): InterfaceSpecification {
        return interfaceSpecificationService.createInterfaceSpecification(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription("Updates the specified InterfaceSpecification, requires ADMIN on the Component of the InterfaceSpecification to update")
    @AutoPayloadType("The updated InterfaceSpecification")
    suspend fun updateInterfaceSpecification(
        @GraphQLDescription("Defines which InterfaceSpecification to update and how to update it")
        input: UpdateInterfaceSpecificationInput, dfe: DataFetchingEnvironment
    ): InterfaceSpecification {
        return interfaceSpecificationService.updateInterfaceSpecification(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription("Deletes the specified InterfaceSpecification, requires ADMIN on the Component of the InterfaceSpecification to delete")
    suspend fun deleteInterfaceSpecification(
        @GraphQLDescription("Defines which InterfaceSpecification to delete")
        input: DeleteNodeInput, dfe: DataFetchingEnvironment
    ): DeleteNodePayload {
        interfaceSpecificationService.deleteInterfaceSpecification(dfe.gropiusAuthorizationContext, input)
        return DeleteNodePayload(input.id)
    }

    @GraphQLDescription("Creates a new InterfaceSpecificationVersion, requires ADMIN on the Component of the InterfaceSpecification.")
    @AutoPayloadType("The created InterfaceSpecificationVersion")
    suspend fun createInterfaceSpecificationVersion(
        @GraphQLDescription("Defines the created InterfaceSpecificationVersion")
        input: CreateInterfaceSpecificationVersionInput, dfe: DataFetchingEnvironment
    ): InterfaceSpecificationVersion {
        return interfaceSpecificationVersionService.createInterfaceSpecificationVersion(
            dfe.gropiusAuthorizationContext, input
        )
    }

    @GraphQLDescription(
        """Updates the specified InterfaceSpecificationVersion,
        requires ADMIN on the Component of the InterfaceSpecification of the InterfaceSpecificationVersion to update
        """
    )
    @AutoPayloadType("The updated InterfaceSpecificationVersion")
    suspend fun updateInterfaceSpecificationVersion(
        @GraphQLDescription("Defines which InterfaceSpecificationVersion to update and how to update it")
        input: UpdateInterfaceSpecificationVersionInput, dfe: DataFetchingEnvironment
    ): InterfaceSpecificationVersion {
        return interfaceSpecificationVersionService.updateInterfaceSpecificationVersion(
            dfe.gropiusAuthorizationContext, input
        )
    }

    @GraphQLDescription(
        """Deletes the specified InterfaceSpecificationVersion,
        requires ADMIN on the Component of the InterfaceSpecification of the InterfaceSpecificationVersion to delete
        """
    )
    suspend fun deleteInterfaceSpecificationVersion(
        @GraphQLDescription("Defines which InterfaceSpecificationVersion to delete")
        input: DeleteNodeInput, dfe: DataFetchingEnvironment
    ): DeleteNodePayload {
        interfaceSpecificationVersionService.deleteInterfaceSpecificationVersion(dfe.gropiusAuthorizationContext, input)
        return DeleteNodePayload(input.id)
    }

    @GraphQLDescription("Creates a new InterfacePart, requires ADMIN on the Component of the InterfaceSpecification.")
    @AutoPayloadType("The created InterfacePart")
    suspend fun createInterfacePart(
        @GraphQLDescription("Defines the created InterfacePart")
        input: CreateInterfacePartInput, dfe: DataFetchingEnvironment
    ): InterfacePart {
        return interfacePartService.createInterfacePart(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Updates the specified InterfacePart,
        requires ADMIN on the Component of the InterfaceSpecification of the InterfacePart to update
        """
    )
    @AutoPayloadType("The updated InterfacePart")
    suspend fun updateInterfacePart(
        @GraphQLDescription("Defines which InterfacePart to update and how to update it")
        input: UpdateInterfacePartInput, dfe: DataFetchingEnvironment
    ): InterfacePart {
        return interfacePartService.updateInterfacePart(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Deletes the specified InterfacePart,
        requires ADMIN on the Component of the InterfaceSpecification of the InterfacePart to delete"""
    )
    suspend fun deleteInterfacePart(
        @GraphQLDescription("Defines which InterfacePart to delete")
        input: DeleteNodeInput, dfe: DataFetchingEnvironment
    ): DeleteNodePayload {
        interfacePartService.deleteInterfacePart(dfe.gropiusAuthorizationContext, input)
        return DeleteNodePayload(input.id)
    }

    @GraphQLDescription(
        """Updates the specified Interface,
        requires ADMIN on the Component of the ComponentVersion of the InterfaceDefinition of the Interface  to update
        """
    )
    @AutoPayloadType("The updated Interface ")
    suspend fun updateInterface(
        @GraphQLDescription("Defines which Interface to update and how to update it")
        input: UpdateInterfaceInput,
        dfe: DataFetchingEnvironment,
        @GraphQLIgnore
        @Autowired
        interfaceService: InterfaceService
    ): Interface {
        return interfaceService.updateInterface(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Updates the specified InterfaceDefinition,
        requires ADMIN on the Component of the ComponentVersion of the InterfaceDefinition to update
        """
    )
    @AutoPayloadType("The updated InterfaceDefinition ")
    suspend fun updateInterfaceDefinition(
        @GraphQLDescription("Defines which InterfaceDefinition to update and how to update it")
        input: UpdateInterfaceDefinitionInput,
        dfe: DataFetchingEnvironment,
        @GraphQLIgnore
        @Autowired
        interfaceDefinitionService: InterfaceDefinitionService
    ): InterfaceDefinition {
        return interfaceDefinitionService.updateInterfaceDefinition(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Adds an InterfaceSpecificationVersion (in)visible to ComponentVersions, 
        requires ADMIN on the Component of the ComponentVersion to update
        """
    )
    @AutoPayloadType("The updated ComponentVersion")
    suspend fun addInterfaceSpecificationVersionToComponentVersion(
        @GraphQLDescription("Defines the InterfaceSpecificationVersion and ComponentVersion")
        input: AddInterfaceSpecificationVersionToComponentVersionInput, dfe: DataFetchingEnvironment
    ): ComponentVersion {
        return componentVersionService.addInterfaceSpecificationToComponentVersion(
            dfe.gropiusAuthorizationContext, input
        )
    }

    @GraphQLDescription(
        """Removes an InterfaceSpecificationVersion (in)visible from ComponentVersions, 
        requires ADMIN on the Component of the ComponentVersion to update
        """
    )
    @AutoPayloadType("The updated ComponentVersion")
    suspend fun removeInterfaceSpecificationVersionFromComponentVersion(
        @GraphQLDescription("Defines the InterfaceSpecificationVersion and ComponentVersion")
        input: RemoveInterfaceSpecificationVersionFromComponentVersionInput, dfe: DataFetchingEnvironment
    ): ComponentVersion {
        return componentVersionService.removeInterfaceSpecificationFromComponentVersion(
            dfe.gropiusAuthorizationContext, input
        )
    }

    @GraphQLDescription("Creates a new ComponentVersion, requires ADMIN on the Component.")
    @AutoPayloadType("The created ComponentVersion")
    suspend fun createComponentVersion(
        @GraphQLDescription("Defines the created ComponentVersion")
        input: CreateComponentVersionInput, dfe: DataFetchingEnvironment
    ): ComponentVersion {
        return componentVersionService.createComponentVersion(
            dfe.gropiusAuthorizationContext, input
        )
    }

    @GraphQLDescription("Updates the specified ComponentVersion, requires ADMIN on the Component of the ComponentVersion to update")
    @AutoPayloadType("The updated ComponentVersion")
    suspend fun updateComponentVersion(
        @GraphQLDescription("Defines which ComponentVersion to update and how to update it")
        input: UpdateComponentVersionInput, dfe: DataFetchingEnvironment
    ): ComponentVersion {
        return componentVersionService.updateComponentVersion(
            dfe.gropiusAuthorizationContext, input
        )
    }

    @GraphQLDescription("Deletes the specified ComponentVersion, requires ADMIN on the Component of the ComponentVersion to delete")
    suspend fun deleteComponentVersion(
        @GraphQLDescription("Defines which ComponentVersion to delete")
        input: DeleteNodeInput, dfe: DataFetchingEnvironment
    ): DeleteNodePayload {
        componentVersionService.deleteComponentVersion(dfe.gropiusAuthorizationContext, input)
        return DeleteNodePayload(input.id)
    }

    @GraphQLDescription(
        """Creates a new Relation, requires RELATE_FROM_COMPONENT on the Component associated with start.
        """
    )
    @AutoPayloadType("The created Relation")
    suspend fun createRelation(
        @GraphQLDescription("Defines the created Relation")
        input: CreateRelationInput, dfe: DataFetchingEnvironment
    ): Relation {
        return relationService.createRelation(
            dfe.gropiusAuthorizationContext, input
        )
    }

    @GraphQLDescription(
        """Creates multiple Relations, requires RELATE_FROM_COMPONENT on the Component associated with start.
        """
    )
    @AutoPayloadType("The created Relations", "relations")
    suspend fun bulkCreateRelation(
        @GraphQLDescription("Defines the created Relations")
        input: BulkCreateRelationInput, dfe: DataFetchingEnvironment
    ): List<Relation> {
        return relationService.bulkCreateRelation(
            dfe.gropiusAuthorizationContext, input
        )
    }

    @GraphQLDescription(
        """Updates the specified Relation, requires RELATE_FROM_COMPONENT on the Component associated with start.
        """
    )
    @AutoPayloadType("The updated Relation")
    suspend fun updateRelation(
        @GraphQLDescription("Defines which Relation to update and how to update it")
        input: UpdateRelationInput, dfe: DataFetchingEnvironment
    ): Relation {
        return relationService.updateRelation(
            dfe.gropiusAuthorizationContext, input
        )
    }

    @GraphQLDescription(
        """Deletes the specified Relation, requires RELATE_FROM_COMPONENT on the Component associated with start.
        """
    )
    suspend fun deleteRelation(
        @GraphQLDescription("Defines which Relation to delete")
        input: DeleteNodeInput, dfe: DataFetchingEnvironment
    ): DeleteNodePayload {
        relationService.deleteRelation(dfe.gropiusAuthorizationContext, input)
        return DeleteNodePayload(input.id)
    }

    @GraphQLDescription(
        """Adds a ComponentVersion to a Project,
        requires MANAGE_COMPONENTS on the Project and ADD_TO_PROJECTS on the Component associated
        with the ComponentVersion
        """
    )
    suspend fun addComponentVersionToProject(
        @GraphQLDescription("Defines which ComponentVersion to add to which Project")
        input: AddComponentVersionToProjectInput, dfe: DataFetchingEnvironment
    ): AddComponentVersionToProjectPayload {
        val (project, componentVersion) = projectService.addComponentVersionToProject(dfe.gropiusAuthorizationContext, input)
        return AddComponentVersionToProjectPayload(project, componentVersion)
    }

    @GraphQLDescription(
        """Removes a ComponentVersion from a Project,
        requires MANAGE_COMPONENTS on the Project
        """
    )
    @AutoPayloadType("The updated Project")
    suspend fun removeComponentVersionFromProject(
        @GraphQLDescription("Defines which ComponentVersion to remove from which Project")
        input: RemoveComponentVersionFromProjectInput, dfe: DataFetchingEnvironment
    ): Project {
        return projectService.removeComponentVersionFromProject(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Creates a new IMS, requires CAN_CREATE_IMSS.
        Automatically generates a default IMSPermission which grants the authorized user READ and ADMIN
        """
    )
    @AutoPayloadType("The created IMS")
    suspend fun createIMS(
        @GraphQLDescription("Defines the created IMS")
        input: CreateIMSInput, dfe: DataFetchingEnvironment
    ): IMS {
        return imsService.createIMS(
            dfe.gropiusAuthorizationContext, input
        )
    }

    @GraphQLDescription("Updates the specified IMS, requires ADMIN on the IMS.")
    @AutoPayloadType("The updated IMS")
    suspend fun updateIMS(
        @GraphQLDescription("Defines which IMS to update and how to update it")
        input: UpdateIMSInput, dfe: DataFetchingEnvironment
    ): IMS {
        return imsService.updateIMS(
            dfe.gropiusAuthorizationContext, input
        )
    }

    @GraphQLDescription(
        """Deletes the specified IMS, requires ADMIN on the IMS to delete. 
        Also deletes all associated IMSProjects
        """
    )
    suspend fun deleteIMS(
        @GraphQLDescription("Defines which IMS to delete")
        input: DeleteNodeInput, dfe: DataFetchingEnvironment
    ): DeleteNodePayload {
        imsService.deleteIMS(dfe.gropiusAuthorizationContext, input)
        return DeleteNodePayload(input.id)
    }

    @GraphQLDescription(
        """Creates a new IMSProject, requirse SYNC_TRACKABLES on the specified IMS
        AND MANAGE_IMS on the specified Trackable
        """
    )
    @AutoPayloadType("The created IMSProject")
    suspend fun createIMSProject(
        @GraphQLDescription("Defines the created IMSProject")
        input: CreateIMSProjectInput, dfe: DataFetchingEnvironment
    ): IMSProject {
        return imsProjectService.createIMSProject(
            dfe.gropiusAuthorizationContext, input
        )
    }

    @GraphQLDescription(
        """Updates the specified IMSProject, requirse SYNC_TRACKABLES on the IMS associted with the
        specified IMSProject AND MANAGE_IMS on the Trackable associated with the specified
        IMSProject.
        """
    )
    @AutoPayloadType("The updated IMSProject")
    suspend fun updateIMSProject(
        @GraphQLDescription("Defines which IMSProject to update and how to update it")
        input: UpdateIMSProjectInput, dfe: DataFetchingEnvironment
    ): IMSProject {
        return imsProjectService.updateIMSProject(
            dfe.gropiusAuthorizationContext, input
        )
    }

    @GraphQLDescription(
        """Deletes the specified IMSProject, requirse ADMIN on the IMS associted with the
        specified IMSProject OR MANAGE_IMS on the Trackable associated with the specified
        IMSProject.
        """
    )
    suspend fun deleteIMSProject(
        @GraphQLDescription("Defines which IMSProject to delete")
        input: DeleteNodeInput, dfe: DataFetchingEnvironment
    ): DeleteNodePayload {
        imsProjectService.deleteIMSProject(dfe.gropiusAuthorizationContext, input)
        return DeleteNodePayload(input.id)
    }

    @GraphQLDescription(
        """Creates a new IntraComponentDependencySpecification, requires ADMIN on the Component associated with the 
        specified ComponentVersion.
        """
    )
    @AutoPayloadType("The created IntraComponentDependencySpecification")
    suspend fun createIntraComponentDependencySpecification(
        @GraphQLDescription("Defines the created IntraComponentDependencySpecification")
        input: CreateIntraComponentDependencySpecificationInput, dfe: DataFetchingEnvironment
    ): IntraComponentDependencySpecification {
        return intraComponentDependencySpecificationService.createIntraComponentDependencySpecification(
            dfe.gropiusAuthorizationContext, input
        )
    }

    @GraphQLDescription(
        """Updates the specified IntraComponentDependencySpecification, requires ADMIN on the Component associated with the 
        IntraComponentDependencySpecification to update."""
    )
    @AutoPayloadType("The updated IntraComponentDependencySpecification")
    suspend fun updateIntraComponentDependencySpecification(
        @GraphQLDescription("Defines which IntraComponentDependencySpecification to update and how to update it")
        input: UpdateIntraComponentDependencySpecificationInput, dfe: DataFetchingEnvironment
    ): IntraComponentDependencySpecification {
        return intraComponentDependencySpecificationService.updateIntraComponentDependencySpecification(
            dfe.gropiusAuthorizationContext, input
        )
    }

    @GraphQLDescription(
        """Deletes the specified IntraComponentDependencySpecification, requires ADMIN on the Component associated with the 
        IntraComponentDependencySpecification to delete."""
    )
    suspend fun deleteIntraComponentDependencySpecification(
        @GraphQLDescription("Defines which IntraComponentDependencySpecification to delete")
        input: DeleteNodeInput, dfe: DataFetchingEnvironment
    ): DeleteNodePayload {
        intraComponentDependencySpecificationService.deleteIntraComponentDependencySpecification(
            dfe.gropiusAuthorizationContext, input
        )
        return DeleteNodePayload(input.id)
    }

    @GraphQLDescription("Updates whether the current user allows sync self/others on the specified target")
    @AutoPayloadType("The updated SyncPermissionTarget")
    suspend fun updateSyncPermissions(
        @GraphQLDescription("Defines which SyncPermissionTarget to update and how to update it")
        input: UpdateSyncPermissionsInput, dfe: DataFetchingEnvironment
    ): SyncPermissionTarget {
        return syncPermissionTargetService.updateSyncPermissions(
            dfe.gropiusAuthorizationContext, input
        )
    }

}