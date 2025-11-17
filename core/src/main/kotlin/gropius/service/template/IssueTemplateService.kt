package gropius.service.template

import gropius.authorization.GropiusAuthorizationContext
import gropius.dto.input.template.CreateIssueTemplateInput
import gropius.dto.input.template.UpdateIssueTemplateInput
import gropius.model.template.*
import gropius.repository.findById
import gropius.repository.template.IssueTemplateRepository
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service

/**
 * Service for [IssueTemplate]s. Provides functions to create, update and delete
 *
 * @param repository the associated repository used for CRUD functionality
 */
@Service
class IssueTemplateService(
    repository: IssueTemplateRepository
) : AbstractTemplateService<IssueTemplate, IssueTemplateRepository>(repository) {

    /**
     * Creates a new [IssueTemplate] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines the [IssueTemplate]
     * @return the saved created [IssueTemplate]
     */
    suspend fun createIssueTemplate(
        authorizationContext: GropiusAuthorizationContext, input: CreateIssueTemplateInput
    ): IssueTemplate {
        input.validate()
        checkCreateTemplatePermission(authorizationContext)
        val template = IssueTemplate(input.name, input.description, mutableMapOf(), false)
        createdTemplate(template, input)
        template.issueTypes() += input.issueTypes.map { IssueType(it.name, it.description, it.iconPath) }
        template.issueTypes() += template.extends().flatMap { it.issueTypes() }
        template.issueStates() += input.issueStates.map { IssueState(it.name, it.description, it.isOpen) }
        template.issueStates() += template.extends().flatMap { it.issueStates() }
        template.assignmentTypes() += input.assignmentTypes.map { AssignmentType(it.name, it.description) }
        template.assignmentTypes() += template.extends().flatMap { it.assignmentTypes() }
        template.issuePriorities() += input.issuePriorities.map { IssuePriority(it.name, it.description, it.value) }
        template.issuePriorities() += template.extends().flatMap { it.issuePriorities() }
        template.relationTypes() += input.relationTypes.map { IssueRelationType(it.name, it.description, it.inverseName) }
        template.relationTypes() += template.extends().flatMap { it.relationTypes() }
        return repository.save(template).awaitSingle()
    }

    /**
     * Updates an [IssueTemplate] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [IssueTemplate] to update and how
     * @return the updated [IssueTemplate]
     */
    suspend fun updateIssueTemplate(
        authorizationContext: GropiusAuthorizationContext, input: UpdateIssueTemplateInput
    ): IssueTemplate {
        input.validate()
        checkCreateTemplatePermission(authorizationContext)
        val template = repository.findById(input.id)
        updateNamedNode(template, input)
        return repository.save(template).awaitSingle()
    }

}