package gropius.sync.github

import com.apollographql.apollo3.ApolloClient
import gropius.sync.github.generated.TimelineReadQuery
import gropius.sync.github.generated.TimelineReadQuery.Data.Companion.metaData
import gropius.sync.github.generated.TimelineReadQuery.Data.Node.Companion.asIssue
import gropius.sync.github.generated.fragment.TimelineItemData
import gropius.sync.github.generated.fragment.TimelineItemData.Companion.asNode
import gropius.sync.github.model.IssueInfo
import gropius.sync.github.model.TimelineItemDataCache
import gropius.sync.github.repository.IssueInfoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.*
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import java.time.OffsetDateTime

/**
 * Implementation of Grabber to retrieve timeline items and cache them in the database
 */
class TimelineGrabber(
    /**
     * Reference for the spring instance of IssueInfoRepository
     */
    private val issueInfoRepository: IssueInfoRepository,
    /**
     * Reference for the spring instance of ReactiveMongoOperations
     */
    private val mongoOperations: ReactiveMongoOperations,
    /**
     * github id of the issue
     */
    private val id: String
) : Grabber<TimelineItemData>() {

    /**
     * The response of a single step of timeline grabbing
     */
    class TimelineStepResponse(
        /**
         * The raw github response
         */
        val content: TimelineReadQuery.Data
    ) : StepResponse<TimelineItemData> {
        override val metaData get() = content.metaData()!!
        override val nodes
            get() = content.node!!.asIssue()!!.timelineItems.nodes!!.filterNotNull()
        override val totalCount get() = content.node!!.asIssue()!!.timelineItems.totalCount
        override val pageInfoData get() = content.node!!.asIssue()!!.timelineItems.pageInfo
    }

    override suspend fun writeTimestamp(time: OffsetDateTime) {
        mongoOperations.update<IssueInfo>().matching(Query.query(Criteria.where(IssueInfo::githubId.name).`is`(id)))
            .apply(
                Update().max(IssueInfo::lastAccess.name, time)
            ).firstAndAwait()
    }

    override suspend fun readTimestamp(): OffsetDateTime? {
        return issueInfoRepository.findByGithubId(id)?.lastAccess
    }

    override suspend fun addToCache(node: TimelineItemData): ObjectId {
        return mongoOperations.update<TimelineItemDataCache>()
            .matching(Query.query(Criteria.where(TimelineItemDataCache::githubId.name).`is`(node.asNode()!!.id)))
            .apply(Update.update(TimelineItemDataCache::data.name, node).set(TimelineItemDataCache::issue.name, id))
            .withOptions(FindAndModifyOptions.options().upsert(true).returnNew(true)).findAndModify().awaitSingle().id!!
    }

    override suspend fun iterateCache(): Flow<TimelineItemData> {
        return mongoOperations.query<TimelineItemDataCache>().matching(
            Query.query(
                Criteria.where(TimelineItemDataCache::issue.name).`is`(id)
            ).addCriteria(Criteria.where(TimelineItemDataCache::attempts.name).not().gte(7))
        ).all().asFlow().map { it.data }
    }

    override suspend fun removeFromCache(node: String) {
        mongoOperations.remove<TimelineItemDataCache>(
            Query.query(
                Criteria.where(TimelineItemDataCache::issue.name).`is`(id)
            ).addCriteria(Criteria.where(TimelineItemDataCache::githubId.name).`is`(node))
        ).awaitSingle()
    }

    override suspend fun increasedFailedCache(node: String) {
        mongoOperations.update<TimelineItemDataCache>().matching(Query.query(Criteria.where("data.id").`is`(node)))
            .apply(Update().inc(TimelineItemDataCache::attempts.name, 1)).firstAndAwait()
    }

    override fun nodeId(node: TimelineItemData): String {
        return node.asNode()?.id!!
    }

    override suspend fun grabStep(
        since: OffsetDateTime?, cursor: String?, count: Int
    ): StepResponse<TimelineItemData>? {
        //TODO: Pool the clients by accessing user
        val apolloClient = ApolloClient.Builder().serverUrl("https://api.github.com/graphql")
            .addHttpHeader("Authorization", "bearer " + System.getenv("GITHUB_DUMMY_PAT")!!).build()
        val response = apolloClient.query(
            TimelineReadQuery(
                issue = id, since = since, cursor = cursor, issueCount = count
            )
        ).execute()
        return if (response.data?.node?.asIssue()?.timelineItems?.nodes != null) {
            TimelineGrabber.TimelineStepResponse(response.data!!)
        } else {
            null
        }
    }
}