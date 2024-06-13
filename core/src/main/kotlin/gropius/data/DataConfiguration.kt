package gropius.data

import org.neo4j.cypherdsl.core.renderer.Dialect
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.neo4j.core.convert.Neo4jConversions
import java.time.OffsetDateTime
import org.neo4j.cypherdsl.core.renderer.Configuration as CypherDslConfiguration

/**
 * Configuration for the Neo4j database
 */
@Configuration
class DataConfiguration {

    /**
     * Provides the converts
     * Includes a converter for [OffsetDateTime]
     *
     * @return the coverter set
     */
    @Bean
    fun neo4jConversion(): Neo4jConversions {
        return Neo4jConversions(setOf(OffsetDateTimeConverter()))
    }

    /**
     * Provides the Cypher DSL configuration
     *
     * @return the Cypher DSL configuration
     */
    @Bean
    fun cypherDslConfiguration(): CypherDslConfiguration {
        return CypherDslConfiguration.newConfig().withDialect(Dialect.NEO4J_5).build()
    }

}