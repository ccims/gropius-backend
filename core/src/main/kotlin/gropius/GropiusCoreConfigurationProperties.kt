package gropius

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for the core module
 *
 * @param allowedAvatarUrlPrefixes a list of allowed prefixes for avatar urls
 */
@ConfigurationProperties("gropius.core")
data class GropiusCoreConfigurationProperties(val allowedAvatarUrlPrefixes: List<String> = emptyList()) {

    init {
        for (prefix in allowedAvatarUrlPrefixes) {
            if (!prefix.matches("^http(s)?://.*/.*".toRegex())) {
                throw IllegalArgumentException("Invalid avatar url prefix: $prefix")
            }
        }
    }

}