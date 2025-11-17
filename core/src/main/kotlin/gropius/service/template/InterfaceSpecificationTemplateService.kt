package gropius.service.template

import gropius.authorization.GropiusAuthorizationContext
import gropius.dto.input.ifPresent
import gropius.dto.input.orElse
import gropius.dto.input.template.CreateInterfaceSpecificationTemplateInput
import gropius.dto.input.template.UpdateInterfacePartTemplateInput
import gropius.dto.input.template.UpdateInterfaceSpecificationTemplateInput
import gropius.dto.input.template.UpdateInterfaceSpecificationVersionTemplateInput
import gropius.model.template.*
import gropius.repository.findById
import gropius.repository.template.InterfaceSpecificationTemplateRepository
import gropius.repository.template.SubTemplateRepository
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service

/**
 * Service for [InterfaceSpecificationTemplate]s. Provides functions to create, update and delete
 *
 * @param repository the associated repository used for CRUD functionality
 * @param componentTemplateService used to get [ComponentTemplate]s
 * @param subTemplateService used to create [SubTemplate]s
 */
@Service
class InterfaceSpecificationTemplateService(
    repository: InterfaceSpecificationTemplateRepository,
    private val componentTemplateService: ComponentTemplateService,
    private val subTemplateService: SubTemplateService,
    private val subTemplateRepository: SubTemplateRepository
) : AbstractRelationPartnerTemplateService<InterfaceSpecificationTemplate, InterfaceSpecificationTemplateRepository>(repository) {

    /**
     * Creates a new [InterfaceSpecificationTemplate] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines the [InterfaceSpecificationTemplate]
     * @return the saved created [InterfaceSpecificationTemplate]
     */
    suspend fun createInterfaceSpecificationTemplate(
        authorizationContext: GropiusAuthorizationContext, input: CreateInterfaceSpecificationTemplateInput
    ): InterfaceSpecificationTemplate {
        input.validate()
        checkCreateTemplatePermission(authorizationContext)
        val template = InterfaceSpecificationTemplate(
            input.name, input.description, mutableMapOf(), false, input.shapeRadius.orElse(null), input.shapeType
        )
        createdRelationPartnerTemplate(template, input)
        template.canBeVisibleOnComponents() += componentTemplateService.findAllByIdWithExtending(input.canBeVisibleOnComponents)
        template.canBeVisibleOnComponents() += template.extends().flatMap { it.canBeVisibleOnComponents() }
        template.canBeInvisibleOnComponents() += componentTemplateService.findAllByIdWithExtending(input.canBeInvisibleOnComponents)
        template.canBeInvisibleOnComponents() += template.extends().flatMap { it.canBeInvisibleOnComponents() }
        template.derivableBy() += template.extends().flatMap { it.derivableBy() }
        initSubTemplates(template, input)
        return repository.save(template).awaitSingle()
    }

    /**
     * Initializes the [SubTemplate]s of a created [InterfaceSpecificationTemplate]
     *
     * @param template the newly created [InterfaceSpecificationTemplate]
     * @param input used to create [template], defines the [SubTemplate]s to create
     */
    private suspend fun initSubTemplates(
        template: InterfaceSpecificationTemplate, input: CreateInterfaceSpecificationTemplateInput
    ) {
        val extendedTemplates = template.extends()
        template.interfaceSpecificationVersionTemplate().value =
            subTemplateService.createSubTemplate(::InterfaceSpecificationVersionTemplate,
                input.interfaceSpecificationVersionTemplate,
                extendedTemplates.map { it.interfaceSpecificationVersionTemplate().value })
        template.interfacePartTemplate().value = subTemplateService.createSubTemplate(::InterfacePartTemplate,
            input.interfacePartTemplate,
            extendedTemplates.map { it.interfacePartTemplate().value })
    }

    /**
     * Updates an [InterfaceSpecificationTemplate] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [InterfaceSpecificationTemplate] to update and how
     * @return the updated [InterfaceSpecificationTemplate]
     */
    suspend fun updateInterfaceSpecificationTemplate(
        authorizationContext: GropiusAuthorizationContext, input: UpdateInterfaceSpecificationTemplateInput
    ): InterfaceSpecificationTemplate {
        input.validate()
        checkCreateTemplatePermission(authorizationContext)
        val template = repository.findById(input.id)
        updateNamedNode(template, input)
        input.shapeRadius.ifPresent {
            template.shapeRadius = it
        }
        input.shapeType.ifPresent {
            template.shapeType = it
        }
        return repository.save(template).awaitSingle()
    }

    /**
     * Updates an [InterfaceSpecificationVersionTemplate] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [InterfaceSpecificationVersionTemplate] to update and how
     * @return the updated [InterfaceSpecificationVersionTemplate]
     */
    suspend fun updateInterfaceSpecificationVersionTemplate(
        authorizationContext: GropiusAuthorizationContext, input: UpdateInterfaceSpecificationVersionTemplateInput
    ): InterfaceSpecificationVersionTemplate {
        input.validate()
        checkCreateTemplatePermission(authorizationContext)
        val template = subTemplateRepository.findById(input.id) as InterfaceSpecificationVersionTemplate
        updateNamedNode(template, input)
        return subTemplateRepository.save(template).awaitSingle() as InterfaceSpecificationVersionTemplate
    }

    /**
     * Updates an [InterfacePartTemplate] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [InterfacePartTemplate] to update and how
     * @return the updated [InterfacePartTemplate]
     */
    suspend fun updateInterfacePartTemplate(
        authorizationContext: GropiusAuthorizationContext, input: UpdateInterfacePartTemplateInput
    ): InterfacePartTemplate {
        input.validate()
        checkCreateTemplatePermission(authorizationContext)
        val template = subTemplateRepository.findById(input.id) as InterfacePartTemplate
        updateNamedNode(template, input)
        return subTemplateRepository.save(template).awaitSingle() as InterfacePartTemplate
    }

}