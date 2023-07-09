package gropius.service.user

import gropius.GropiusCoreConfigurationProperties
import gropius.authorization.GropiusAuthorizationContext
import gropius.dto.input.ifPresent
import gropius.dto.input.user.CreateGropiusUserInput
import gropius.dto.input.user.UpdateGropiusUserInput
import gropius.model.user.GropiusUser
import gropius.repository.findById
import gropius.repository.user.GropiusUserRepository
import gropius.service.common.AbstractExtensibleNodeService
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service
import java.net.URI

/**
 * Service for [GropiusUser]s. Provides functions to create and update
 *
 * @param repository the associated repository used for CRUD functionality
 * @param gropiusCoreConfigurationProperties the configuration properties
 */
@Service
class GropiusUserService(
    repository: GropiusUserRepository,
    private val gropiusCoreConfigurationProperties: GropiusCoreConfigurationProperties
) : AbstractExtensibleNodeService<GropiusUser, GropiusUserRepository>(repository) {

    /**
     * Updates a [GropiusUser] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [GropiusUser] to update and how
     * @return the updated [GropiusUser]
     */
    suspend fun updateGropiusUser(
        authorizationContext: GropiusAuthorizationContext, input: UpdateGropiusUserInput
    ): GropiusUser {
        input.validate()
        val gropiusUser = repository.findById(input.id)
        if (authorizationContext.userId != gropiusUser.rawId) {
            checkIsAdmin(authorizationContext, "update this GropiusUser")
        }
        input.displayName.ifPresent {
            gropiusUser.displayName = it
        }
        input.email.ifPresent {
            gropiusUser.email = it
        }
        input.avatar.ifPresent {
            if (it != null) {
                validateAvatar(it)
            }
            gropiusUser.avatar = it
        }
        input.isAdmin.ifPresent {
            checkIsAdmin(authorizationContext, "update isAdmin of a GropiusUser")
            gropiusUser.isAdmin = it
        }
        updateExtensibleNode(gropiusUser, input)
        return repository.save(gropiusUser).awaitSingle()
    }

    /**
     * Creates a new [GropiusUser] based on the provided [input]
     * Does not check the authorization status
     * Checks that no [GropiusUser] with the same username exists
     * This MUST NOT be exposed via the public API
     *
     * @param input defines the [GropiusUser]
     * @return the created [GropiusUser]
     */
    suspend fun createGropiusUser(input: CreateGropiusUserInput): GropiusUser {
        input.validate()
        if (repository.existsByUsername(input.username)) {
            throw IllegalArgumentException("A GropiusUser with the specified username already exists")
        }
        if (input.avatar != null) {
            validateAvatar(input.avatar)
        }
        val gropiusUser = GropiusUser(input.displayName, input.email, input.avatar, input.username, input.isAdmin)
        createdExtensibleNode(gropiusUser, input)
        return repository.save(gropiusUser).awaitSingle()
    }

    /**
     * Finds a [GropiusUser] by username
     * No authorization status check necessary
     *
     * @param username the username of the user to get
     * @return the found user
     */
    suspend fun findGropiusUserByUsername(username: String): GropiusUser {
        return repository.findByUsername(username)
            ?: throw IllegalStateException("User with provided username does not exist")
    }

    /**
     * Validates that a URI can be used as avatar for a [GropiusUser]
     *
     * @param avatar the URI to validate
     * @throws IllegalArgumentException otherwise
     */
    suspend fun validateAvatar(avatar: URI) {
        val asString = avatar.toASCIIString()
        val allowedPrefixes = gropiusCoreConfigurationProperties.allowedAvatarUrlPrefixes + "data://image/"
        if (allowedPrefixes.none { asString.startsWith(it) }) {
            throw IllegalArgumentException("Avatar URI is not allowed")
        }
    }

}