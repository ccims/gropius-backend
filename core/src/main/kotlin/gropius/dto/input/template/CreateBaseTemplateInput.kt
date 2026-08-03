package gropius.dto.input.template

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.execution.OptionalInput
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import gropius.dto.input.common.CreateNamedNodeInput
import gropius.dto.input.common.JSONFieldInput
import gropius.dto.input.common.validateAndEnsureNoDuplicates
import gropius.dto.input.ifPresent
import gropius.model.template.BaseTemplate
import gropius.util.schema.Schema
import kotlin.properties.Delegates

/**
 * Fragment for create mutation inputs for classes extending [BaseTemplate]
 */
abstract class CreateBaseTemplateInput : CreateNamedNodeInput() {

    @GraphQLDescription(
        """Additional initial templateFieldSpecifications, should be a JSON schema JSON.
        Must be disjoint with templateFieldSpecifications of templates this template extends.
        """
    )
    var templateFieldSpecifications: OptionalInput<List<JSONFieldInput>> by Delegates.notNull()

    override fun validate() {
        super.validate()
        templateFieldSpecifications.ifPresent {
            it.validateAndEnsureNoDuplicates()
            for (field in it) {
                val schema = field.value as JsonNode
                val parsedSchema = ObjectMapper().treeToValue(schema, Schema::class.java)
                parsedSchema.verify()
                validateJsonSchema(parsedSchema, field.name)
            }
        }
    }

    /**
     * Can be overridden to further validate the schema
     *
     * @param schema the schema to validate
     * @param name the name of the field
     */
    open fun validateJsonSchema(schema: Schema, name: String) {}
}