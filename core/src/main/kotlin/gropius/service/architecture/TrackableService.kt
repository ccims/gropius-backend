package gropius.service.architecture

import gropius.dto.input.architecture.UpdateTrackableInput
import gropius.dto.input.ifPresent
import gropius.model.architecture.Trackable
import gropius.repository.GropiusRepository
import gropius.repository.common.NodeRepository
import gropius.repository.issue.ArtefactRepository
import gropius.repository.issue.LabelRepository
import gropius.service.issue.IssueService
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.beans.factory.annotation.Autowired

/**
 * Base class for services for subclasses of [Trackable]
 *
 * @param repository the associated repository used for CRUD functionality
 */
abstract class TrackableService<T : Trackable, R : GropiusRepository<T, String>>(
    repository: R
) : NamedAffectedByIssueService<T, R>(repository) {

    /**
     * Injected [IssueService]
     */
    @Autowired
    lateinit var issueService: IssueService

    /**
     * Injected [LabelRepository]
     */
    @Autowired
    lateinit var labelRepository: LabelRepository

    /**
     * Injected [ArtefactRepository]
     */
    @Autowired
    lateinit var artefactRepository: ArtefactRepository

    /**
     * Injected [IMSProjectService]
     */
    @Autowired
    lateinit var imsProjectService: IMSProjectService

    /**
     * Injected [NodeRepository]
     */
    @Autowired
    lateinit var nodeRepository: NodeRepository

    /**
     * Updates [node] based on [input]
     * Calls [updateNamedAffectedByIssue]
     * Updates repositoryURL
     *
     * @param node the node to update
     * @param input defines how to update the provided [node]
     */
    fun updateTrackable(node: Trackable, input: UpdateTrackableInput) {
        updateNamedAffectedByIssue(node, input)
        input.repositoryURL.ifPresent {
            node.repositoryURL = it
        }
    }

    /**
     * Must be called before [node] is deleted via the repository
     * Deletes all Artefacts
     * Deletes Labels and Issues, if only present on this Trackable
     *
     * @param node the [Trackable] which will be deleted
     */
    suspend fun beforeDeleteTrackable(node: Trackable) {
        val labelsToDelete = node.labels().filter {
            it.trackables().size == 1
        }
        val issuesToDelete = node.issues().filter {
            it.trackables().size == 1
        }.flatMap { issueService.prepareIssueDeletion(it) }
        val imsProjectsToDelete = node.syncsTo().flatMap { imsProjectService.getNodesToDelete(it) }
        node.syncsTo().forEach {
            imsProjectService.deleteIMSProject(it)
        }
        nodeRepository.deleteAll(labelsToDelete + imsProjectsToDelete + issuesToDelete + node.artefacts())
            .awaitSingleOrNull()
    }

}