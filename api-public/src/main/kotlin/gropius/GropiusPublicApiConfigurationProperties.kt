package gropius

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for the public API
 *
 * @param debugNoAuthentication if `true`, all queries and most mutations don't require authentication to be present
 * @param jwtSecret secret used for JWT validation
 */
@ConfigurationProperties("gropius.api.public")
data class GropiusPublicApiConfigurationProperties(val debugNoAuthentication: Boolean = false, val jwtSecret: String)