package gropius.service.template

import gropius.authorization.GropiusAuthorizationContext
import gropius.dto.input.template.UpdateIMSIssueTemplateInput
import gropius.dto.input.template.UpdateIMSProjectTemplateInput
import gropius.dto.input.template.UpdateIMSTemplateInput
import gropius.dto.input.template.UpdateIMSUserTemplateInput
import gropius.model.template.IMSIssueTemplate
import gropius.model.template.IMSProjectTemplate
import gropius.model.template.IMSTemplate
import gropius.model.template.IMSUserTemplate
import gropius.repository.findById
import gropius.repository.template.IMSTemplateRepository
import gropius.repository.template.SubTemplateRepository
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service

/**
 * Service for [IMSTemplate]s. Provides functions to create, update and delete
 *
 * @param repository the associated repository used for CRUD functionality
 * @param subTemplateRepository used to find and save SubTemplates
 */
@Service
class IMSTemplateService(
    repository: IMSTemplateRepository,
    private val subTemplateRepository: SubTemplateRepository
) : AbstractTemplateService<IMSTemplate, IMSTemplateRepository>(repository) {

    /**
     * Updates an [IMSTemplate] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [IMSTemplate] to update and how
     * @return the updated [IMSTemplate]
     */
    suspend fun updateIMSTemplate(
        authorizationContext: GropiusAuthorizationContext, input: UpdateIMSTemplateInput
    ): IMSTemplate {
        input.validate()
        checkCreateTemplatePermission(authorizationContext)
        val template = repository.findById(input.id)
        updateNamedNode(template, input)
        return repository.save(template).awaitSingle()
    }

    /**
     * Updates an [IMSProjectTemplate] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [IMSProjectTemplate] to update and how
     * @return the updated [IMSProjectTemplate]
     */
    suspend fun updateIMSProjectTemplate(
        authorizationContext: GropiusAuthorizationContext, input: UpdateIMSProjectTemplateInput
    ): IMSProjectTemplate {
        input.validate()
        checkCreateTemplatePermission(authorizationContext)
        val template = subTemplateRepository.findById(input.id) as IMSProjectTemplate
        updateNamedNode(template, input)
        return subTemplateRepository.save(template).awaitSingle() as IMSProjectTemplate
    }

    /**
     * Updates an [IMSIssueTemplate] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [IMSIssueTemplate] to update and how
     * @return the updated [IMSIssueTemplate]
     */
    suspend fun updateIMSIssueTemplate(
        authorizationContext: GropiusAuthorizationContext, input: UpdateIMSIssueTemplateInput
    ): IMSIssueTemplate {
        input.validate()
        checkCreateTemplatePermission(authorizationContext)
        val template = subTemplateRepository.findById(input.id) as IMSIssueTemplate
        updateNamedNode(template, input)
        return subTemplateRepository.save(template).awaitSingle() as IMSIssueTemplate
    }

    /**
     * Updates an [IMSUserTemplate] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [IMSUserTemplate] to update and how
     * @return the updated [IMSUserTemplate]
     */
    suspend fun updateIMSUserTemplate(
        authorizationContext: GropiusAuthorizationContext, input: UpdateIMSUserTemplateInput
    ): IMSUserTemplate {
        input.validate()
        checkCreateTemplatePermission(authorizationContext)
        val template = subTemplateRepository.findById(input.id) as IMSUserTemplate
        updateNamedNode(template, input)
        return subTemplateRepository.save(template).awaitSingle() as IMSUserTemplate
    }

}
