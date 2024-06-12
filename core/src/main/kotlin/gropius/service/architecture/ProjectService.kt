package gropius.service.architecture

import gropius.authorization.GropiusAuthorizationContext
import gropius.dto.input.architecture.AddComponentVersionToProjectInput
import gropius.dto.input.architecture.CreateProjectInput
import gropius.dto.input.architecture.RemoveComponentVersionFromProjectInput
import gropius.dto.input.architecture.UpdateProjectInput
import gropius.dto.input.common.DeleteNodeInput
import gropius.model.architecture.ComponentVersion
import gropius.model.architecture.Project
import gropius.model.user.permission.ComponentPermission
import gropius.model.user.permission.GlobalPermission
import gropius.model.user.permission.NodePermission
import gropius.model.user.permission.ProjectPermission
import gropius.repository.architecture.ComponentVersionRepository
import gropius.repository.architecture.ProjectRepository
import gropius.repository.findById
import gropius.service.user.permission.ProjectPermissionService
import io.github.graphglue.authorization.Permission
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service

/**
 * Service for [Project]s. Provides functions to create, update and delete
 *
 * @param repository the associated repository used for CRUD functionality
 * @param projectPermissionService used to create the initial permission for a created [Project]
 * @param componentVersionRepository used to get [ComponentVersion]s by id
 */
@Service
class ProjectService(
    repository: ProjectRepository,
    private val projectPermissionService: ProjectPermissionService,
    private val componentVersionRepository: ComponentVersionRepository
) : TrackableService<Project, ProjectRepository>(repository) {

    /**
     * Creates a new [Project] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines the [Project]
     * @return the saved created [Project]
     */
    suspend fun createProject(
        authorizationContext: GropiusAuthorizationContext, input: CreateProjectInput
    ): Project {
        input.validate()
        val user = getUser(authorizationContext)
        checkPermission(
            user, Permission(GlobalPermission.CAN_CREATE_PROJECTS, authorizationContext), "create Projects"
        )
        val project = Project(input.name, input.description, input.repositoryURL)
        projectPermissionService.createDefaultPermission(user, project)
        return repository.save(project).awaitSingle()
    }

    /**
     * Updates a [Project] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [Project] to update and how
     * @return the updated [Project]
     */
    suspend fun updateProject(
        authorizationContext: GropiusAuthorizationContext, input: UpdateProjectInput
    ): Project {
        input.validate()
        val project = repository.findById(input.id)
        checkPermission(
            project, Permission(NodePermission.ADMIN, authorizationContext), "update the Project"
        )
        projectPermissionService.updatePermissionsOfNode(
            project, input.addedPermissions, input.removedPermissions, authorizationContext
        )
        updateTrackable(project, input)
        return repository.save(project).awaitSingle()
    }

    /**
     * Deletes a [Project] by id
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [Project] to delete
     */
    suspend fun deleteProject(
        authorizationContext: GropiusAuthorizationContext, input: DeleteNodeInput
    ) {
        input.validate()
        val project = repository.findById(input.id)
        checkPermission(
            project, Permission(NodePermission.ADMIN, authorizationContext), "delete the Project"
        )
        beforeDeleteTrackable(project)
        repository.delete(project).awaitSingleOrNull()
    }

    /**
     * Adds a [ComponentVersion] to a [Project]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [ComponentVersion] to add to which [Project]
     * @return the updated [Project] and the added [ComponentVersion]
     */
    suspend fun addComponentVersionToProject(
        authorizationContext: GropiusAuthorizationContext, input: AddComponentVersionToProjectInput
    ): Pair<Project, ComponentVersion> {
        input.validate()
        val project = repository.findById(input.project)
        checkPermission(
            project,
            Permission(ProjectPermission.MANAGE_COMPONENTS, authorizationContext),
            "add ComponentVersions to the Project"
        )
        val componentVersion = componentVersionRepository.findById(input.componentVersion)
        checkPermission(
            componentVersion,
            Permission(ComponentPermission.ADD_TO_PROJECTS, authorizationContext),
            "add the ComponentVersion to Projects"
        )
        project.components() += componentVersion
        val savedNodes = nodeRepository.saveAll(listOf(project, componentVersion)).collectList().awaitSingle()
        return Pair(
            savedNodes.first { it is Project } as Project,
            savedNodes.first { it is ComponentVersion } as ComponentVersion
        )
    }

    /**
     * Removes a [ComponentVersion] from a [Project]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [ComponentVersion] to remove from which [Project]
     * @return the updated [Project]
     */
    suspend fun removeComponentVersionFromProject(
        authorizationContext: GropiusAuthorizationContext, input: RemoveComponentVersionFromProjectInput
    ): Project {
        input.validate()
        val project = repository.findById(input.project)
        checkPermission(
            project,
            Permission(ProjectPermission.MANAGE_COMPONENTS, authorizationContext),
            "remove ComponentVersions from the Project"
        )
        val componentVersion = componentVersionRepository.findById(input.componentVersion)
        project.components() -= componentVersion
        return repository.save(project).awaitSingle()
    }

}