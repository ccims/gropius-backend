package gropius.sync.github

import gropius.model.architecture.IMSProject
import gropius.model.issue.Issue
import gropius.model.issue.Label
import gropius.model.issue.timeline.Assignment
import gropius.model.issue.timeline.IssueComment
import gropius.model.issue.timeline.TimelineItem
import gropius.model.template.IMSTemplate
import gropius.model.template.IssueState
import gropius.model.user.GropiusUser
import gropius.model.user.IMSUser
import gropius.model.user.User
import gropius.sync.*
import gropius.sync.github.config.IMSConfigManager
import gropius.sync.github.config.IMSProjectConfig
import gropius.sync.github.generated.*
import gropius.sync.github.generated.MutateAddLabelMutation.Data.AddLabelsToLabelable.Labelable.Companion.asIssue
import gropius.sync.github.generated.MutateAssignUserMutation.Data.AddAssigneesToAssignable.Assignable.Companion.asIssue
import gropius.sync.github.generated.MutateCreateCommentMutation.Data.AddComment.CommentEdge.Node.Companion.asIssueTimelineItems
import gropius.sync.github.generated.MutateRemoveLabelMutation.Data.RemoveLabelsFromLabelable.Labelable.Companion.asIssue
import gropius.sync.github.generated.MutateUnassignUserMutation.Data.RemoveAssigneesFromAssignable.Assignable.Companion.asIssue
import gropius.sync.github.generated.fragment.TimelineItemData.Companion.asNode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

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

    override suspend fun labelStateMap(imsProject: IMSProject): Map<String, String> {
        val imsProjectConfig = IMSProjectConfig(helper, imsProject)
        return imsProjectConfig.labelStateMapper
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

    override suspend fun isOutgoingTemplatedFieldsEnabled(imsProject: IMSProject): Boolean {
        return false
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

        githubDataService.issueTemplate(imsProject)
        githubDataService.issueType(imsProject)
        githubDataService.issueState(imsProject, true)
        githubDataService.issueState(imsProject, false)

        val walkers = mutableListOf<ResourceWalker>()
        walkers += IssueWalker(
            imsProject, GitHubResourceWalkerConfig(
                CursorResourceWalkerConfig<GithubGithubResourceWalkerBudgetUsageType, GithubGithubResourceWalkerEstimatedBudgetUsageType>(
                    10.0,
                    1.0,
                    GithubGithubResourceWalkerEstimatedBudgetUsageType(),
                    GithubGithubResourceWalkerBudgetUsageType()
                ), imsProjectConfig.repo.owner, imsProjectConfig.repo.repo, 100
            ), budget, githubDataService, issuePileService, cursorResourceWalkerDataService
        )

        walkers += issuePileService.findByImsProjectAndNeedsTimelineRequest(
            imsProject.rawId!!, true
        ).map {
            TimelineWalker(
                imsProject, it.id!!, GitHubResourceWalkerConfig(
                    CursorResourceWalkerConfig<GithubGithubResourceWalkerBudgetUsageType, GithubGithubResourceWalkerEstimatedBudgetUsageType>(
                        1.0,
                        0.001,
                        GithubGithubResourceWalkerEstimatedBudgetUsageType(),
                        GithubGithubResourceWalkerBudgetUsageType()
                    ), imsProjectConfig.repo.owner, imsProjectConfig.repo.repo, 100
                ), budget, githubDataService, issuePileService, cursorResourceWalkerDataService
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
                            0.001,
                            GithubGithubResourceWalkerEstimatedBudgetUsageType(),
                            GithubGithubResourceWalkerBudgetUsageType()
                        ), imsProjectConfig.repo.owner, imsProjectConfig.repo.repo, 100
                    ), budget, githubDataService, issuePileService, cursorResourceWalkerDataService
                )
            }
        }
        return walkers
    }

    override suspend fun findUnsyncedIssues(imsProject: IMSProject): List<IncomingIssue> {
        return issuePileService.findByImsProjectAndHasUnsyncedData(imsProject.rawId!!, true)
    }

    override suspend fun syncComment(
        imsProject: IMSProject, issueId: String, issueComment: IssueComment, users: List<User>
    ): TimelineItemConversionInformation? {
        val body = issueComment.body
        if (body.isNullOrEmpty()) return null;
        val response = githubDataService.mutation(
            imsProject, users, MutateCreateCommentMutation(issueId, body), gropiusUserList(users)
        ).second
        val item = response.data?.addComment?.commentEdge?.node?.asIssueTimelineItems()
        if (item != null) {
            return TODOTimelineItemConversionInformation(imsProject.rawId!!, item.id)
        }
        logger.error("${response.data} ${response.errors}")
        //TODO("ERROR HANDLING")
        return null
    }

    override suspend fun syncSingleAssigned(
        imsProject: IMSProject, issueId: String, assignment: Assignment, users: List<User>
    ): TimelineItemConversionInformation? {
        val assignedUser = assignment.user().value
        val imsUsers =
            if (assignedUser as? IMSUser != null) listOf(assignedUser) else if (assignedUser as? GropiusUser != null) assignedUser.imsUsers()
                .filter { it.ims().value == imsProject.ims().value } else emptyList()
        val ids = imsUsers.map {
            it.templatedFields["github_id"]!!
        }
        if (ids.isEmpty()) {
            return null
        }
        val response = githubDataService.mutation(
            imsProject, users, MutateAssignUserMutation(issueId, ids.first()), gropiusUserList(users)
        ).second
        val item =
            response.data?.addAssigneesToAssignable?.assignable?.asIssue()?.timelineItems?.nodes?.lastOrNull()?.asNode()
        if (item != null) {
            return TODOTimelineItemConversionInformation(imsProject.rawId!!, item.id)
        }
        logger.error("${response.data} ${response.errors}")
        //TODO("ERROR HANDLING")
        return null
    }

    override suspend fun syncSingleUnassigned(
        imsProject: IMSProject, issueId: String, assignment: Assignment, users: List<User>
    ): TimelineItemConversionInformation? {
        val assignedUser = assignment.user().value
        val imsUsers =
            if (assignedUser as? IMSUser != null) listOf(assignedUser) else if (assignedUser as? GropiusUser != null) assignedUser.imsUsers()
                .filter { it.ims().value == imsProject.ims().value } else emptyList()
        val ids = imsUsers.map {
            it.templatedFields["github_id"]!!
        }
        if (ids.isEmpty()) {
            return null
        }
        val response = githubDataService.mutation(
            imsProject, users, MutateUnassignUserMutation(issueId, ids.first()), gropiusUserList(users)
        ).second
        val item =
            response.data?.removeAssigneesFromAssignable?.assignable?.asIssue()?.timelineItems?.nodes?.lastOrNull()
                ?.asNode()
        if (item != null) {
            return TODOTimelineItemConversionInformation(imsProject.rawId!!, item.id)
        }
        logger.error("${response.data} ${response.errors}")
        //TODO("ERROR HANDLING")
        return null
    }

    override suspend fun syncFallbackComment(
        imsProject: IMSProject, issueId: String, comment: String, original: TimelineItem?, users: List<User>
    ): TimelineItemConversionInformation? {
        val response = githubDataService.mutation(
            imsProject, users, MutateCreateCommentMutation(issueId, comment), gropiusUserList(users)
        ).second
        val item = response.data?.addComment?.commentEdge?.node?.asIssueTimelineItems()
        if (item != null) {
            return TODOTimelineItemConversionInformation(imsProject.rawId!!, item.id)
        }
        logger.error("${response.data} ${response.errors}")
        //TODO("ERROR HANDLING")
        return null
    }

    override suspend fun syncAddedLabel(
        imsProject: IMSProject, issueId: String, label: Label, users: List<User>
    ): TimelineItemConversionInformation? {
        val labelInfo =
            githubDataService.labelInfoRepository.findByImsProjectAndNeo4jId(imsProject.rawId!!, label.rawId!!)
        if (labelInfo == null) {
            logger.error("Create label on remote")
            //TODO("Create label on remote")
            return null
        }
        val response = githubDataService.mutation(
            imsProject, users, MutateAddLabelMutation(issueId, labelInfo.githubId), gropiusUserList(users)
        ).second
        val item = response.data?.addLabelsToLabelable?.labelable?.asIssue()?.timelineItems?.nodes?.lastOrNull()
        if (item != null) {
            return TODOTimelineItemConversionInformation(imsProject.rawId!!, item.asNode()!!.id)
        }
        logger.error("${response.data} ${response.errors}")
        //TODO("ERROR HANDLING")
        return null
    }

    override suspend fun syncTitleChange(
        imsProject: IMSProject, issueId: String, newTitle: String, users: List<User>
    ): TimelineItemConversionInformation? {
        val response = githubDataService.mutation(
            imsProject, users, MutateChangeTitleMutation(issueId, newTitle), gropiusUserList(users)
        ).second
        val item = response.data?.updateIssue?.issue?.timelineItems?.nodes?.lastOrNull()
        if (item != null) {
            return TODOTimelineItemConversionInformation(imsProject.rawId!!, item.asNode()!!.id)
        }
        logger.error("${response.data} ${response.errors}")
        //TODO("ERROR HANDLING")
        return null
    }

    override suspend fun syncStateChange(
        imsProject: IMSProject, issueId: String, newState: IssueState, users: List<User>
    ): TimelineItemConversionInformation? {
        if (newState.isOpen) {
            val response = githubDataService.mutation(
                imsProject, users, MutateReopenIssueMutation(issueId), gropiusUserList(users)
            ).second
            val item = response.data?.reopenIssue?.issue?.timelineItems?.nodes?.lastOrNull()
            if (item != null) {
                return TODOTimelineItemConversionInformation(imsProject.rawId!!, item.asNode()!!.id)
            }
            logger.error("${response.data} ${response.errors}")
            //TODO("ERROR HANDLING")
            return null
        } else {
            val response = githubDataService.mutation(
                imsProject, users, MutateCloseIssueMutation(issueId), gropiusUserList(users)
            ).second
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
        imsProject: IMSProject, issueId: String, label: Label, users: List<User>
    ): TimelineItemConversionInformation? {
        val labelInfo =
            githubDataService.labelInfoRepository.findByImsProjectAndNeo4jId(imsProject.rawId!!, label.rawId!!)!!
        val response = githubDataService.mutation(
            imsProject, users, MutateRemoveLabelMutation(issueId, labelInfo.githubId), gropiusUserList(users)
        ).second
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
        val repoInfoResponse = githubDataService.query(
            imsProject,
            listOf(issue.createdBy().value, issue.lastModifiedBy().value) + issue.timelineItems()
                .map { it.createdBy().value },
            RepositoryIDQuery(imsProjectConfig.repo.owner, imsProjectConfig.repo.repo)
        ).second//TODO
        val repoId = repoInfoResponse.data?.repository?.id!!
        val response = githubDataService.mutation(
            imsProject,
            listOf(issue.createdBy().value, issue.lastModifiedBy().value) + issue.timelineItems()
                .map { it.createdBy().value },
            MutateCreateIssueMutation(repoId, issue.title, issue.bodyBody),
            gropiusUserList(listOf(issue.createdBy().value, issue.lastModifiedBy().value) + issue.timelineItems()
                .map { it.createdBy().value })
        ).second
        val item = response.data?.createIssue?.issue
        if (item != null) {
            return IssueConversionInformation(imsProject.rawId!!, item.id, issue.rawId!!)
        }
        logger.error("${response.data} ${response.errors}")
        //TODO("ERROR HANDLING")
        return null
    }
}
