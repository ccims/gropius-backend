package gropius.sync

import gropius.model.architecture.IMSProject
import gropius.model.issue.Issue
import gropius.model.issue.timeline.IssueComment
import gropius.model.issue.timeline.TimelineItem

class HeuristicDereplicator : IssueDereplicator {
    suspend fun matchIssue(newIssue: Issue, existingIssue: Issue): Boolean {
        return (newIssue.title == existingIssue.title) && (newIssue.createdBy == existingIssue.createdBy) && (newIssue.body().value.body == existingIssue.body().value.body);
    }

    override suspend fun validateIssue(imsProject: IMSProject, issue: Issue): IssueDereplicatorIssueResult {
        for (otherIssue in imsProject.trackable().value.issues()) {
            if (matchIssue(issue, otherIssue)) {
                return SimpleDereplicatorIssueResult(otherIssue, listOf())
            }
        }
        return SimpleDereplicatorIssueResult(issue, listOf())
    }

    suspend fun matchIssueComment(newComment: IssueComment, existingComment: IssueComment): Boolean {
        return (newComment.body == existingComment.body) && (newComment.createdBy == existingComment.createdBy);
    }

    override suspend fun validateTimelineItem(
        issue: Issue, timelineItems: List<TimelineItem>
    ): IssueDereplicatorTimelineItemResult {
        val comment = (timelineItems.first() as? IssueComment)
        if (comment != null) {
            val match =
                issue.timelineItems().mapNotNull { it as? IssueComment }.filter { matchIssueComment(comment, it) }
                    .singleOrNull()
            if (match != null) {
                return SimpleDereplicatorTimelineItemResult(listOf(match))
            }
        }
        return SimpleDereplicatorTimelineItemResult(timelineItems)
    }
}