package gropius

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for the GitHub API
 *
 * @param maxMutationCount maximum number of mutations during single sync cycle
 */
@ConfigurationProperties("gropius.sync.github")
data class GithubConfigurationProperties(val maxMutationCount: Int = 100, val dieOnError: Boolean = false)