package gropius.repository

import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository
import org.springframework.data.neo4j.repository.support.ReactiveCypherdslConditionExecutor
import org.springframework.data.repository.NoRepositoryBean

/**
 * Meta-repository that included [ReactiveNeo4jRepository] and [ReactiveCypherdslConditionExecutor]
 *
 * @param T the type of node
 * @param ID the type of id used
 */
@NoRepositoryBean
interface GropiusRepository<T : Any, ID : Any> : ReactiveNeo4jRepository<T, ID>, ReactiveCypherdslConditionExecutor<T>