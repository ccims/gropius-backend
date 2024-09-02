package gropius

import io.github.graphglue.data.repositories.EnableGraphglueRepositories
import org.neo4j.driver.Driver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager

/**
 * Configuration provider for the neo4j transaction manager
 * @param configurableApplicationContext Reference for the spring instance of ConfigurableApplicationContext
 */
@Configuration
class SyncConfiguration(
    val configurableApplicationContext: ConfigurableApplicationContext
) {
    @Autowired
    fun setMapKeyDotReplacement(mappingMongoConverter: MappingMongoConverter) {
        mappingMongoConverter.setMapKeyDotReplacement("_FUCKDOT_")
    }

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
class Application {}

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
