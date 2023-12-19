package gropius.sync.github

import gropius.model.architecture.IMSProject
import gropius.model.issue.Label
import gropius.model.template.*
import gropius.sync.SyncDataService
import gropius.sync.github.generated.fragment.LabelData
import gropius.sync.model.LabelInfo
import gropius.sync.repository.LabelInfoRepository
import gropius.sync.user.UserMapper
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.neo4j.core.ReactiveNeo4jOperations
import org.springframework.data.neo4j.core.findById
import org.springframework.stereotype.Component
import java.time.OffsetDateTime

/**
 * Service to handle data from GitHub
 * @param issuePileService the issue pile service to use
 * @param userMapper the user mapper to use
 * @param neoOperations Reference for the spring instance of ReactiveNeo4jOperations
 * @param labelInfoRepository the label info repository to use
 */
@Component
class GithubDataService(
    val issuePileService: IssuePileService,
    val userMapper: UserMapper,
    @Qualifier("graphglueNeo4jOperations")
    val neoOperations: ReactiveNeo4jOperations,
    val labelInfoRepository: LabelInfoRepository
) : SyncDataService {

    /**
     * Find and ensure the IMSTemplate in the database
     * @return the IssueTemplate
     */
    suspend fun issueTemplate(): IssueTemplate {
        return neoOperations.findAll(IssueTemplate::class.java).awaitFirstOrNull() ?: neoOperations.save(
            IssueTemplate("noissue", "", mutableMapOf(), false)
        ).awaitSingle()
    }

    /**
     * Find and ensure the IMSIssueTemplate in the database
     * @return the IssueType
     */
    suspend fun issueType(): IssueType {
        val newIssueType = IssueType("type", "", "")
        newIssueType.partOf() += issueTemplate()
        return neoOperations.findAll(IssueType::class.java).awaitFirstOrNull() ?: neoOperations.save(newIssueType)
            .awaitSingle()
    }

    /**
     * Find and ensure the IMSIssueTemplate in the database
     * @param isOpen whether the state is open or closed
     * @return the IssueState
     */
    suspend fun issueState(isOpen: Boolean): IssueState {
        val newIssueState = IssueState(if (isOpen) "open" else "closed", "", isOpen)
        newIssueState.partOf() += issueTemplate()
        return neoOperations.findAll(IssueState::class.java).filter { it.isOpen == isOpen }.awaitFirstOrNull()
            ?: neoOperations.save(newIssueState).awaitSingle()
    }

    /**
     * Map a Label from GitHub to Gropius
     * @param imsProject the Gropius IMSProject to use as input
     * @param labelData the label data to map
     * @return the mapped Label
     */
    suspend fun mapLabel(imsProject: IMSProject, labelData: LabelData): Label? {
        val labelInfo = labelInfoRepository.findByImsProjectAndGithubId(imsProject.rawId!!, labelData.id)
        if (labelInfo != null) {
            return neoOperations.findById<Label>(labelInfo.neo4jId)
        }
        var label = Label(
            labelData.createdAt ?: OffsetDateTime.MIN,
            labelData.createdAt ?: OffsetDateTime.MIN,
            labelData.name,
            "GitHub Label",
            labelData.color
        )
        label.createdBy().value = userMapper.mapUser(imsProject, "github-user")
        label.lastModifiedBy().value = label.createdBy().value
        label.trackables() += imsProject.trackable().value
        label = neoOperations.save(label).awaitSingle()
        labelInfoRepository.save(LabelInfo(imsProject.rawId!!, labelData.id, label.rawId!!)).awaitSingle()
        return label
    }
}