package gropius.service.template

import gropius.dto.input.ifPresent
import gropius.dto.input.orElse
import gropius.dto.input.template.CreateRelationPartnerTemplateInput
import gropius.dto.input.template.UpdateRelationPartnerTemplateInput
import gropius.model.template.RelationPartnerTemplate
import gropius.model.template.style.BaseStyle
import gropius.model.template.style.FillStyle
import gropius.model.template.style.StrokeStyle
import gropius.repository.GropiusRepository
import gropius.repository.common.NodeRepository
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.beans.factory.annotation.Autowired

/**
 * Base class for services for subclasses of [RelationPartnerTemplate]
 *
 * @param repository the associated repository used for CRUD functionality
 * @param T the type of Node this service is used for
 * @param R Repository type associated with [T]
 */
abstract class AbstractRelationPartnerTemplateService<T : RelationPartnerTemplate<*, T>, R : GropiusRepository<T, String>>(
    repository: R
) : AbstractTemplateService<T, R>(repository) {

    /**
     * Injected [NodeRepository], used to delete replaced [BaseStyle]s
     */
    @Autowired
    private lateinit var nodeRepository: NodeRepository

    /**
     * Updates [template] based on [input]
     * Calls [createdTemplate]
     * Sets the [RelationPartnerTemplate.possibleStartOfRelations] and [RelationPartnerTemplate.possibleEndOfRelations]
     * based on extended Templates
     *
     * @param template the [RelationPartnerTemplate] to update
     * @param input specifies added templateFieldSpecifications
     */
    suspend fun createdRelationPartnerTemplate(template: T, input: CreateRelationPartnerTemplateInput) {
        createdTemplate(template, input)
        template.possibleStartOfRelations() += template.extends().flatMap { it.possibleStartOfRelations() }
        template.possibleEndOfRelations() += template.extends().flatMap { it.possibleEndOfRelations() }
        input.fill.ifPresent {
            template.fill().value = FillStyle(it.color)
        }
        input.stroke.ifPresent {
            template.stroke().value = StrokeStyle(it.color.orElse(null), it.dash.orElse(null))
        }
    }

    /**
     * Updates [template] based on [input], saves it and deletes the styles it replaced
     * Does not check the authorization status
     *
     * @param template the [RelationPartnerTemplate] to update
     * @param input specifies how to update [template]
     * @return the saved updated [RelationPartnerTemplate]
     */
    suspend fun updateRelationPartnerTemplate(template: T, input: UpdateRelationPartnerTemplateInput): T {
        updateNamedNode(template, input)
        input.shapeRadius.ifPresent {
            template.shapeRadius = it
        }
        input.shapeType.ifPresent {
            template.shapeType = it
        }
        val replacedStyles = mutableListOf<BaseStyle>()
        input.fill.ifPresent { fill ->
            template.fill().value?.let { replacedStyles += it }
            template.fill().value = fill?.let { FillStyle(it.color) }
        }
        input.stroke.ifPresent { stroke ->
            template.stroke().value?.let { replacedStyles += it }
            template.stroke().value = stroke?.let { StrokeStyle(it.color.orElse(null), it.dash.orElse(null)) }
        }
        val savedTemplate = repository.save(template).awaitSingle()
        // a replaced style is only reachable via the template, so it can only be deleted once the saved template
        // no longer references it
        if (replacedStyles.isNotEmpty()) {
            nodeRepository.deleteAll(replacedStyles).awaitSingleOrNull()
        }
        return savedTemplate
    }

}
