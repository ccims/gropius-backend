package gropius.sync

import gropius.model.architecture.IMSProject
import gropius.model.issue.Issue
import gropius.model.issue.timeline.TimelineItem

open class SimpleDereplicatorIssueResult(
    val issue: Issue, override val fakeSyncedItems: List<TimelineItem>
) : IssueDereplicatorIssueResult {
    override val resultingIssue
        get(): Issue {
            return issue
        }
}

class SimpleDereplicatorTimelineItemResult(val timelineItems: List<TimelineItem>) :
    IssueDereplicatorTimelineItemResult {
    override val resultingTimelineItems
        get(): List<TimelineItem> {
            return timelineItems
        }
}

class NullDereplicator : IssueDereplicator {
    override suspend fun validateIssue(imsProject: IMSProject, issue: Issue): IssueDereplicatorIssueResult {
        return SimpleDereplicatorIssueResult(issue, listOf())
    }

    override suspend fun validateTimelineItem(
        issue: Issue, timelineItems: List<TimelineItem>
    ): IssueDereplicatorTimelineItemResult {
        return SimpleDereplicatorTimelineItemResult(timelineItems)
    }
}