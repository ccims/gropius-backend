package gropius.sync.github

import com.apollographql.apollo3.ApolloClient
import gropius.model.architecture.IMSProject
import gropius.model.issue.Issue
import gropius.model.issue.Label
import gropius.model.issue.timeline.IssueComment
import gropius.model.template.IMSTemplate
import gropius.model.template.IssueState
import gropius.sync.*
import gropius.sync.github.config.IMSConfigManager
import gropius.sync.github.config.IMSProjectConfig
import gropius.sync.github.generated.*
import gropius.sync.github.generated.MutateAddLabelMutation.Data.AddLabelsToLabelable.Labelable.Companion.asIssue
import gropius.sync.github.generated.MutateCreateCommentMutation.Data.AddComment.CommentEdge.Node.Companion.asIssueTimelineItems
import gropius.sync.github.generated.MutateRemoveLabelMutation.Data.RemoveLabelsFromLabelable.Labelable.Companion.asIssue
import gropius.sync.github.generated.fragment.TimelineItemData.Companion.asNode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.URI

/**
 * This class is responsible for syncing data from and to GitHub
 * @param githubDataService The service used to access the database
 * @param issuePileService The service used to access the issue pile
 * @param helper The helper used to convert data to and from JSON
 * @param cursorResourceWalkerDataService The service used to access the cursor resource walker data
 * @param imsConfigManager The manager used to access the IMS config
 * @param collectedSyncInfo The collected sync info
 * @param loadBalancedDataFetcher The load balanced data fetcher
 */
@Component
final class GithubSync(
    val githubDataService: GithubDataService,
    val issuePileService: IssuePileService,
    val helper: JsonHelper,
    val cursorResourceWalkerDataService: CursorResourceWalkerDataService,
    val imsConfigManager: IMSConfigManager,
    collectedSyncInfo: CollectedSyncInfo,
    val loadBalancedDataFetcher: LoadBalancedDataFetcher = LoadBalancedDataFetcher()
) : AbstractSync(collectedSyncInfo), LoadBalancedDataFetcherImplementation, DataFetcher by loadBalancedDataFetcher {

    init {
        loadBalancedDataFetcher.start(this)
    }

    private val apolloClient = ApolloClient.Builder().serverUrl(URI("https://api.github.com/graphql").toString())
        .addHttpHeader("Authorization", "bearer " + System.getenv("GITHUB_DUMMY_PAT")).build()

    /**
     * Logger used to print notifications
     */
    private val logger = LoggerFactory.getLogger(GithubSync::class.java)

    override suspend fun createBudget(): GeneralResourceWalkerBudget {
        return GithubResourceWalkerBudget()
    }

    override fun syncDataService(): SyncDataService {
        return githubDataService
    }

    override suspend fun findTemplates(): Set<IMSTemplate> {
        return imsConfigManager.findTemplates()
    }

    override suspend fun isOutgoingEnabled(imsProject: IMSProject): Boolean {
        val imsProjectConfig = IMSProjectConfig(helper, imsProject)
        return imsProjectConfig.enableOutgoing
    }

    override suspend fun isOutgoingLabelsEnabled(imsProject: IMSProject): Boolean {
        val imsProjectConfig = IMSProjectConfig(helper, imsProject)
        return imsProjectConfig.enableOutgoingLabels
    }

    override suspend fun isOutgoingCommentsEnabled(imsProject: IMSProject): Boolean {
        val imsProjectConfig = IMSProjectConfig(helper, imsProject)
        return imsProjectConfig.enableOutgoingComments
    }

    override suspend fun isOutgoingTitleChangedEnabled(imsProject: IMSProject): Boolean {
        val imsProjectConfig = IMSProjectConfig(helper, imsProject)
        return imsProjectConfig.enableOutgoingTitleChanges
    }

    override suspend fun isOutgoingAssignmentsEnabled(imsProject: IMSProject): Boolean {
        val imsProjectConfig = IMSProjectConfig(helper, imsProject)
        return imsProjectConfig.enableOutgoingAssignments
    }

    override suspend fun isOutgoingStatesEnabled(imsProject: IMSProject): Boolean {
        val imsProjectConfig = IMSProjectConfig(helper, imsProject)
        return imsProjectConfig.enableOutgoingState
    }

    override suspend fun balancedFetchData(
        imsProject: IMSProject, generalBudget: GeneralResourceWalkerBudget
    ): List<ResourceWalker> {
        val imsProjectConfig = IMSProjectConfig(helper, imsProject)
        val budget = generalBudget as GithubResourceWalkerBudget

        val walkers = mutableListOf<ResourceWalker>()
        walkers += IssueWalker(
            imsProject, GitHubResourceWalkerConfig(
                CursorResourceWalkerConfig<GithubGithubResourceWalkerBudgetUsageType, GithubGithubResourceWalkerEstimatedBudgetUsageType>(
                    10.0,
                    1.0,
                    GithubGithubResourceWalkerEstimatedBudgetUsageType(),
                    GithubGithubResourceWalkerBudgetUsageType()
                ), imsProjectConfig.repo.owner, imsProjectConfig.repo.repo, 100
            ), budget, apolloClient, issuePileService, cursorResourceWalkerDataService
        )

        walkers += issuePileService.findByImsProjectAndNeedsTimelineRequest(
            imsProject.rawId!!, true
        ).map {
            TimelineWalker(
                imsProject, it.id!!, GitHubResourceWalkerConfig(
                    CursorResourceWalkerConfig<GithubGithubResourceWalkerBudgetUsageType, GithubGithubResourceWalkerEstimatedBudgetUsageType>(
                        1.0,
                        0.1,
                        GithubGithubResourceWalkerEstimatedBudgetUsageType(),
                        GithubGithubResourceWalkerBudgetUsageType()
                    ), imsProjectConfig.repo.owner, imsProjectConfig.repo.repo, 100
                ), budget, apolloClient, issuePileService, cursorResourceWalkerDataService
            )
        }
        for (dirtyIssue in issuePileService.findByImsProjectAndNeedsCommentRequest(
            imsProject.rawId!!, true
        )) {
            for (comment in dirtyIssue.timelineItems.mapNotNull { it as? IssueCommentTimelineItem }) {
                walkers += CommentWalker(
                    imsProject, dirtyIssue.id!!, comment.githubId, GitHubResourceWalkerConfig(
                        CursorResourceWalkerConfig<GithubGithubResourceWalkerBudgetUsageType, GithubGithubResourceWalkerEstimatedBudgetUsageType>(
                            1.0,
                            0.1,
                            GithubGithubResourceWalkerEstimatedBudgetUsageType(),
                            GithubGithubResourceWalkerBudgetUsageType()
                        ), imsProjectConfig.repo.owner, imsProjectConfig.repo.repo, 100
                    ), budget, apolloClient, issuePileService, cursorResourceWalkerDataService
                )
            }
        }
        return walkers
    }

    override suspend fun findUnsyncedIssues(imsProject: IMSProject): List<IncomingIssue> {
        return issuePileService.findByImsProjectAndHasUnsyncedData(imsProject.rawId!!, true)
    }

    override suspend fun syncComment(
        imsProject: IMSProject, issueId: String, issueComment: IssueComment
    ): TimelineItemConversionInformation? {
        val body = issueComment.body
        if (body.isNullOrEmpty()) return null;
        val response = apolloClient.mutation(MutateCreateCommentMutation(issueId, body)).execute()
        val item = response.data?.addComment?.commentEdge?.node?.asIssueTimelineItems()
        if (item != null) {
            return TODOTimelineItemConversionInformation(imsProject.rawId!!, item.id)
        }
        logger.error("${response.data} ${response.errors}")
        //TODO("ERROR HANDLING")
        return null
    }

    override suspend fun syncAddedLabel(
        imsProject: IMSProject, issueId: String, label: Label
    ): TimelineItemConversionInformation? {
        val labelInfo =
            githubDataService.labelInfoRepository.findByImsProjectAndNeo4jId(imsProject.rawId!!, label.rawId!!)
        if (labelInfo == null) {
            logger.error("Create label on remote")
            //TODO("Create label on remote")
            return null
        }
        val response = apolloClient.mutation(MutateAddLabelMutation(issueId, labelInfo.githubId)).execute()
        val item = response.data?.addLabelsToLabelable?.labelable?.asIssue()?.timelineItems?.nodes?.lastOrNull()
        if (item != null) {
            return TODOTimelineItemConversionInformation(imsProject.rawId!!, item.asNode()!!.id)
        }
        logger.error("${response.data} ${response.errors}")
        //TODO("ERROR HANDLING")
        return null
    }

    override suspend fun syncTitleChange(
        imsProject: IMSProject, issueId: String, newTitle: String
    ): TimelineItemConversionInformation? {
        val response = apolloClient.mutation(MutateChangeTitleMutation(issueId, newTitle)).execute()
        val item = response.data?.updateIssue?.issue?.timelineItems?.nodes?.lastOrNull()
        if (item != null) {
            return TODOTimelineItemConversionInformation(imsProject.rawId!!, item.asNode()!!.id)
        }
        logger.error("${response.data} ${response.errors}")
        //TODO("ERROR HANDLING")
        return null
    }

    override suspend fun syncStateChange(
        imsProject: IMSProject, issueId: String, newState: IssueState
    ): TimelineItemConversionInformation? {
        if (newState.isOpen) {
            val response = apolloClient.mutation(MutateReopenIssueMutation(issueId)).execute()
            val item = response.data?.reopenIssue?.issue?.timelineItems?.nodes?.lastOrNull()
            if (item != null) {
                return TODOTimelineItemConversionInformation(imsProject.rawId!!, item.asNode()!!.id)
            }
            logger.error("${response.data} ${response.errors}")
            //TODO("ERROR HANDLING")
            return null
        } else {
            val response = apolloClient.mutation(MutateCloseIssueMutation(issueId)).execute()
            val item = response.data?.closeIssue?.issue?.timelineItems?.nodes?.lastOrNull()
            if (item != null) {
                return TODOTimelineItemConversionInformation(imsProject.rawId!!, item.asNode()!!.id)
            }
            logger.error("${response.data} ${response.errors}")
            //TODO("ERROR HANDLING")
            return null
        }
    }

    override suspend fun syncRemovedLabel(
        imsProject: IMSProject, issueId: String, label: Label
    ): TimelineItemConversionInformation? {
        val labelInfo =
            githubDataService.labelInfoRepository.findByImsProjectAndNeo4jId(imsProject.rawId!!, label.rawId!!)!!
        val response = apolloClient.mutation(MutateRemoveLabelMutation(issueId, labelInfo.githubId)).execute()
        val item = response.data?.removeLabelsFromLabelable?.labelable?.asIssue()?.timelineItems?.nodes?.lastOrNull()
        if (item != null) {
            return TODOTimelineItemConversionInformation(imsProject.rawId!!, item.asNode()!!.id)
        }
        logger.error("${response.data} ${response.errors}")
        //TODO("ERROR HANDLING")
        return null
    }

    override suspend fun createOutgoingIssue(imsProject: IMSProject, issue: Issue): IssueConversionInformation? {
        val imsProjectConfig = IMSProjectConfig(helper, imsProject)
        val repoInfoResponse =
            apolloClient.query(RepositoryIDQuery(imsProjectConfig.repo.owner, imsProjectConfig.repo.repo))
                .execute()//TODO
        val repoId = repoInfoResponse.data?.repository?.id!!
        val response =
            apolloClient.mutation(MutateCreateIssueMutation(repoId, issue.title, issue.body().value.body)).execute()
        val item = response.data?.createIssue?.issue
        if (item != null) {
            return IssueConversionInformation(imsProject.rawId!!, item.id, issue.rawId!!)
        }
        logger.error("${response.data} ${response.errors}")
        //TODO("ERROR HANDLING")
        return null
    }
}