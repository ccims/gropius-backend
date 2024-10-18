package gropius.sync

import gropius.model.issue.Issue
import gropius.model.issue.timeline.*
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
                val label = item.addedLabel().value
                if (label != null) {
                    issue.labels().add(label)
                }
            }
            if (item is RemovedLabelEvent) {
                issue.labels().remove(item.removedLabel().value)
            }
        }
    }

    /**
     * Clean all labels on this issue into a consistent state
     * @param issue issue to work on
     */
    private suspend fun cleanAssignments(issue: Issue) {
        issue.assignments().clear()
        for (item in issue.timelineItems().sortedBy { it.createdAt }) {
            if (item is Assignment) {
                issue.assignments() += item
            }
        }
        for (item in issue.timelineItems().sortedBy { it.createdAt }) {
            if (item is AssignmentTypeChangedEvent) {
                TODO()
            }
        }
        for (item in issue.timelineItems().sortedBy { it.createdAt }) {
            if (item is RemovedAssignmentEvent) {
                issue.assignments() -= item.removedAssignment().value
            }
        }
    }

    /**
     * Clean the title on this issue into a consistent state
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
     * Clean the templated fields on this issue into a consistent state
     * @param issue issue to work on
     */
    private suspend fun cleanTemplatedFields(issue: Issue) {
        issue.templatedFields.clear()
        for (item in issue.timelineItems().sortedBy { it.createdAt }) {
            if (item is TemplatedFieldChangedEvent) {
                issue.templatedFields[item.fieldName] = item.newValue
            }
        }
    }

    /**
     * Clean the open state on this issue into a consistent state
     * @param issue issue to work on
     */
    private suspend fun cleanState(issue: Issue) {
        for (item in issue.timelineItems().sortedBy { it.createdAt }) {
            if (item is StateChangedEvent) {
                issue.state().value = item.newState().value
            }
        }
    }

    /**
     * Resort comments into comments list
     * @param issue issue to work on
     */
    private suspend fun cleanComments(issue: Issue) {
        for (item in issue.timelineItems()) {
            if (item is IssueComment) {
                issue.issueComments() += item
            }
        }
    }

    /**
     * Execute the cleaning process
     *
     * @param id Issue ID to clean
     */
    @Transactional
    @Suppress("UNUSED_VALUE")
    suspend fun cleanIssue(id: String) {
        var issue = neoOperations.findById<Issue>(id)!!
        cleanLabels(issue)
        cleanAssignments(issue)
        cleanState(issue)
        cleanTitle(issue)
        cleanComments(issue)
        cleanTemplatedFields(issue)
        issue = neoOperations.save(issue).awaitSingle()
    }
}