package gropius.util.schema

import tools.jackson.databind.JsonNode
import org.springframework.stereotype.Component
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Validates schemas against instances, returning a list of validation errors.
 * Modified version of https://github.com/jsontypedef/json-typedef-java/blob/master/src/main/java/com/jsontypedef/jtd/Validator.java
 */
@Component
class Validator {

    companion object {
        /**
         * The maximum number of references `validate` will follow before
         * raising `MaxDepthExceededException`.
         */
        var MAX_DEPTH: Int = 100
        /**
         * The maximum number of errors `validate` may return.
         */
        var MAX_ERRORS: Int = 100
    }

    /**
     * Validate `schema` against `instance`, returning a list of
     * `ValidationError`.
     *
     * If there are no validation errors, then this method returns an empty list.
     * To limit the maximum number of errors returned, use `setMaxErrors`.
     *
     * The default behavior is to return all errors, and to follow an unlimited
     * number of references. For schemas with cyclic references, this may result
     * in a stack overflow.
     *
     * @param schema the schema to validate against
     * @param instance the JSON data to validate
     * @return a list of validation errors
     * @throws MaxDepthExceededException if the number of references followed exceeds the configured maximum depth
     */
    fun validate(schema: Schema, instance: JsonNode): List<ValidationError> {
        val state = ValidationState(schema)
        state.schemaTokens.add(ArrayList())
        state.maxErrors = MAX_ERRORS

        try {
            validate(state, schema, instance, null)
        } catch (e: MaxErrorsReachedException) {
            // Nothing to be done here. This is not an actual error condition, just a
            // circuit-breaker.
        }

        return state.errors
    }

    /**
     * Validate [schema] against [instance]
     *
     * @param schema the schema to validate against
     * @param instance the JSON data to validate
     * @param state the validation state, used to track errors
     * @param parentTag the parent tag, used to prevent infinite recursion
     */
    private fun validate(state: ValidationState, schema: Schema, instance: JsonNode, parentTag: String?) {
        if (schema.nullable && instance.isNull) {
            return
        }

        when (schema.form) {
            Form.REF -> {
                if (state.schemaTokens.size == MAX_DEPTH) {
                    throw MaxDepthExceededException()
                }

                state.schemaTokens.add(ArrayList())
                state.pushSchemaToken("definitions")
                state.pushSchemaToken(schema.ref!!)

                validate(state, state.root.definitions!![schema.ref]!!, instance, null)

                state.schemaTokens.removeAt(state.schemaTokens.size - 1)
            }

            Form.TYPE -> {
                state.pushSchemaToken("type")
                when (schema.type!!) {
                    Type.BOOLEAN -> if (!instance.isBoolean) {
                        state.pushError()
                    }

                    Type.FLOAT32, Type.FLOAT64 -> if (!instance.isNumber) {
                        state.pushError()
                    }

                    Type.INT8 -> checkInt(state, instance, -128, 127)
                    Type.UINT8 -> checkInt(state, instance, 0, 255)
                    Type.INT16 -> checkInt(state, instance, -32768, 32767)
                    Type.UINT16 -> checkInt(state, instance, 0, 65535)
                    Type.INT32 -> checkInt(state, instance, -2147483648, 2147483647)
                    Type.UINT32 -> checkInt(state, instance, 0, 4294967295L)
                    Type.STRING -> if (!instance.isTextual) {
                        state.pushError()
                    }

                    Type.TIMESTAMP -> if (!instance.isTextual) {
                        state.pushError()
                    } else {
                        // The instance is a JSON string. Let's verify it's a
                        // well-formatted RFC3339 timestamp.
                        try {
                            DateTimeFormatter.ISO_ZONED_DATE_TIME.parse(instance.asText())
                        } catch (e: DateTimeParseException) {
                            state.pushError()
                        }
                    }
                }
                state.popSchemaToken()
            }

            Form.ENUM -> {
                state.pushSchemaToken("enum")

                if (!instance.isTextual) {
                    state.pushError()
                } else {
                    if (!schema.enum!!.contains(instance.asText())) {
                        state.pushError()
                    }
                }

                state.popSchemaToken()
            }

            Form.ELEMENTS -> {
                state.pushSchemaToken("elements")

                if (!instance.isArray) {
                    state.pushError()
                } else {
                    var index = 0
                    for (subInstance in instance) {
                        state.pushInstanceToken(index.toString())
                        validate(state, schema.elements!!, subInstance, null)
                        state.popInstanceToken()

                        index += 1
                    }
                }

                state.popSchemaToken()
            }

            Form.PROPERTIES -> if (instance.isObject) {
                if (schema.properties != null) {
                    state.pushSchemaToken("properties")
                    for ((key, value) in schema.properties) {
                        state.pushSchemaToken(key)
                        if (instance.asPropertyMap().containsKey(key)) {
                            state.pushInstanceToken(key)
                            validate(state, value, instance.asPropertyMap()[key]!!, null)
                            state.popInstanceToken()
                        } else {
                            state.pushError()
                        }
                        state.popSchemaToken()
                    }
                    state.popSchemaToken()
                }

                if (schema.optionalProperties != null) {
                    state.pushSchemaToken("optionalProperties")
                    for ((key, value) in schema.optionalProperties) {
                        state.pushSchemaToken(key)
                        if (instance.asPropertyMap().containsKey(key)) {
                            state.pushInstanceToken(key)
                            validate(state, value, instance.asPropertyMap()[key]!!, null)
                            state.popInstanceToken()
                        }
                        state.popSchemaToken()
                    }
                    state.popSchemaToken()
                }
                for (key in instance.asPropertyMap().keys) {
                    val inProperties = schema.properties != null && schema.properties.containsKey(key)
                    val inOptionalProperties = (schema.optionalProperties != null
                            && schema.optionalProperties.containsKey(key))
                    val discriminatorTagException = key == parentTag

                    if (!inProperties && !inOptionalProperties && !discriminatorTagException) {
                        state.pushInstanceToken(key)
                        state.pushError()
                        state.popInstanceToken()
                    }
                }
            } else {
                if (schema.properties == null) {
                    state.pushSchemaToken("optionalProperties")
                } else {
                    state.pushSchemaToken("properties")
                }

                state.pushError()
                state.popSchemaToken()
            }

            Form.VALUES -> {
                state.pushSchemaToken("values")
                if (instance.isObject) {
                    for ((key, value) in instance.asPropertyMap().entries) {
                        state.pushInstanceToken(key)
                        validate(state, schema.values!!, value, null)
                        state.popInstanceToken()
                    }
                } else {
                    state.pushError()
                }
                state.popSchemaToken()
            }

            Form.DISCRIMINATOR -> if (instance.isObject) {
                val instanceObj: Map<String, JsonNode> = instance.asPropertyMap()

                if (instanceObj.containsKey(schema.discriminator)) {
                    val instanceTag: JsonNode = instanceObj[schema.discriminator]!!
                    if (instanceTag.isTextual) {
                        val instanceTagString: String = instanceTag.asText()
                        if (schema.mapping!!.containsKey(instanceTagString)) {
                            val subSchema = schema.mapping[instanceTagString]!!

                            state.pushSchemaToken("mapping")
                            state.pushSchemaToken(instanceTagString)
                            validate(state, subSchema, instance, schema.discriminator)
                            state.popSchemaToken()
                            state.popSchemaToken()
                        } else {
                            state.pushSchemaToken("mapping")
                            state.pushInstanceToken(schema.discriminator!!)
                            state.pushError()
                            state.popInstanceToken()
                            state.popSchemaToken()
                        }
                    } else {
                        state.pushSchemaToken("discriminator")
                        state.pushInstanceToken(schema.discriminator!!)
                        state.pushError()
                        state.popInstanceToken()
                        state.popSchemaToken()
                    }
                } else {
                    state.pushSchemaToken("discriminator")
                    state.pushError()
                    state.popSchemaToken()
                }
            } else {
                state.pushSchemaToken("discriminator")
                state.pushError()
                state.popSchemaToken()
            }
        }
    }

    /**
     * Validates an integer.
     *
     * @param state the validation state
     * @param instance the instance to validate
     * @param min the minimum value
     * @param max the maximum value
     */
    private fun checkInt(state: ValidationState, instance: JsonNode, min: Long, max: Long) {
        if (!instance.isNumber) {
            state.pushError()
        } else {
            val `val`: Double = instance.asDouble()
            if (`val` < min || `val` > max || `val` != Math.round(`val`).toDouble()) {
                state.pushError()
            }
        }
    }

    /**
     * Validation state.
     *
     * @property root the root schema
     */
    private class ValidationState(val root: Schema) {
        val errors: MutableList<ValidationError> = mutableListOf()
        val instanceTokens: MutableList<String> = mutableListOf()
        val schemaTokens: MutableList<MutableList<String>> = mutableListOf()
        var maxErrors: Int = 0

        fun pushSchemaToken(token: String) {
            schemaTokens[schemaTokens.size - 1].add(token)
        }

        fun popSchemaToken() {
            val last: MutableList<String> = schemaTokens[schemaTokens.size - 1]
            last.removeAt(last.size - 1)
        }

        fun pushInstanceToken(token: String) {
            instanceTokens.add(token)
        }

        fun popInstanceToken() {
            instanceTokens.removeAt(instanceTokens.size - 1)
        }

        fun pushError() {
            errors.add(
                ValidationError(
                    ArrayList(instanceTokens),
                    ArrayList(schemaTokens[schemaTokens.size - 1])
                )
            )

            if (errors.size == maxErrors) {
                throw MaxErrorsReachedException()
            }
        }
    }
}

/**
 * Dummy error to implement maxDepth. Never returned to the user.
 */
private class MaxErrorsReachedException : Exception()

/**
 * Gets the properties of a [JsonNode] as a map.
 *
 * @return the properties by name
 */
private fun JsonNode.asPropertyMap(): Map<String, JsonNode> {
    return this.properties().associate { it.key to it.value }
}
