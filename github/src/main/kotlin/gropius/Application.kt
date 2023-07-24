package gropius

import gropius.sync.github.SyncSelector
import io.github.graphglue.data.repositories.EnableGraphglueRepositories
import kotlinx.coroutines.runBlocking
import org.neo4j.driver.Driver
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager
import kotlin.system.exitProcess

/**
 * Configuration provider for the neo4j transaction manager
 * @param configurableApplicationContext Reference for the spring instance of ConfigurableApplicationContext
 */
@Configuration
class SyncConfiguration(
    val configurableApplicationContext: ConfigurableApplicationContext
) {
    /**
     * Necessary transaction manager
     *
     * @param driver used Neo4j driver
     * @param databaseNameProvider Neo4j database provider
     * @return the generated transaction manager
     */
    @Bean
    fun reactiveTransactionManager(
        driver: Driver, databaseNameProvider: ReactiveDatabaseSelectionProvider
    ): ReactiveNeo4jTransactionManager {
        return ReactiveNeo4jTransactionManager(driver, databaseNameProvider)
    }
}

/**
 * Main Application
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableGraphglueRepositories
@EnableReactiveMongoRepositories
class Application : CommandLineRunner {
    /**
     * Reference for the spring instance of SyncSelector
     */
    @Autowired
    lateinit var syncSelector: SyncSelector

    /**
     * Logger used to print notifications
     */
    private val logger = LoggerFactory.getLogger(Application::class.java)

    override fun run(vararg args: String?) {
        try {
            runBlocking {
                syncSelector.sync()
            }
        } catch (e: Exception) {
            logger.error("Error in sync", e)
        } catch (e: Error) {
            e.printStackTrace()
            logger.error("Throwable in sync", e)
            exitProcess(1)//TODO: remove ASAP
        } finally {
            exitProcess(0)//TODO: remove ASAP
        }
    }
}

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
