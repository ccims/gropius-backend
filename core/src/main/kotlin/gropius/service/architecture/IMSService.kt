package gropius.service.architecture

import gropius.authorization.GropiusAuthorizationContext
import gropius.dto.input.architecture.CreateIMSInput
import gropius.dto.input.architecture.UpdateIMSInput
import gropius.dto.input.common.DeleteNodeInput
import gropius.model.architecture.IMS
import gropius.model.template.IMSTemplate
import gropius.model.user.permission.GlobalPermission
import gropius.model.user.permission.NodePermission
import gropius.repository.architecture.IMSRepository
import gropius.repository.findById
import gropius.repository.template.IMSTemplateRepository
import gropius.service.common.NamedNodeService
import gropius.service.template.TemplateService
import gropius.service.template.TemplatedNodeService
import gropius.service.user.permission.IMSPermissionService
import io.github.graphglue.authorization.Permission
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service

/**
 * Service for [IMS]s. Provides functions to create, update and delete
 *
 * @param repository the associated repository used for CRUD functionality
 * @param imsPermissionService used to create the initial permission for a created [IMS]
 * @param imsTemplateRepository used to get [IMSTemplate]s by id
 * @param templatedNodeService service used to update templatedFields
 */
@Service
class IMSService(
    repository: IMSRepository,
    private val imsPermissionService: IMSPermissionService,
    private val imsTemplateRepository: IMSTemplateRepository,
    private val templatedNodeService: TemplatedNodeService,
    private val templateService: TemplateService
) : NamedNodeService<IMS, IMSRepository>(repository) {

    /**
     * Creates a new [IMS] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines the [IMS]
     * @return the saved created [IMS]
     */
    suspend fun createIMS(
        authorizationContext: GropiusAuthorizationContext, input: CreateIMSInput
    ): IMS {
        input.validate()
        val user = getUser(authorizationContext)
        checkPermission(
            user, Permission(GlobalPermission.CAN_CREATE_IMSS, authorizationContext), "create IMSs"
        )
        val template = imsTemplateRepository.findById(input.template)
        val templatedFields = templatedNodeService.validateInitialTemplatedFields(template, input)
        val ims = IMS(input.name, input.description, templatedFields)
        templateService.validateTemplateUsage(template)
        ims.template().value = template
        imsPermissionService.createDefaultPermission(user, ims)
        return repository.save(ims).awaitSingle()
    }

    /**
     * Updates a [IMS] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [IMS] to update and how
     * @return the updated [IMS]
     */
    suspend fun updateIMS(
        authorizationContext: GropiusAuthorizationContext, input: UpdateIMSInput
    ): IMS {
        input.validate()
        val ims = repository.findById(input.id)
        checkPermission(
            ims, Permission(NodePermission.ADMIN, authorizationContext), "update the IMS"
        )
        templatedNodeService.updateTemplatedFields(ims, input, false)
        imsPermissionService.updatePermissionsOfNode(
            ims, input.addedPermissions, input.removedPermissions, authorizationContext
        )
        return repository.save(ims).awaitSingle()
    }

    /**
     * Deletes a [IMS] by id
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [IMS] to delete
     */
    suspend fun deleteIMS(
        authorizationContext: GropiusAuthorizationContext, input: DeleteNodeInput
    ) {
        input.validate()
        val ims = repository.findById(input.id)
        checkPermission(
            ims, Permission(NodePermission.ADMIN, authorizationContext), "delete the IMS"
        )
        repository.delete(ims).awaitSingleOrNull()
    }

}