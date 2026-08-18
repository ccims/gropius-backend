package gropius.service.template

import com.expediagroup.graphql.generator.execution.OptionalInput
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.node.JsonNodeFactory
import gropius.dto.input.common.JSONFieldInput
import gropius.dto.input.ifPresent
import gropius.dto.input.orElse
import gropius.dto.input.template.CreateTemplatedNodeInput
import gropius.dto.input.template.UpdateTemplatedNodeInput
import gropius.model.template.BaseTemplate
import gropius.model.template.TemplatedNode
import gropius.util.JsonNodeMapper
import gropius.util.schema.Schema
import gropius.util.schema.Validator
import org.springframework.stereotype.Service

/**
 * Service for [TemplatedNode]s. Provides functions to create, update and delete
 *
 * @param objectMapper injected [ObjectMapper], used to parse [JSONFieldInput]
 * @param jsonNodeMapper used to map [JsonNode] to [String]
 */
@Service
class TemplatedNodeService(val objectMapper: ObjectMapper, val jsonNodeMapper: JsonNodeMapper, val validator: Validator) {

    /**
     * Updates the [TemplatedNode.templatedFields] of a [TemplatedNode] based on the [input]
     * Does not check authorization permissions, and does not save the provided [node] afterwards
     * Validates the new value of the fields
     * If [templateWasUpdated], validates all fields. Requires that the new template is already set
     *
     * @param node the node to update the templated fields of
     * @param input defines how to update the templated fields
     * @param templateWasUpdated if true, no longer existing templatedFields are removed, and all existing fields
     *   are validated
     * @throws IllegalArgumentException if the new/old value for a templated field is invalid
     */
    suspend fun updateTemplatedFields(
        node: TemplatedNode, input: UpdateTemplatedNodeInput, templateWasUpdated: Boolean = false
    ) {
        updateTemplatedFields(node, input.templatedFields, templateWasUpdated)
    }

    /**
     * Updates the [TemplatedNode.templatedFields] of a [TemplatedNode] based on the [templatedFields]
     * Does not check authorization permissions, and does not save the provided [node] afterwards
     * Validates the new value of the fields
     * If [templateWasUpdated], validates all fields. Requires that the new template is already set
     *
     * @param node the node to update the templated fields of
     * @param templatedFields values to update the templatedFields
     * @param templateWasUpdated if true, no longer existing templatedFields are removed, and all existing fields
     *   are validated
     * @throws IllegalArgumentException if the new/old value for a templated field is invalid, or a field does not exist
     */
    suspend fun updateTemplatedFields(
        node: TemplatedNode, templatedFields: OptionalInput<List<JSONFieldInput>>, templateWasUpdated: Boolean
    ) {
        templatedFields.ifPresent { fields ->
            val template = node.template().value
            ensureTemplatedFieldsExist(template, fields.map { it.name })
            for (field in fields) {
                updateTemplatedField(node, field)
            }
        }
        if (templateWasUpdated) {
            validateTemplatedFieldsAfterTemplateUpdate(node, templatedFields)
        }

    }

    /**
     * Updates the [TemplatedNode.templatedFields] of a [TemplatedNode] based on [field]
     * Does not check authorization permissions, and does not save the provided [node] afterwards.
     * Validates the new value of the updated field.
     *
     * @param node the node to update the templated field of
     * @param field the name and new value of the field to update
     * @throws IllegalArgumentException if the new value for the field is invalid, or the field does not exist
     */
    suspend fun updateTemplatedField(node: TemplatedNode, field: JSONFieldInput) {
        val template = node.template().value
        ensureTemplatedFieldExist(template, field.name)
        validateField(field.value as JsonNode?, template.templateFieldSpecifications[field.name]!!, field.name)
        node.templatedFields[field.name] = jsonNodeMapper.jsonNodeToDeterministicString(field.value)
    }

    /**
     * After the template of [node] was updated, validate all fields and remove fields which are no longer necessary
     *
     * @param node the node of which to validate the fields
     * @param templatedFields values to update the templatedFields
     * @throws IllegalArgumentException if the old value for a templated field is invalid
     */
    private suspend fun validateTemplatedFieldsAfterTemplateUpdate(
        node: TemplatedNode, templatedFields: OptionalInput<List<JSONFieldInput>>
    ) {
        val template = node.template().value
        val changedFields = templatedFields.orElse(emptyList()).map { it.name }
        val fieldsToRemove = mutableListOf<String>()
        for ((name, value) in node.templatedFields) {
            if (name in changedFields) {
                continue
            } else if (name !in template.templateFieldSpecifications) {
                fieldsToRemove += name
            } else {
                validateField(objectMapper.readTree(value), template.templateFieldSpecifications[name]!!, name)
            }
        }
        node.templatedFields -= fieldsToRemove.toSet()
    }

    /**
     * Validates the initial input for [TemplatedNode.templatedFields]
     * If no input is provided for a field, `null` is assigned as default value
     *
     * @param template the initial associated template for the node to create
     * @param input input of the create mutation, defines the initial value for the templated fields
     * @return the initial value for [TemplatedNode.templatedFields]
     * @throws IllegalArgumentException if the value for a templated field is not compatible with its schema
     */
    suspend fun validateInitialTemplatedFields(
        template: BaseTemplate<*, *>, input: CreateTemplatedNodeInput
    ): MutableMap<String, String> {
        return validateInitialTemplatedFields(template, input.templatedFields)
    }

    /**
     * Validates the initial input for [TemplatedNode.templatedFields]
     * If no input is provided for a field, `null` is assigned as default value
     *
     * @param template the initial associated template for the node to create
     * @param fields initial templated fields
     * @return the initial value for [TemplatedNode.templatedFields]
     * @throws IllegalArgumentException if the value for a templated field is not compatible with its schema
     */
    suspend fun validateInitialTemplatedFields(
        template: BaseTemplate<*, *>, fields: List<JSONFieldInput>
    ): MutableMap<String, String> {
        ensureTemplatedFieldsExist(template, fields.map { it.name })
        val fieldLookup = fields.associateBy { it.name }
        return template.templateFieldSpecifications.mapValues {
            val value = fieldLookup[it.key]?.value as JsonNode?
            validateField(value, it.value, it.key)
            jsonNodeMapper.jsonNodeToDeterministicString(value)
        }.toMutableMap()
    }

    /**
     * Validates a value of a templated field
     *
     * @param templatedNode the [TemplatedNode] of which a field should be validated
     * @param field the name of the templated field to validate
     * @return `true` if the current value of the templated field is compatible with the template
     */
    suspend fun validateTemplatedField(templatedNode: TemplatedNode, field: String): Boolean {
        val unparsedValue = templatedNode.templatedFields[field]
        return if (unparsedValue != null) {
            val schema = templatedNode.template().value.templateFieldSpecifications[field]!!
            val parsedSchema = objectMapper.readValue(schema, Schema::class.java)
            val validationResult = validator.validate(parsedSchema, objectMapper.readTree(unparsedValue))
            return validationResult.isEmpty()
        } else {
            false
        }
    }

    /**
     * Validates a value for a templated field
     *
     * @param value the new value for the templated field
     * @param schema JSON schema of the templated field obtained from the associated Template
     * @param name the name of the field
     * @throws IllegalArgumentException if the [schema] does not allow [value]
     */
    private fun validateField(value: JsonNode?, schema: String, name: String) {
        val parsedSchema = objectMapper.readValue(schema, Schema::class.java)
        val validationResult = validator.validate(parsedSchema, value ?: JsonNodeFactory.instance.nullNode())
        if (validationResult.isNotEmpty()) {
            throw IllegalArgumentException("Invalid input for templated field $name: ${validationResult.map { it.message }}")
        }
    }

    /**
     * Ensures that all fields in [fields] are defined by [template]
     *
     * @param template the Template of the [TemplatedNode] where [fields] should be used
     * @param fields a list of templated field names to check for existence
     * @throws IllegalArgumentException if any field is not defined by [template]
     */
    private fun ensureTemplatedFieldsExist(template: BaseTemplate<*, *>, fields: Collection<String>) {
        for (field in fields) {
            ensureTemplatedFieldExist(template, field)
        }
    }

    /**
     * Ensures that [field] is defined by [template]
     *
     * @param template the Template of the [TemplatedNode] where [field] should be used
     * @param field a list of templated field names to check for existence
     * @throws IllegalArgumentException if any field is not defined by [template]
     */
    fun ensureTemplatedFieldExist(template: BaseTemplate<*, *>, field: String) {
        if (field !in template.templateFieldSpecifications) {
            throw IllegalArgumentException("Unknown templated field $field")
        }
    }
}