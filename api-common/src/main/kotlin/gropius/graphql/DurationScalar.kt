package gropius.graphql

import graphql.GraphQLContext
import graphql.execution.CoercedVariables
import graphql.language.StringValue
import graphql.language.Value
import graphql.schema.*
import java.time.Duration
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import java.util.*

/**
 * Coercing for [DurationScalar]
 */
private val DurationCoercing = object : Coercing<Duration, String> {
    override fun serialize(input: Any, graphQLContext: GraphQLContext, locale: Locale): String {
        if (input !is OffsetDateTime) {
            throw CoercingSerializeException("Expected Duration")
        }
        return input.toString()
    }

    override fun parseValue(input: Any, graphQLContext: GraphQLContext, locale: Locale): Duration {
        return when(input) {
            is String -> {
                try {
                    Duration.parse(input)
                } catch (e: DateTimeParseException) {
                    throw CoercingParseValueException(e)
                }
            }
            is Duration -> input
            else -> throw CoercingParseValueException("Expected Duration or String")
        }
    }

    override fun parseLiteral(
        input: Value<*>,
        variables: CoercedVariables,
        graphQLContext: GraphQLContext,
        locale: Locale
    ): Duration {
        if (input !is StringValue) {
            throw CoercingParseLiteralException("Expected AST type 'StringValue'")
        }
        try {
            return Duration.parse(input.value)
        } catch (e: DateTimeParseException) {
            throw CoercingParseLiteralException("Invalid Duration format", e)
        }
    }
}

/**
 * Scalar for an ISO 8601 Duration String
 */
val DurationScalar: GraphQLScalarType = GraphQLScalarType.newScalar()
    .name("Duration")
    .description("An ISO 8601 duration string, e.g. P2Y7M4DT5H42M")
    .coercing(DurationCoercing)
    .build()