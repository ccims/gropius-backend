package gropius.graphql

import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.expediagroup.graphql.generator.extensions.getOrDefault
import com.expediagroup.graphql.server.operations.Mutation
import com.expediagroup.graphql.server.spring.execution.SpringDataFetcher
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.future
import kotlinx.coroutines.future.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import io.github.graphglue.model.Node
import org.springframework.context.ApplicationContext
import org.springframework.dao.TransientDataAccessException
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.IdentityHashMap
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.*

/**
 * Delay before the next retry
 */
private val MUTATION_REPETITION_DELAYS = listOf(150L, 500L, 500L)

/**
 * FunctionDataFetcher which handles files parameter mapping for parameters of GraphQL type JSON correctly
 * Allows input classes to have `lateinit` properties which are correctly deserialized.
 * Also handles wrapping the result of a mutation when using [AutoPayloadType]
 * Extends [SpringDataFetcher]
 *
 * @param target if present, the object on which the function is invoked
 * @param function the function the data fetcher calls
 * @param applicationContext used to obtain Spring beans
 */
class GropiusFunctionDataFetcher(
    private val target: Any?, private val function: KFunction<*>, applicationContext: ApplicationContext
) : SpringDataFetcher(target, function, applicationContext) {

    override fun get(environment: DataFetchingEnvironment): Any? {
        val source = environment.getSource<Any>()
        if (source is Node && function.name == "getFromGraphQL") {
            return loadNodeField(environment, source)
        }
        return if (target is Mutation) {
            runMutationFunction(environment)
        } else {
            super.get(environment)
        }
    }

    /** Graphglue 8 caches lazy field results in an ordinary mutable map on each node.
     * Mutation result fields are resolved concurrently, unlike prefetched query results.
     * Serialize lazy loads for the same instance, keeping unrelated nodes parallel.
     */
    private fun loadNodeField(environment: DataFetchingEnvironment, source: Node): CompletableFuture<Any?> {
        val context = environment.graphQlContext
        val locks = synchronized(context) {
            context.get<NodeFieldLocks>(NodeFieldLocks::class.java) ?: NodeFieldLocks().also {
                context.put(NodeFieldLocks::class.java, it)
            }
        }
        val mutex = locks.forNode(source)
        return context.getOrDefault(CoroutineScope(EmptyCoroutineContext)).future {
            mutex.withLock {
                val result = super.get(environment)
                if (result is CompletionStage<*>) result.await() else result
            }
        }
    }

    private class NodeFieldLocks {
        private val locks = IdentityHashMap<Node, Mutex>()
        @Synchronized fun forNode(node: Node): Mutex = locks.getOrPut(node) { Mutex() }
    }

    override fun mapParameterToValue(param: KParameter, environment: DataFetchingEnvironment): Pair<KParameter, Any?>? {
        return when {
            param.hasAnnotation<GraphQLIgnore>() -> super.mapParameterToValue(param, environment)
            param.type.isSubtypeOf(DataFetchingEnvironment::class.createType()) -> param to environment
            else -> param to convertArgumentValueFromParam(param, environment.arguments)
        }
    }

    /**
     * Invokes the mutation function and handles transient exceptions
     *
     * @param environment the DataFetchingEnvironment
     * @return the result of the mutation
     */
    private fun runMutationFunction(environment: DataFetchingEnvironment): CompletableFuture<Any?> {
        val res = runMutationFunctionWithRetries(environment)
        return handleAutoPayloadType(res)
    }

    /**
     * Invokes the mutation function and handles transient exceptions
     *
     * @param environment the DataFetchingEnvironment
     * @return the result of the mutation
     */
    private fun runMutationFunctionWithRetries(environment: DataFetchingEnvironment): CompletableFuture<Any?> {
        val parameterValues = getParameters(function, environment) + Pair(function.instanceParameter!!, target!!)
        return environment.graphQlContext.getOrDefault(CoroutineScope(EmptyCoroutineContext)).future {
            repeat(MUTATION_REPETITION_DELAYS.size + 1) {
                try {
                    try {
                        return@future function.callSuspendBy(parameterValues)
                    } catch (exception: InvocationTargetException) {
                        throw exception.cause ?: exception
                    }
                } catch (exception: TransientDataAccessException) {
                    val minDelay = MUTATION_REPETITION_DELAYS.getOrNull(it) ?: throw exception
                    delay((minDelay..minDelay * 2).random())
                }
            }
        }
    }

    /**
     * Handles the [AutoPayloadType] annotation if present
     *
     * @param res the result of the mutation
     * @return the result of the mutation, possibly wrapped due to [AutoPayloadType]
     */
    private fun handleAutoPayloadType(res: CompletableFuture<Any?>): CompletableFuture<Any?> {
        return if (function.hasAnnotation<AutoPayloadType>()) {
            res.thenApply { PayloadWrapper(it) }
        } else {
            res
        }
    }
}
