package gropius.service.issue

import gropius.authorization.GropiusAuthorizationContext
import gropius.dto.input.common.DeleteNodeInput
import gropius.dto.input.ifPresent
import gropius.dto.input.isPresent
import gropius.dto.input.issue.CreateArtefactInput
import gropius.dto.input.issue.UpdateArtefactInput
import gropius.model.architecture.Trackable
import gropius.model.issue.Artefact
import gropius.model.issue.Issue
import gropius.model.template.ArtefactTemplate
import gropius.model.user.permission.TrackablePermission
import gropius.repository.architecture.TrackableRepository
import gropius.repository.findById
import gropius.repository.issue.ArtefactRepository
import gropius.repository.template.ArtefactTemplateRepository
import gropius.service.common.AuditedNodeService
import gropius.service.template.TemplatedNodeService
import io.github.graphglue.authorization.Permission
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

/**
 * Service for [Artefact]s. Provides functions to create, update and delete
 *
 * @param repository the associated repository used for CRUD functionality
 * @param templatedNodeService service used to update templatedFields
 * @param trackableRepository used to get [Trackable]s by id
 * @param artefactTemplateRepository used to get [ArtefactTemplate]
 * @param issueService used to remove [Artefact]s from [Issue]s
 */
@Service
class ArtefactService(
    repository: ArtefactRepository,
    private val templatedNodeService: TemplatedNodeService,
    private val trackableRepository: TrackableRepository,
    private val artefactTemplateRepository: ArtefactTemplateRepository,
    private val issueService: IssueService
) : AuditedNodeService<Artefact, ArtefactRepository>(repository) {

    /**
     * Creates a new [Artefact] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines the [Artefact]
     * @return the saved created [Artefact]
     */
    suspend fun createArtefact(
        authorizationContext: GropiusAuthorizationContext, input: CreateArtefactInput
    ): Artefact {
        input.validate()
        val trackable = trackableRepository.findById(input.trackable)
        checkPermission(
            trackable, Permission(TrackablePermission.MANAGE_ARTEFACTS, authorizationContext), "manage Artefacts"
        )
        val now = OffsetDateTime.now()
        val template = artefactTemplateRepository.findById(input.template)
        val templatedFields = templatedNodeService.validateInitialTemplatedFields(template, input)
        val artefact = Artefact(now, now, templatedFields, input.file, input.from, input.to, input.version)
        artefact.template().value = template
        artefact.trackable().value = trackable
        createdAuditedNode(artefact, getUser(authorizationContext))
        return repository.save(artefact).awaitSingle()
    }

    /**
     * Updates an [Artefact] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [Artefact] to update and how
     * @return the updated saved [Artefact]
     */
    suspend fun updateArtefact(
        authorizationContext: GropiusAuthorizationContext, input: UpdateArtefactInput
    ): Artefact {
        input.validate()
        val artefact = repository.findById(input.id)
        checkPermission(
            artefact, Permission(TrackablePermission.MANAGE_ARTEFACTS, authorizationContext), "manage Artefacts"
        )
        input.template.ifPresent {
            artefact.template().value = artefactTemplateRepository.findById(it)
        }
        templatedNodeService.updateTemplatedFields(artefact, input, input.template.isPresent)
        input.file.ifPresent { artefact.file = it }
        input.from.ifPresent { artefact.from = it }
        input.to.ifPresent { artefact.to = it }
        input.version.ifPresent { artefact.version = it }
        updateAuditedNode(artefact, getUser(authorizationContext), OffsetDateTime.now())
        return repository.save(artefact).awaitSingle()
    }

    /**
     * Deletes an [Artefact] by id
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [Artefact] to delete
     */
    suspend fun deleteArtefact(authorizationContext: GropiusAuthorizationContext, input: DeleteNodeInput) {
        input.validate()
        val artefact = repository.findById(input.id)
        artefact.referencingComments().clear()
        val now = OffsetDateTime.now()
        val user = getUser(authorizationContext)
        for (issue in artefact.issues()) {
            issueService.removeArtefactFromIssue(issue, artefact, now, user)
        }
        issueService.repository.saveAll(artefact.issues()).collectList().awaitSingle()
        repository.delete(artefact).awaitSingleOrNull()
    }

}