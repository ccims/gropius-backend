package gropius.service.template

import com.fasterxml.jackson.databind.JsonNode
import gropius.authorization.GropiusAuthorizationContext
import gropius.dto.input.orElse
import gropius.dto.input.template.CreateBaseTemplateInput
import gropius.model.template.BaseTemplate
import gropius.model.template.SubTemplate
import gropius.model.template.Template
import gropius.model.user.permission.GlobalPermission
import gropius.repository.GropiusRepository
import gropius.service.common.NamedNodeService
import gropius.util.JsonNodeMapper
import io.github.graphglue.authorization.Permission
import org.springframework.beans.factory.annotation.Autowired

/**
 * Base class for services for subclasses of [BaseTemplate]
 *
 * @param repository the associated repository used for CRUD functionality
 * @param T the type of Node this service is used for
 * @param R Repository type associated with [T]
 */
abstract class BaseTemplateService<T : BaseTemplate<*, *>, R : GropiusRepository<T, String>>(
    repository: R
) : NamedNodeService<T, R>(repository) {

    /**
     * Injected [JsonNodeMapper]
     */
    @Autowired
    private lateinit var jsonNodeMapper: JsonNodeMapper

    /**
     * Updates [template] based on [input]
     * Sets templateFieldSpecifications based on [input], and the templateFieldSpecification
     * of [extendedTemplates]
     *
     * @param template the [BaseTemplate] to update
     * @param input specifies added templateFieldSpecifications
     * @param extendedTemplates [BaseTemplate]s [template] directly ([Template]) or indirectly ([SubTemplate])
     *   extends
     */
    fun createdBaseTemplate(template: T, input: CreateBaseTemplateInput, extendedTemplates: Collection<T>) {
        val additionalFields = input.templateFieldSpecifications.orElse(emptyList()).map {
            Pair(it.name, jsonNodeMapper.jsonNodeToDeterministicString(it.value as JsonNode))
        }
        val derivedFields =
            extendedTemplates.flatMap { it.templateFieldSpecifications.entries }.map { it.toPair() }.toSet()
        val allFields = derivedFields + additionalFields
        val groupedByName = allFields.groupBy { it.first }
        val duplicatesWithDifferentValues = groupedByName.filter { (_, values) ->
            values.map { it.second }.toSet().size > 1
        }.keys
        if (duplicatesWithDifferentValues.isNotEmpty()) {
            throw IllegalArgumentException("Duplicate names with different values found: $duplicatesWithDifferentValues")
        }
        for ((name, value) in allFields) {
            template.templateFieldSpecifications[name] = value
        }
    }

    /**
     * Checks for CAN_CREATE_TEMPLATE permission on the user provided via [authorizationContext]
     *
     * @param authorizationContext used to get the user to check for CAN_CREATE_TEMPLATE
     * @throws IllegalArgumentException if the permission is not granted
     */
    suspend fun checkCreateTemplatePermission(authorizationContext: GropiusAuthorizationContext) {
        val user = getUser(authorizationContext)
        checkPermission(
            user, Permission(GlobalPermission.CAN_CREATE_TEMPLATES, authorizationContext), "create/update Templates"
        )
    }
}