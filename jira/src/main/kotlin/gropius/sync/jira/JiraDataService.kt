package gropius.sync.jira

import gropius.model.architecture.IMSProject
import gropius.model.issue.Label
import gropius.model.template.*
import gropius.model.user.User
import gropius.sync.SyncDataService
import gropius.sync.user.UserMapper
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.neo4j.core.ReactiveNeo4jOperations
import org.springframework.stereotype.Component
import java.time.OffsetDateTime

/**
 * Context for Jira Operations
 * @param userMapper UserMapper to map Jira Users to Gropius Users
 * @param neoOperations Neo4jOperations to access the database
 */
@Component
class JiraDataService(
    val userMapper: UserMapper,
    @Qualifier("graphglueNeo4jOperations")
    val neoOperations: ReactiveNeo4jOperations
) : SyncDataService {

    /**
     * Get the default issue template
     * @return the default issue template
     */
    suspend fun issueTemplate(): IssueTemplate {
        val newTemplate = IssueTemplate("noissue", "", mutableMapOf(), false)
        newTemplate.issueStates() += IssueState("open", "", true)
        newTemplate.issueStates() += IssueState("closed", "", false)
        for (t in newTemplate.issueStates()) t.partOf() += newTemplate
        return neoOperations.findAll(IssueTemplate::class.java).awaitFirstOrNull() ?: neoOperations.save(
            newTemplate
        ).awaitSingle()
    }

    /**
     * Get the default issue type
     * @return the default issue type
     */
    suspend fun issueType(): IssueType {
        val newIssueType = IssueType("type", "", "")
        newIssueType.partOf() += issueTemplate()
        return neoOperations.findAll(IssueType::class.java).awaitFirstOrNull() ?: neoOperations.save(newIssueType)
            .awaitSingle()
    }

    /**
     * Get the default issue state
     * @param isOpen whether the issue state is open or closed
     * @return the default issue state
     */
    suspend fun issueState(isOpen: Boolean): IssueState {
        val newIssueState = IssueState(if (isOpen) "open" else "closed", "", isOpen)
        newIssueState.partOf() += issueTemplate()
        return neoOperations.findAll(IssueState::class.java).filter { it.isOpen == isOpen }.awaitFirstOrNull()
            ?: neoOperations.save(newIssueState).awaitSingle()
    }

    /**
     * Get a IMSUser for a Jira user
     * @param imsProject the project to map the user to
     * @param user the Jira user
     */
    suspend fun mapUser(imsProject: IMSProject, user: JsonElement): User {
        return userMapper.mapUser(
            imsProject,
            user.jsonObject["accountId"]!!.jsonPrimitive.content,
            user.jsonObject["displayName"]!!.jsonPrimitive.content,
            user.jsonObject["emailAddress"]?.jsonPrimitive?.content
        )
    }

    /**
     * Map a label from Jira to Gropius
     * @param imsProject the project to map the label to
     * @param label the label to map
     */
    suspend fun mapLabel(imsProject: IMSProject, label: String): Label {
        val trackable = imsProject.trackable().value
        val labels = trackable.labels().filter { it.name == label }
        if (labels.isEmpty()) {
            val label = Label(OffsetDateTime.now(), OffsetDateTime.now(), label, "Jira Lebl", "000000")
            label.createdBy().value = userMapper.mapUser(imsProject, "jira-user")
            label.lastModifiedBy().value = label.createdBy().value
            label.trackables() += trackable
            return neoOperations.save(label).awaitSingle()
        } else if (labels.size == 1) {
            return labels.single()
        } else {
            return labels.first()
        }
    }
}