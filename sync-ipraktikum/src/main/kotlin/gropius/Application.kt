package gropius

import io.github.graphglue.data.repositories.EnableGraphglueRepositories
import org.neo4j.driver.Driver
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.transaction.reactive.TransactionalOperator

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableGraphglueRepositories
@EnableReactiveMongoRepositories
@EnableScheduling
class Application {
    @Bean
    fun reactiveTransactionManager(driver: Driver, database: ReactiveDatabaseSelectionProvider) =
        ReactiveNeo4jTransactionManager(driver, database)

    @Bean
    fun syncTransactions(manager: ReactiveNeo4jTransactionManager): TransactionalOperator =
        TransactionalOperator.create(manager)
}

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
