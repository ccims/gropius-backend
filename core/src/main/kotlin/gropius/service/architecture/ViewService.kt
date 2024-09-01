package gropius.service.architecture

import gropius.authorization.GropiusAuthorizationContext
import gropius.dto.input.architecture.layout.CreateViewInput
import gropius.dto.input.architecture.layout.UpdateViewInput
import gropius.dto.input.common.DeleteNodeInput
import gropius.model.architecture.Project
import gropius.model.architecture.layout.View
import gropius.model.user.permission.ProjectPermission
import gropius.repository.architecture.ProjectRepository
import gropius.repository.architecture.ViewRepository
import gropius.repository.common.NodeRepository
import gropius.repository.findById
import gropius.service.NodeBatchUpdateContext
import gropius.service.common.NamedNodeService
import io.github.graphglue.authorization.Permission
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service

/**
 * Service for [View]s. Provides functions to create, update and delete
 *
 * @param repository the associated repository used for CRUD functionality
 * @param projectRepository used to get [Project]s by id
 * @param layoutService used to update the layout of a [View]
 * @param nodeRepository used to save the [View]
 */
@Service
class ViewService(
    repository: ViewRepository,
    private val projectRepository: ProjectRepository,
    private val layoutService: LayoutService,
    private val nodeRepository: NodeRepository
) : NamedNodeService<View, ViewRepository>(repository) {

    /**
     * Creates a new [View] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines the [View]
     * @return the saved created [View]
     */
    suspend fun createView(
        authorizationContext: GropiusAuthorizationContext, input: CreateViewInput
    ): View {
        input.validate()
        val project = projectRepository.findById(input.project)
        checkManageViewsPermission(project, authorizationContext)
        val view = View(input.name, input.description)
        val batchUpdater = NodeBatchUpdateContext()
        layoutService.updateLayout(view, input, batchUpdater)
        return batchUpdater.save(view, nodeRepository)
    }

    /**
     * Updates a [View] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [View] to update and how
     * @return the updated [View]
     */
    suspend fun updateView(
        authorizationContext: GropiusAuthorizationContext, input: UpdateViewInput
    ): View {
        input.validate()
        val view = repository.findById(input.id)
        checkManageViewsPermission(view.project().value, authorizationContext)
        updateNamedNode(view, input)
        val batchUpdater = NodeBatchUpdateContext()
        layoutService.updateLayout(view, input, batchUpdater)
        return batchUpdater.save(view, nodeRepository)
    }

    /**
     * Deletes a [View] by id
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [View] to delete
     */
    suspend fun deleteView(
        authorizationContext: GropiusAuthorizationContext, input: DeleteNodeInput
    ) {
        input.validate()
        val view = repository.findById(input.id)
        checkManageViewsPermission(view.project().value, authorizationContext)
        nodeRepository.deleteAll(view.relationLayouts() + view.relationLayouts()).awaitSingle()
        repository.delete(view).awaitSingle()
    }

    /**
     * Checks that the user has [ProjectPermission.MANAGE_VIEWS] on the provided [project]
     *
     * @param project the [Project] where the permission must be granted
     * @param authorizationContext necessary for checking for the permission
     * @throws IllegalArgumentException if the permission is not granted
     */
    private suspend fun checkManageViewsPermission(
        project: Project, authorizationContext: GropiusAuthorizationContext
    ) {
        checkPermission(
            project, Permission(ProjectPermission.MANAGE_VIEWS, authorizationContext), "manage views"
        )
    }

}