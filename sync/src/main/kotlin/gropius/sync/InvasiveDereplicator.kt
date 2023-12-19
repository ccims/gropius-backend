package gropius.sync

import gropius.model.architecture.IMSProject
import gropius.model.issue.Issue
import gropius.model.issue.timeline.TimelineItem
import gropius.model.issue.timeline.TitleChangedEvent
import java.time.OffsetDateTime
import java.util.*

/**
 * Dereplicator that changes the title of issues to include a UUID
 */
class InvasiveDereplicator : IssueDereplicator {
    private val titleRegex =
        Regex("\\[([0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12})\\]$");

    suspend fun getId(issue: Issue): String? {
        var title = issue.title
        for (timelineItem in issue.timelineItems().mapNotNull { it as? TitleChangedEvent }.sortedBy { it.createdAt }) {
            title = timelineItem.newTitle
        }
        return titleRegex.find(title)?.groupValues?.get(1)
    }

    override suspend fun validateIssue(
        imsProject: IMSProject,
        issue: Issue,
        request: IssueDereplicatorRequest
    ): IssueDereplicatorIssueResult {
        for (otherIssue in imsProject.trackable().value.issues()) {
            val otherId = getId(otherIssue)
            if ((otherId != null) && (otherId == getId(issue))) {
                return SimpleDereplicatorIssueResult(otherIssue, listOf())
            }
        }
        val id = getId(issue)
        if (id == null) {
            var title = issue.title
            for (timelineItem in issue.timelineItems().mapNotNull { it as? TitleChangedEvent }
                .sortedBy { it.createdAt }) {
                title = timelineItem.newTitle
            }
            val titleChange = TitleChangedEvent(
                OffsetDateTime.now(), OffsetDateTime.now(), title, title + " [${UUID.randomUUID().toString()}]"
            )
            titleChange.createdBy().value = request.dummyUser
            titleChange.lastModifiedBy().value = request.dummyUser
            issue.timelineItems() += titleChange
            return SimpleDereplicatorIssueResult(issue, listOf(titleChange))
        }
        return SimpleDereplicatorIssueResult(issue, listOf())
    }

    override suspend fun validateTimelineItem(
        issue: Issue, timelineItems: List<TimelineItem>, request: IssueDereplicatorRequest
    ): IssueDereplicatorTimelineItemResult {
        return SimpleDereplicatorTimelineItemResult(timelineItems)
    }
}