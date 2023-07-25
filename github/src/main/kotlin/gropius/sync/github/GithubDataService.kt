package gropius.sync.github

import gropius.model.template.*
import gropius.sync.SyncDataService
import gropius.sync.user.UserMapper
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.neo4j.core.ReactiveNeo4jOperations
import org.springframework.stereotype.Component

@Component
class GithubDataService(
    val issuePileService: IssuePileService,
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
}