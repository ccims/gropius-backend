package gropius.sync

import gropius.model.issue.Issue
import gropius.model.issue.timeline.AddedLabelEvent
import gropius.model.issue.timeline.RemovedLabelEvent
import gropius.model.issue.timeline.TitleChangedEvent
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.neo4j.core.ReactiveNeo4jOperations
import org.springframework.data.neo4j.core.findById
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Reconsturcts the fields of an issue from the timeline
 *
 * Sync services using this can only zip the timelines together and clean it into a valid state to merge two issues
 */
@Component
class IssueCleaner(
    @Qualifier("graphglueNeo4jOperations")
    val neoOperations: ReactiveNeo4jOperations,
) {

    /**
     * Clean all labels on this issue into a consistent state
     * @param issue issue to work on
     */
    private suspend fun cleanLabels(issue: Issue) {
        issue.labels().clear()
        for (item in issue.timelineItems().sortedBy { it.createdAt }) {
            if (item is AddedLabelEvent) {
                issue.labels().add(item.addedLabel().value)
            }
            if (item is RemovedLabelEvent) {
                issue.labels().remove(item.removedLabel().value)
            }
        }
    }

    /**
     * Clean the ttile on this issue into a consistent state
     * @param issue issue to work on
     */
    private suspend fun cleanTitle(issue: Issue) {
        for (item in issue.timelineItems().sortedBy { it.createdAt }) {
            if (item is TitleChangedEvent) {
                issue.title = item.newTitle
            }
        }
    }

    /**
     * Execute the cleaning process
     *
     * @param id Issue ID to clean
     */
    @Transactional
    suspend fun cleanIssue(id: String) {
        var issue = neoOperations.findById<Issue>(id)!!
        cleanLabels(issue)
        cleanTitle(issue)
        issue = neoOperations.save(issue).awaitSingle()
    }
}