package gropius.sync.jira

import gropius.model.architecture.IMSProject
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

@Component
class JiraDataService(
    //val issuePileService: IssuePileService,
    val userMapper: UserMapper,
    @Qualifier("graphglueNeo4jOperations")
    val neoOperations: ReactiveNeo4jOperations
) : SyncDataService {
    suspend fun issueTemplate(): IssueTemplate {
        return neoOperations.findAll(IssueTemplate::class.java).awaitFirstOrNull() ?: neoOperations.save(
            IssueTemplate("noissue", "", mutableMapOf(), false)
        ).awaitSingle()
    }

    suspend fun issueType(): IssueType {
        val newIssueType = IssueType("type", "", "")
        newIssueType.partOf() += issueTemplate()
        return neoOperations.findAll(IssueType::class.java).awaitFirstOrNull() ?: neoOperations.save(newIssueType)
            .awaitSingle()
    }

    suspend fun issueState(): IssueState {
        val newIssueState = IssueState("open", "", true)
        newIssueState.partOf() += issueTemplate()
        return neoOperations.findAll(IssueState::class.java).awaitFirstOrNull() ?: neoOperations.save(newIssueState)
            .awaitSingle()
    }

    suspend fun mapUser(imsProject: IMSProject, user: JsonElement): User {
        return userMapper.mapUser(
            imsProject,
            user.jsonObject["accountId"]!!.jsonPrimitive.content,
            user.jsonObject["displayName"]!!.jsonPrimitive.content,
            user.jsonObject["emailAddress"]!!.jsonPrimitive.content
        )
    }
}