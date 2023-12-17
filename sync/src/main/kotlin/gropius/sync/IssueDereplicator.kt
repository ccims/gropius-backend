package gropius.sync

import gropius.model.architecture.IMSProject
import gropius.model.issue.Issue
import gropius.model.issue.timeline.TimelineItem
import gropius.model.user.User
import gropius.repository.issue.IssueRepository
import org.springframework.data.neo4j.core.ReactiveNeo4jOperations

interface IssueDereplicatorIssueResult {
    val resultingIssue: Issue
    val fakeSyncedItems: List<TimelineItem>
}

interface IssueDereplicatorTimelineItemResult {
    val resultingTimelineItems: List<TimelineItem>
}

interface IssueDereplicatorRequest {
    val dummyUser: User
    val neoOperations: ReactiveNeo4jOperations
    val issueRepository: IssueRepository
}

interface IssueDereplicator {
    suspend fun validateIssue(
        imsProject: IMSProject, issue: Issue, request: IssueDereplicatorRequest
    ): IssueDereplicatorIssueResult

    suspend fun validateTimelineItem(
        issue: Issue, timelineItem: List<TimelineItem>, request: IssueDereplicatorRequest
    ): IssueDereplicatorTimelineItemResult
}