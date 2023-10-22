package gropius

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for the core module
 *
 * @param allowedAvatarUrlPrefixes a list of allowed prefixes for avatar urls
 * @param createIndicesOnStartup if true, indices will be created on startup
 */
@ConfigurationProperties("gropius.core")
data class GropiusCoreConfigurationProperties(
    val allowedAvatarUrlPrefixes: List<String> = emptyList(),
    val createIndicesOnStartup: Boolean = true
) {

    init {
        for (prefix in allowedAvatarUrlPrefixes) {
            if (!prefix.matches("^http(s)?://.*/.*".toRegex())) {
                throw IllegalArgumentException("Invalid avatar url prefix: $prefix")
            }
        }
    }

}