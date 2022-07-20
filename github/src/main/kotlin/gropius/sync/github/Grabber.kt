package gropius.sync.github

import gropius.sync.github.generated.fragment.MetaData
import gropius.sync.github.generated.fragment.PageInfoData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import org.bson.types.ObjectId
import java.time.OffsetDateTime

/**
 * Requests data from github using steps that are managed over restarts for processing items (e.g. issues, timeline, ...)
 * @param T type of the resulting grabbed item
 */
abstract class Grabber<T : Any> {

    /**
     * The response of a single step
     * @param T type of the resulting grabbed item (same as in Grabber)
     */
    protected interface StepResponse<T : Any> {
        /**
         * Global metadata like rate limit
         */
        val metaData: MetaData

        /**
         * The resulting nodes
         */
        val nodes: Iterable<T>

        /**
         * Total node count (as github didn't put it in the metadata section)
         */
        val totalCount: Int

        /**
         * The pageinfo block containing next cursor
         */
        val pageInfoData: PageInfoData
    }

    /**
     * Request a single step from github
     * @param since First entry that could be relevant, null for all-time
     * @param cursor Cursor of the pageInfoData of the previous query
     * @param count preferred number of entries to request this request
     * @return result, null for no entries
     */
    protected abstract suspend fun grabStep(since: OffsetDateTime?, cursor: String?, count: Int): StepResponse<T>?

    /**
     * Set the newest timestamp to the given timestamp or newer
     * @param time Timestamp to set the database to (if older exists)
     */
    protected abstract suspend fun writeTimestamp(time: OffsetDateTime)

    /**
     * Read the highest timestamp from the database
     * @return returns the highest timestamp or null if none exists
     */
    protected abstract suspend fun readTimestamp(): OffsetDateTime?

    /**
     * Add a node into the database cache
     * @param node to add
     * @return the inserted id
     */
    protected abstract suspend fun addToCache(node: T): ObjectId

    /**
     * Iterate through all nodes of the database cache
     * @return Iterate through all nodes currently in the cache
     */
    protected abstract suspend fun iterateCache(): Flow<T>

    /**
     * Remove a single id from the database cache
     * @param node The id in the node to query (not mongo id)
     */
    protected abstract suspend fun removeFromCache(node: String)

    /**
     * Increase the number of failed attempts for a node in the cache
     * @param node Node id to increase the number on (not mongo id)
     */
    protected abstract suspend fun increasedFailedCache(node: String)

    /**
     * Get the id from a node
     * @param node The node as object
     * @return The node as string
     */
    protected abstract fun nodeId(node: T): String

    /**
     * Handle the resuöts of a step response (currently inserting into cache)
     * @param response the response
     */
    private suspend fun handleStepResponse(response: Grabber.StepResponse<T>) {
        for (node in response.nodes) {
            addToCache(node)
        }
    }

    /**
     * Update nodes with fresh data
     */
    suspend fun requestNewNodes() {
        var githubCursor: String? = null
        var remaining = 1
        do {
            println("Stepping " + (remaining.coerceIn(1, 100)) + " nodes from " + githubCursor)
            val response = grabStep(readTimestamp(), githubCursor, remaining.coerceIn(1, 100))
            if (response != null) {
                if (githubCursor == null) {
                    remaining = response.totalCount //TODO: Should be subtracted with the number of known nodes
                    println("Requesting $remaining nodes")
                }
                githubCursor = response.pageInfoData?.endCursor
                handleStepResponse(response);
            }
        } while ((githubCursor != null) && (remaining > 0))
    }

    /**
     * Iterate through all nodes that have not yet been done
     * @param callback Iterate through all nodes in the cache. Has to return a new date time if successful and null if failed and should be retried
     */
    suspend fun iterate(callback: suspend (atom: T) -> OffsetDateTime?) {
        val times = mutableListOf<OffsetDateTime>()
        for (node in iterateCache().toList()) {
            increasedFailedCache(nodeId(node))
            val newMaxTime = callback(node)
            if (newMaxTime != null) {
                times.add(newMaxTime)
                removeFromCache(nodeId(node))
            }
        }
        if (times.size > 0) {
            writeTimestamp(times.maxOrNull()!!)
        }
    }
}