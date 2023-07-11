package gropius.graphql

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import graphql.GraphQLContext
import graphql.execution.CoercedVariables
import graphql.language.*
import graphql.scalars.`object`.ObjectScalar
import graphql.schema.Coercing
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException
import java.util.*

/**
 * GraphQL Coercing which maps JSON objects from / to [JsonNode]
 *
 * @param objectMapper used for object parsing
 */
class JSONCoercing(private val objectMapper: ObjectMapper) : Coercing<JsonNode, JsonNode> {

    override fun serialize(dataFetcherResult: Any, graphQLContext: GraphQLContext, locale: Locale): JsonNode {
        if (dataFetcherResult !is JsonNode) {
            throw CoercingSerializeException("Only JSONNodes can be serialized")
        }
        return dataFetcherResult
    }

    override fun parseValue(input: Any, graphQLContext: GraphQLContext, locale: Locale): JsonNode {
        try {
            return objectMapper.valueToTree(input)
        } catch (e: Exception) {
            throw CoercingParseValueException(e)
        }
    }

    override fun parseLiteral(
        input: Value<*>,
        variables: CoercedVariables,
        graphQLContext: GraphQLContext,
        locale: Locale
    ): JsonNode {
        return when (input) {
            is NullValue -> JsonNodeFactory.instance.nullNode()
            is FloatValue -> JsonNodeFactory.instance.numberNode(input.value)
            is StringValue -> JsonNodeFactory.instance.textNode(input.value)
            is IntValue -> JsonNodeFactory.instance.numberNode(input.value)
            is BooleanValue -> JsonNodeFactory.instance.booleanNode(input.isValue)
            is EnumValue -> JsonNodeFactory.instance.textNode(input.name)
            is VariableReference -> parseValue(variables[input.name] ?: input.name, graphQLContext, locale)
            is ArrayValue -> JsonNodeFactory.instance.arrayNode().also { arrayNode ->
                arrayNode.addAll(input.values.map { parseLiteral(it, variables, graphQLContext, locale) })
            }

            is ObjectValue -> JsonNodeFactory.instance.objectNode().also { objectNode ->
                objectNode.setAll<JsonNode>(input.objectFields.associate {
                    it.name to if (it.value is NullValue) {
                        JsonNodeFactory.instance.nullNode()
                    } else {
                        parseLiteral(it.value, variables, graphQLContext, locale)
                    }
                })
            }

            else -> throw CoercingParseLiteralException("Cannot handle literal $input")
        }
    }

    override fun valueToLiteral(input: Any, graphQLContext: GraphQLContext, locale: Locale): Value<out Value<*>> {
        return ObjectScalar.INSTANCE.coercing.valueToLiteral(input, graphQLContext, locale)
    }
}