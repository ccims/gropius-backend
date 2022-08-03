package gropius.sync.github

import com.apollographql.apollo3.ApolloClient
import gropius.sync.github.generated.IssueReadQuery
import gropius.sync.github.generated.IssueReadQuery.Data.Companion.metaData
import gropius.sync.github.generated.IssueReadQuery.Data.Repository.Issues.PageInfo.Companion.pageInfoData
import gropius.sync.github.generated.fragment.IssueDataExtensive
import gropius.sync.github.model.IssueDataCache
import gropius.sync.github.model.RepositoryInfo
import gropius.sync.github.repository.RepositoryInfoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.*
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Criteria.where
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Query.query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.Update.update
import java.time.OffsetDateTime

/**
 * Implementation of Grabber to retrieve issues and cache them in the database
 * @param apolloClient Apollo Client to use
 */
class IssueGrabber(
    private val remote: RepoDescription,
    /**
     * Reference for the spring instance of RepositoryInfoRepository
     */
    private val repositoryInfoRepository: RepositoryInfoRepository,
    /**
     * Reference for the spring instance of ReactiveMongoOperations
     */
    private val mongoOperations: ReactiveMongoOperations,
   private val apolloClient: ApolloClient
) : Grabber<IssueDataExtensive>() {
    /**
     * The response of a single issue grabbing step
     */
    class IssueStepResponse(
        /**
         * The raw github response
         */
        val content: IssueReadQuery.Data) : StepResponse<IssueDataExtensive> {
        override val metaData get() = content.metaData()!!
        override val nodes get() = content.repository!!.issues.nodes!!.filterNotNull()
        override val totalCount get() = content.repository!!.issues.totalCount
        override val pageInfoData get() = content.repository!!.issues.pageInfo.pageInfoData()!!
    }

    override suspend fun writeTimestamp(time: OffsetDateTime) {
        mongoOperations.update<RepositoryInfo>().matching(
            query(where(RepositoryInfo::user.name).`is`(remote.owner)).addCriteria(
                where(RepositoryInfo::repo.name).`is`(
                    remote.owner
                )
            )
        ).apply(
            Update().max(RepositoryInfo::lastAccess.name, time)
        ).upsertAndAwait()
    }

    override suspend fun readTimestamp(): OffsetDateTime? {
        return repositoryInfoRepository.findByUserAndRepo(remote.owner, remote.repo)?.lastAccess
    }

    override suspend fun addToCache(node: IssueDataExtensive): ObjectId {
        return mongoOperations.update<IssueDataCache>()
            .matching(query(where(IssueDataCache::githubId.name).`is`(node.id))).apply(
                update(IssueDataCache::data.name, node).set(IssueDataCache::repo.name, remote.repo)
                    .set(IssueDataCache::user.name, remote.owner)
            ).withOptions(FindAndModifyOptions.options().upsert(true).returnNew(true)).findAndModify()
            .awaitSingle().id!!
    }

    override suspend fun iterateCache(): Flow<IssueDataExtensive> {
        return mongoOperations.query<IssueDataCache>().matching(
            query(
                where(IssueDataCache::user.name).`is`(remote.owner)
            ).addCriteria(where(IssueDataCache::repo.name).`is`(remote.repo))
                .addCriteria(where(IssueDataCache::attempts.name).not().gte(7))
        ).all().asFlow().map { it.data }
    }

    override suspend fun removeFromCache(node: String) {
        mongoOperations.remove<IssueDataCache>(
            query(
                where(IssueDataCache::user.name).`is`(remote.owner)
            ).addCriteria(where(IssueDataCache::repo.name).`is`(remote.repo)).addCriteria(where("data.id").`is`(node))
        ).awaitSingle()
    }

    override suspend fun increaseFailedCache(node: String) {
        mongoOperations.update<IssueDataCache>().matching(Query.query(Criteria.where("data.id").`is`(node)))
            .apply(Update().inc(IssueDataCache::attempts.name, 1)).firstAndAwait()
    }

    override fun nodeId(node: IssueDataExtensive): String {
        return node.id
    }

    override suspend fun grabStep(
        since: OffsetDateTime?, cursor: String?, count: Int
    ): StepResponse<IssueDataExtensive>? {
        val response = apolloClient.query(
            IssueReadQuery(
                repoOwner = remote.owner, repoName = remote.repo, since = since, cursor = cursor, issueCount = count
            )
        ).execute()
        return if (response.data?.repository?.issues?.nodes != null) {
            IssueStepResponse(response.data!!)
        } else {
            null
        }
    }
}