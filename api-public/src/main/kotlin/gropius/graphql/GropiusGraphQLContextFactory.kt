package gropius.graphql

import com.expediagroup.graphql.generator.extensions.plus
import com.expediagroup.graphql.server.spring.execution.DefaultSpringGraphQLContextFactory
import graphql.GraphQLContext
import gropius.GropiusPublicApiConfigurationProperties
import gropius.authorization.GropiusAuthorizationContext
import gropius.model.user.GropiusUser
import gropius.model.user.permission.NodePermission
import gropius.repository.user.GropiusUserRepository
import io.github.graphglue.authorization.AuthorizationContext
import io.github.graphglue.authorization.Permission
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.server.ResponseStatusException
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * String that the audience claim of a JWT must contain for it to be accepted
 */
private val JWT_AUDIENCE_BACKEND = "backend";

/**
 * Generates the GraphQL context map
 *
 * @param gropiusUserRepository used to get the user
 * @param gropiusPublicApiConfigurationProperties used to determine if authentication is optional
 */
@Component
class GropiusGraphQLContextFactory(
    private val gropiusUserRepository: GropiusUserRepository,
    private val gropiusPublicApiConfigurationProperties: GropiusPublicApiConfigurationProperties
) : DefaultSpringGraphQLContextFactory() {

    /**
     * Jwt parser based on the secret defined by [gropiusPublicApiConfigurationProperties]
     */
    private val jwtParser = Jwts.parser()
        .verifyWith(publicKey)
        .build()

    /**
     * Public key used to verify JWTs
     */
    @OptIn(ExperimentalEncodingApi::class)
    private val publicKey: PublicKey get() {
        val publicKeyPem = String(Base64.decode(gropiusPublicApiConfigurationProperties.jwtPublicKey))
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("\n", "")
            .replace("-----END PUBLIC KEY-----", "")
        val encoded = Base64.decode(publicKeyPem)
        return KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(encoded))
    }

    override suspend fun generateContext(request: ServerRequest): GraphQLContext {
        val token = request.headers().firstHeader("Authorization")
        val additionalContextEntries = if (token == null) {
            if (gropiusPublicApiConfigurationProperties.debugNoAuthentication) {
                emptyMap()
            } else {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "No authentication token provided")
            }
        } else {
            val user = verifyToken(token)
            val isAdmin = user.isAdmin || gropiusPublicApiConfigurationProperties.debugNoAuthentication
            val context = GropiusAuthorizationContext(user.rawId!!, !isAdmin)
            if (isAdmin) {
                mapOf(AuthorizationContext::class to context)
            } else {
                mapOf(Permission::class to Permission(NodePermission.READ, context))
            }

        }
        return super.generateContext(request) + additionalContextEntries
    }

    /**
     * Verifies a jwt and returns the [GropiusUser] defined by the id in subject
     *
     * @param token the jwt, possibly starting with `"Bearer "`
     * @return the user defined by the id in subject
     * @throws IllegalStateException if the user does not exist or if the jwt is invalid
     */
    private suspend fun verifyToken(token: String): GropiusUser {
        val tokenWithoutBearer = token.replace("Bearer ", "", true)
        val jwt = try {
            jwtParser.parseSignedClaims(tokenWithoutBearer)
        } catch (e: JwtException) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid jwt")
        }
        if (!jwt.payload.audience.contains(JWT_AUDIENCE_BACKEND)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not a backend token")
        }
        val user = jwt.payload.subject!!
        return gropiusUserRepository.findById(user).awaitSingle()
    }

}