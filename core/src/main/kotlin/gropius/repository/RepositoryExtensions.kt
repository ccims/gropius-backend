package gropius.repository

import com.expediagroup.graphql.generator.scalars.ID
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.repository.reactive.ReactiveCrudRepository

/**
 * Wrapper for [ReactiveCrudRepository.findById] which unwraps the provided [id] and awaits the result
 *
 * @param T the node type of the repository
 * @param id the id of the node to find
 * @return the found node
 */
suspend fun <T> GropiusRepository<T, String>.findById(id: ID): T {
    return findById(id.value).awaitSingle()
}

/**
 * Wrapper for [ReactiveCrudRepository.findAllById] which unwraps the provided [ids] and awaits the result
 *
 * @param T the node type of the repository
 * @param ids the ids of the nodes to find
 * @return the found nodes
 */
suspend fun <T> GropiusRepository<T, String>.findAllById(ids: Iterable<ID>): List<T> {
    return findAllById(ids.map { it.value }).collectList().awaitSingle()
}