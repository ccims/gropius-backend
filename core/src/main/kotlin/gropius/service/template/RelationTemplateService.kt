package gropius.service.template

import gropius.authorization.GropiusAuthorizationContext
import gropius.dto.input.ifPresent
import gropius.dto.input.orElse
import gropius.dto.input.template.CreateRelationTemplateInput
import gropius.dto.input.template.RelationConditionInput
import gropius.dto.input.template.UpdateRelationTemplateInput
import gropius.model.template.*
import gropius.model.template.style.StrokeStyle
import gropius.repository.findById
import gropius.repository.template.RelationTemplateRepository
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service

/**
 * Service for [RelationTemplate]s. Provides functions to create, update and delete
 *
 * @param repository the associated repository used for CRUD functionality
 * @param interfaceSpecificationTemplateService used to find extending [InterfaceSpecificationTemplate]s
 * @param relationPartnerTemplateService used to find extending [RelationPartnerTemplate]s
 */
@Service
class RelationTemplateService(
    repository: RelationTemplateRepository,
    private val interfaceSpecificationTemplateService: InterfaceSpecificationTemplateService,
    private val relationPartnerTemplateService: RelationPartnerTemplateService
) : AbstractTemplateService<RelationTemplate, RelationTemplateRepository>(repository) {

    /**
     * Creates a new [RelationTemplate] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines the [RelationTemplate]
     * @return the saved created [RelationTemplate]
     */
    suspend fun createRelationTemplate(
        authorizationContext: GropiusAuthorizationContext, input: CreateRelationTemplateInput
    ): RelationTemplate {
        input.validate()
        checkCreateTemplatePermission(authorizationContext)
        val template = RelationTemplate(input.name, input.description, mutableMapOf(), false, input.markerType)
        input.stroke.ifPresent {
            template.stroke().value = StrokeStyle(it.color.orElse(null), it.dash.orElse(null))
        }
        createdTemplate(template, input)
        template.relationConditions() += input.relationConditions.map {
            createRelationCondition(it)
        }
        template.relationConditions() += template.extends().flatMap { it.relationConditions() }
        return repository.save(template).awaitSingle()
    }

    /**
     * Creates a new [RelationCondition] based on [input]
     * Does not check permission or save the created [RelationCondition]
     *
     * @param input defines the created [RelationCondition]
     * @return the created [RelationCondition]
     */
    private suspend fun createRelationCondition(
        input: RelationConditionInput
    ): RelationCondition {
        val relationCondition = RelationCondition()
        relationCondition.interfaceSpecificationDerivationConditions() += input.interfaceSpecificationDerivationConditions.map {
            val condition = InterfaceSpecificationDerivationCondition(
                it.derivesVisibleSelfDefined,
                it.derivesInvisibleSelfDefined,
                it.derivesVisibleDerived,
                it.derivesInvisibleDerived,
                it.isVisibleDerived,
                it.isInvisibleDerived
            )
            condition.derivableInterfaceSpecifications() += interfaceSpecificationTemplateService.findAllByIdWithExtending(it.derivableInterfaceSpecifications)
            condition
        }
        relationCondition.from() += relationPartnerTemplateService.findAllByIdWithExtending(input.from)
        relationCondition.to() += relationPartnerTemplateService.findAllByIdWithExtending(input.to)
        return relationCondition
    }

    /**
     * Updates a [RelationTemplate] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [RelationTemplate] to update and how
     * @return the updated [RelationTemplate]
     */
    suspend fun updateRelationTemplate(
        authorizationContext: GropiusAuthorizationContext, input: UpdateRelationTemplateInput
    ): RelationTemplate {
        input.validate()
        checkCreateTemplatePermission(authorizationContext)
        val template = repository.findById(input.id)
        updateNamedNode(template, input)
        input.markerType.ifPresent {
            template.markerType = it
        }
        return repository.save(template).awaitSingle()
    }

}