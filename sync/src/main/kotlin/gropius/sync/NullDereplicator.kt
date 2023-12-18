package gropius.sync

import gropius.model.architecture.IMSProject
import gropius.model.issue.Issue
import gropius.model.issue.timeline.TimelineItem

/**
 * A simple implementation of [IssueDereplicatorIssueResult] that just returns the issue and the fake synced items.
 * @param issue The issue to return.
 * @param fakeSyncedItems The fake synced items to return.
 */
open class SimpleDereplicatorIssueResult(
    val issue: Issue, override val fakeSyncedItems: List<TimelineItem>
) : IssueDereplicatorIssueResult {
    override val resultingIssue
        get(): Issue {
            return issue
        }
}

/**
 * A simple implementation of [IssueDereplicatorTimelineItemResult] that just returns the timeline items.
 * @param timelineItems The timeline items to return.
 */
class SimpleDereplicatorTimelineItemResult(val timelineItems: List<TimelineItem>) :
    IssueDereplicatorTimelineItemResult {
    override val resultingTimelineItems
        get(): List<TimelineItem> {
            return timelineItems
        }
}

/**
 * A simple implementation of [IssueDereplicator] that just returns the issue and the timeline items.
 */
class NullDereplicator : IssueDereplicator {
    override suspend fun validateIssue(
        imsProject: IMSProject, issue: Issue, request: IssueDereplicatorRequest
    ): IssueDereplicatorIssueResult {
        return SimpleDereplicatorIssueResult(issue, listOf())
    }

    override suspend fun validateTimelineItem(
        issue: Issue, timelineItems: List<TimelineItem>, request: IssueDereplicatorRequest
    ): IssueDereplicatorTimelineItemResult {
        return SimpleDereplicatorTimelineItemResult(timelineItems)
    }
}