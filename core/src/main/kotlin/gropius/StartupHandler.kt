package gropius

import io.github.graphglue.data.DefaultIndexCreator
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component

/**
 * Handles startup tasks like creating indices.
 *
 * @param defaultIndexCreator the [DefaultIndexCreator] to use
 * @param gropiusCoreConfigurationProperties used to decide whether to create indices on startup
 */
@Component
class StartupHandler(
    val defaultIndexCreator: DefaultIndexCreator,
    val gropiusCoreConfigurationProperties: GropiusCoreConfigurationProperties
) {

    /**
     * Called after startup
     */
    @PostConstruct
    fun onStartup() {
        if (gropiusCoreConfigurationProperties.createIndicesOnStartup) {
            defaultIndexCreator.createDefaultIndices()
        }
    }

}