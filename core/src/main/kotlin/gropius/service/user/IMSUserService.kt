package gropius.service.user

import gropius.dto.input.ifPresent
import gropius.dto.input.user.CreateIMSUserInput
import gropius.dto.input.user.UpdateIMSUserInput
import gropius.model.user.IMSUser
import gropius.model.architecture.IMS
import gropius.repository.architecture.IMSRepository
import gropius.repository.findById
import gropius.repository.user.IMSUserRepository
import gropius.service.common.AbstractExtensibleNodeService
import gropius.service.template.TemplatedNodeService
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service

/**
 * Service for [IMSUser]s. Provides function to create and update
 *
 * @param repository the associated repository used for CRUD functionality
 * @param imsRepository used to get [IMS]s by id
 * @param templatedNodeService used to update templatedFields
 */
@Service
class IMSUserService(
    repository: IMSUserRepository,
    private val imsRepository: IMSRepository,
    private val templatedNodeService: TemplatedNodeService
) : AbstractExtensibleNodeService<IMSUser, IMSUserRepository>(repository) {

    /**
     * Creates a new [IMSUser] based on the provided [input]
     * Does not check the authorization status
     * This MUST NOT be exposed via the public API
     *
     * @param input defines the [IMSUser]
     * @return the created [IMSUser]
     */
    suspend fun createIMSUser(input: CreateIMSUserInput): IMSUser {
        input.validate()
        val ims = imsRepository.findById(input.ims)
        val template = ims.template().value.imsUserTemplate().value
        val templatedFields = templatedNodeService.validateInitialTemplatedFields(template, input)
        val imsUser = IMSUser(input.displayName, input.email, null, input.username, templatedFields)
        imsUser.template().value = template
        imsUser.ims().value = ims
        if (input.gropiusUser != null) {
            imsUser.gropiusUser().value = gropiusUserRepository.findById(input.gropiusUser)
        }
        createdExtensibleNode(imsUser, input)
        return repository.save(imsUser).awaitSingle()
    }

    /**
     * Updates a [IMSUser] based on the provided [input]
     * Does not check the authorization status
     * This MUST NOT be exposed via the public API
     *
     * @param input defines which [IMSUser] to update and how
     * @return the updated [IMSUser]
     */
    suspend fun updateIMSUser(input: UpdateIMSUserInput): IMSUser {
        input.validate()
        val imsUser = repository.findById(input.id)
        input.displayName.ifPresent {
            imsUser.displayName = it
        }
        input.email.ifPresent {
            imsUser.email = it
        }
        input.username.ifPresent {
            imsUser.username = it
        }
        input.gropiusUser.ifPresent {
            if (it == null) {
                imsUser.gropiusUser().value = null
            } else {
                imsUser.gropiusUser().value = gropiusUserRepository.findById(it)
            }
        }
        templatedNodeService.updateTemplatedFields(imsUser, input, false)
        updateExtensibleNode(imsUser, input)
        return repository.save(imsUser).awaitSingle()
    }

}