package gropius.sync

import gropius.model.architecture.IMSProject
import gropius.model.issue.Issue
import gropius.model.issue.timeline.TimelineItem

interface IssueDereplicatorIssueResult {
    val resultingIssue: Issue
    val fakeSyncedItems: List<TimelineItem>
}

interface IssueDereplicatorTimelineItemResult {
    val resultingTimelineItems: List<TimelineItem>
}

interface IssueDereplicator {
    suspend fun validateIssue(imsProject: IMSProject, issue: Issue): IssueDereplicatorIssueResult
    suspend fun validateTimelineItem(
        issue: Issue, timelineItem: List<TimelineItem>
    ): IssueDereplicatorTimelineItemResult
}