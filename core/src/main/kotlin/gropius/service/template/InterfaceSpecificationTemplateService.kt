package gropius.service.template

import gropius.authorization.GropiusAuthorizationContext
import gropius.dto.input.orElse
import gropius.dto.input.template.CreateInterfaceSpecificationTemplateInput
import gropius.model.template.*
import gropius.repository.findAllById
import gropius.repository.template.ComponentTemplateRepository
import gropius.repository.template.InterfaceSpecificationTemplateRepository
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service

/**
 * Service for [InterfaceSpecificationTemplate]s. Provides functions to create, update and delete
 *
 * @param repository the associated repository used for CRUD functionality
 * @param componentTemplateRepository used to get [ComponentTemplate]s
 * @param subTemplateService used to create [SubTemplate]s
 */
@Service
class InterfaceSpecificationTemplateService(
    repository: InterfaceSpecificationTemplateRepository,
    private val componentTemplateRepository: ComponentTemplateRepository,
    private val subTemplateService: SubTemplateService
) : RelationPartnerTemplateService<InterfaceSpecificationTemplate, InterfaceSpecificationTemplateRepository>(repository) {

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
        template.canBeVisibleOnComponents() += componentTemplateRepository.findAllById(input.canBeVisibleOnComponents)
        template.canBeVisibleOnComponents() += template.extends().flatMap { it.canBeVisibleOnComponents() }
        template.canBeInvisibleOnComponents() += componentTemplateRepository.findAllById(input.canBeInvisibleOnComponents)
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
        template.interfaceTemplate().value = subTemplateService.createSubTemplate(
            ::InterfaceTemplate,
            input.interfaceTemplate,
            extendedTemplates.map { it.interfaceTemplate().value })
        template.interfaceDefinitionTemplate().value =
            subTemplateService.createSubTemplate(::InterfaceDefinitionTemplate,
                input.interfaceDefinitionTemplate,
                extendedTemplates.map { it.interfaceDefinitionTemplate().value })
    }

}