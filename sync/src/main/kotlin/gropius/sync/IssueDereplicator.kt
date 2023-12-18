package gropius.sync

import gropius.model.architecture.IMSProject
import gropius.model.issue.Issue
import gropius.model.issue.timeline.TimelineItem
import gropius.model.user.User
import gropius.repository.issue.IssueRepository
import org.springframework.data.neo4j.core.ReactiveNeo4jOperations

/**
 * Output of a dereplicator for an issue
 */
interface IssueDereplicatorIssueResult {
    /**
     * The resulting issue
     */
    val resultingIssue: Issue

    /**
     * TimelineItems that should be markes as already snyced
     */
    val fakeSyncedItems: List<TimelineItem>
}

/**
 * Output of a dereplicator for a timeline item
 */
interface IssueDereplicatorTimelineItemResult {
    /**
     * The resulting timeline items
     */
    val resultingTimelineItems: List<TimelineItem>
}

/**
 * Input for a dereplicator
 */
interface IssueDereplicatorRequest {
    /**
     * The project the issue is in
     */
    val dummyUser: User

    /**
     * The project the issue is in
     */
    val neoOperations: ReactiveNeo4jOperations

    /**
     * The project the issue is in
     */
    val issueRepository: IssueRepository
}

/**
 * Dereplicator for issues
 */
interface IssueDereplicator {

    /**
     * Validates an issue
     * @param imsProject The project the issue is in
     * @param issue The issue to validate
     * @param request The request
     * @return The result
     */
    suspend fun validateIssue(
        imsProject: IMSProject, issue: Issue, request: IssueDereplicatorRequest
    ): IssueDereplicatorIssueResult

    /**
     * Validates a timeline item
     * @param issue The issue the timeline item is in
     * @param timelineItem The timeline item to validate
     * @param request The request
     * @return The result
     */
    suspend fun validateTimelineItem(
        issue: Issue, timelineItem: List<TimelineItem>, request: IssueDereplicatorRequest
    ): IssueDereplicatorTimelineItemResult
}