package gropius.sync

import gropius.model.architecture.IMSProject
import gropius.model.architecture.Trackable
import gropius.model.common.AuditedNode
import gropius.model.issue.Issue
import gropius.model.issue.timeline.Body
import gropius.model.issue.timeline.IssueComment
import gropius.model.issue.timeline.TimelineItem
import gropius.model.user.User
import org.neo4j.cypherdsl.core.Cypher
import org.slf4j.LoggerFactory

class HeuristicDereplicator(val issueThreshold: Double, val commentThreshold: Double) : IssueDereplicator {
    /**
     * Logger used to print notifications
     */
    private val logger = LoggerFactory.getLogger(HeuristicDereplicator::class.java)

    suspend fun matchIssue(
        newIssue: Issue,
        existingIssueMatchTitle: Boolean,
        existingIssueMatchAuthor: Boolean,
        existingIssueMatchBody: Boolean
    ): Boolean {
        val list = listOf(
            existingIssueMatchTitle, existingIssueMatchAuthor, existingIssueMatchBody
        )
        return (list.count { it } / list.count().toDouble()) >= issueThreshold
    }

    override suspend fun validateIssue(
        imsProject: IMSProject, issue: Issue, request: IssueDereplicatorRequest
    ): IssueDereplicatorIssueResult {
        val otherIssues = imsProject.trackable().value.issues()
        val issueNamedNode = Cypher.node(Issue::class.simpleName).named("issue")
        val trackableId = imsProject.trackable().value.rawId!!
        val trackableNamedNode =
            Cypher.node(Trackable::class.simpleName).withProperties(mapOf("id" to Cypher.anonParameter(trackableId)))
        val titleMatches = request.issueRepository.findAll(
            issueNamedNode.property("title").eq(Cypher.anonParameter(issue.title))
                .and(issueNamedNode.relationshipFrom(trackableNamedNode, Trackable.ISSUE).asCondition())
        ).toIterable().map { it.rawId!! }.toSet()
        val meNamedNode = Cypher.node(User::class.simpleName)
            .withProperties(mapOf("id" to Cypher.anonParameter(issue.createdBy().value.rawId)))
        val createdByMatches = request.issueRepository.findAll(
            issueNamedNode.relationshipTo(meNamedNode, AuditedNode.CREATED_BY).asCondition()
                .and(issueNamedNode.relationshipFrom(trackableNamedNode, Trackable.ISSUE).asCondition())
        ).toIterable().map { it.rawId!! }.toSet()
        val bodyNamedNode = Cypher.node(Body::class.simpleName)
            .withProperties(mapOf("body" to Cypher.anonParameter(issue.body().value.body)))
        val bodyMatches = request.issueRepository.findAll(
            issueNamedNode.relationshipTo(bodyNamedNode, Issue.BODY).asCondition()
                .and(issueNamedNode.relationshipFrom(trackableNamedNode, Trackable.ISSUE).asCondition())
        ).toIterable().map { it.rawId!! }.toSet()
        logger.trace("OISSUE ${issue.rawId} ${issue.title} $titleMatches $createdByMatches $bodyMatches")
        for (otherIssue in otherIssues) {
            if (matchIssue(
                    issue,
                    titleMatches.contains(otherIssue.rawId!!),
                    createdByMatches.contains(otherIssue.rawId!!),
                    bodyMatches.contains(otherIssue.rawId!!)
                )
            ) {
                logger.info("DEREPL MATCH $issueThreshold")
                return SimpleDereplicatorIssueResult(otherIssue, listOf())
            }
        }
        return SimpleDereplicatorIssueResult(issue, listOf())
    }

    suspend fun matchIssueComment(newComment: IssueComment, existingComment: IssueComment): Boolean {
        val list = listOf(
            (newComment.body == existingComment.body), (newComment.createdBy == existingComment.createdBy)
        )
        return (list.count { it } / list.count().toDouble()) >= issueThreshold
    }

    override suspend fun validateTimelineItem(
        issue: Issue, timelineItems: List<TimelineItem>, request: IssueDereplicatorRequest
    ): IssueDereplicatorTimelineItemResult {
        val comment = (timelineItems.firstOrNull() as? IssueComment)
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