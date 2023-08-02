package gropius.sync.github.utils

import com.apollographql.apollo3.ApolloClient
import gropius.sync.github.config.IMSProjectConfig
import gropius.sync.github.generated.IssueReadQuery
import gropius.sync.github.generated.IssueReadQuery.Data.Companion.metaData
import gropius.sync.github.generated.IssueReadQuery.Data.Repository.Issues.PageInfo.Companion.pageInfoData
import gropius.sync.github.generated.fragment.IssueDataExtensive
import gropius.sync.github.model.RepoDescription
import gropius.sync.github.repository.RepositoryInfoRepository
import kotlinx.coroutines.flow.Flow
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import java.time.OffsetDateTime

/**
 * Implementation of Grabber to retrieve issues and cache them in the database
 * @param remote URL and name of the repo
 * @param imsProjectConfig Config of the IMSProject to filter for
 * @param apolloClient Apollo Client to use
 * @param repositoryInfoRepository Reference for the spring instance of RepositoryInfoRepository
 * @param mongoOperations Reference for the spring instance of ReactiveMongoOperations
 */
class IssueGrabber(
    private val remote: RepoDescription,
    private val repositoryInfoRepository: RepositoryInfoRepository,
    private val mongoOperations: ReactiveMongoOperations,
    private val apolloClient: ApolloClient,
    private val imsProjectConfig: IMSProjectConfig
) : Grabber<IssueDataExtensive>() {
    /**
     * The response of a single issue grabbing step
     * @param content The raw GitHub response
     */
    class IssueStepResponse(
        val content: IssueReadQuery.Data
    ) : StepResponse<IssueDataExtensive> {
        override val metaData get() = content.metaData()!!
        override val nodes get() = content.repository!!.issues.nodes!!.filterNotNull()
        override val totalCount get() = content.repository!!.issues.totalCount
        override val pageInfoData get() = content.repository!!.issues.pageInfo.pageInfoData()!!
    }

    override suspend fun writeTimestamp(time: OffsetDateTime) {
        /*mongoOperations.update<RepositoryInfo>().matching(
            query(where(RepositoryInfo::url.name).`is`(imsProjectConfig.url))
        ).apply(
            Update().max(RepositoryInfo::lastAccess.name, time).set(RepositoryInfo::user.name, remote.owner)
                .set(RepositoryInfo::repo.name, remote.repo)
        ).upsertAndAwait()*/
    }

    override suspend fun readTimestamp(): OffsetDateTime? {
        return null;//repositoryInfoRepository.findByUrl(imsProjectConfig.url)?.lastAccess
    }

    override suspend fun addToCache(node: IssueDataExtensive): ObjectId {
        /*return mongoOperations.update<IssueDataCache>().matching(
            query(
                where(IssueDataCache::githubId.name).`is`(node.id).and(IssueDataCache::url.name)
                    .`is`(imsProjectConfig.url)
            )
        ).apply(
            update(IssueDataCache::data.name, node)
        ).withOptions(FindAndModifyOptions.options().upsert(true).returnNew(true)).findAndModify().awaitSingle().id!!*/
        return null!!
    }

    override suspend fun iterateCache(): Flow<IssueDataExtensive> {
        /*return mongoOperations.query<IssueDataCache>().matching(
            query(
                where(IssueDataCache::url.name).`is`(imsProjectConfig.url)
            ).addCriteria(where(IssueDataCache::attempts.name).not().gte(7))
        ).all().asFlow().map { it.data }*/
        TODO();
    }

    override suspend fun removeFromCache(node: String) {
        /*mongoOperations.remove<IssueDataCache>(
            query(
                where(IssueDataCache::url.name).`is`(
                    imsProjectConfig.url
                )
            ).addCriteria(where("data.id").`is`(node))
        ).awaitSingle()*/
        TODO()
    }

    override suspend fun increaseFailedCache(node: String) {
        /*mongoOperations.update<IssueDataCache>().matching(
            query(
                where("data.id").`is`(node).and(IssueDataCache::url.name).`is`(imsProjectConfig.url)
            )
        ).apply(Update().inc(IssueDataCache::attempts.name, 1)).firstAndAwait()*/
        TODO()
    }

    override fun nodeId(node: IssueDataExtensive): String {
        return node.id
    }

    override suspend fun grabStep(
        since: OffsetDateTime?, cursor: String?, count: Int
    ): StepResponse<IssueDataExtensive>? {
        val query = IssueReadQuery(
            repoOwner = remote.owner, repoName = remote.repo, since = since, cursor = cursor, issueCount = count
        )
        val response = apolloClient.query(query).execute()
        return if (response.data?.repository?.issues?.nodes != null) {
            IssueStepResponse(response.data!!)
        } else {
            null
        }
    }
}
