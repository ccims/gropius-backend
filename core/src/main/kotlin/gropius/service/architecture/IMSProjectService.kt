package gropius.service.architecture

import gropius.authorization.GropiusAuthorizationContext
import gropius.dto.input.architecture.CreateIMSProjectInput
import gropius.dto.input.architecture.UpdateIMSProjectInput
import gropius.dto.input.common.DeleteNodeInput
import gropius.model.architecture.IMS
import gropius.model.architecture.IMSIssue
import gropius.model.architecture.IMSProject
import gropius.model.architecture.Trackable
import gropius.model.user.permission.IMSPermission
import gropius.model.user.permission.NodePermission
import gropius.model.user.permission.TrackablePermission
import gropius.repository.architecture.IMSIssueRepository
import gropius.repository.architecture.IMSProjectRepository
import gropius.repository.architecture.IMSRepository
import gropius.repository.architecture.TrackableRepository
import gropius.repository.common.NodeRepository
import gropius.repository.findById
import gropius.service.common.NamedNodeService
import gropius.service.template.TemplatedNodeService
import io.github.graphglue.authorization.Permission
import io.github.graphglue.model.Node
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service

/**
 * Service for [IMSProject]s. Provides functions to create, update and delete
 *
 * @param repository the associated repository used for CRUD functionality
 * @param imsRepository used to get [IMS]s by id
 * @param trackableRepository used to get [Trackable]s by id
 * @param imsIssueRepository used to delete [IMSIssue]s when project is deleted
 * @param templatedNodeService service used to update templatedFields
 * @param nodeRepository used to delete [Node]s
 */
@Service
class IMSProjectService(
    repository: IMSProjectRepository,
    private val imsRepository: IMSRepository,
    private val trackableRepository: TrackableRepository,
    private val imsIssueRepository: IMSIssueRepository,
    private val templatedNodeService: TemplatedNodeService,
    private val nodeRepository: NodeRepository
) : NamedNodeService<IMSProject, IMSProjectRepository>(repository) {

    /**
     * Creates a new [IMSProject] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines the [IMSProject]
     * @return the saved created [IMSProject]
     */
    suspend fun createIMSProject(
        authorizationContext: GropiusAuthorizationContext, input: CreateIMSProjectInput
    ): IMSProject {
        input.validate()
        val ims = imsRepository.findById(input.ims)
        checkPermission(
            ims,
            Permission(IMSPermission.SYNC_TRACKABLES, authorizationContext),
            "create create projects on the specified IMS"
        )
        val trackable = trackableRepository.findById(input.trackable)
        checkPermission(
            trackable, Permission(TrackablePermission.MANAGE_IMS, authorizationContext), "sync the Trackable to an IMS"
        )
        val template = ims.template().value.imsProjectTemplate().value
        val templatedFields = templatedNodeService.validateInitialTemplatedFields(template, input)
        val imsProject = IMSProject(input.name, input.description, templatedFields)
        imsProject.template().value = template
        imsProject.ims().value = ims
        imsProject.trackable().value = trackable
        return repository.save(imsProject).awaitSingle()
    }

    /**
     * Updates a [IMSProject] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [IMSProject] to update and how
     * @return the updated [IMSProject]
     */
    suspend fun updateIMSProject(
        authorizationContext: GropiusAuthorizationContext, input: UpdateIMSProjectInput
    ): IMSProject {
        input.validate()
        val imsProject = repository.findById(input.id)
        checkPermission(
            imsProject.ims().value,
            Permission(IMSPermission.SYNC_TRACKABLES, authorizationContext),
            "update the IMSProject due to missing Permission on the IMS"
        )
        checkPermission(
            imsProject.trackable().value,
            Permission(TrackablePermission.MANAGE_IMS, authorizationContext),
            "update the IMSProject due to missing Permission on the Trackable"
        )
        updateNamedNode(imsProject, input)
        templatedNodeService.updateTemplatedFields(imsProject, input)
        return repository.save(imsProject).awaitSingle()
    }

    /**
     * Deletes a [IMSProject] by id
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [IMSProject] to delete
     */
    suspend fun deleteIMSProject(
        authorizationContext: GropiusAuthorizationContext, input: DeleteNodeInput
    ) {
        input.validate()
        val imsProject = repository.findById(input.id)
        if (!evaluatePermission(
                imsProject.ims().value, Permission(NodePermission.ADMIN, authorizationContext)
            ) && !evaluatePermission(
                imsProject.trackable().value, Permission(TrackablePermission.MANAGE_IMS, authorizationContext)
            )
        ) {
            throw IllegalArgumentException("User does not have Permission to delete the IMSProject")
        }
        deleteIMSProject(imsProject)
    }

    /**
     * Deletes a [IMSProject]
     * Does not check the authorization status
     *
     * @param imsProject the [IMSProject] to delete
     */
    suspend fun deleteIMSProject(imsProject: IMSProject) {
        nodeRepository.deleteAll(getNodesToDelete(imsProject)).awaitSingleOrNull()
    }

    /**
     * Gets all nodes to delete when this [IMSProject] is deleted, including the [IMSProject] itself.
     *
     * @param node the [IMSProject] to delete
     * @return all nodes to delete when this [IMSProject] is deleted, including the [IMSProject] itself
     */
    suspend fun getNodesToDelete(node: IMSProject): Collection<Node> {
        return node.imsIssues() + node
    }

}