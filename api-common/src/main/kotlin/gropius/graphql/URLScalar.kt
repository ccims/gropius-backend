package gropius.graphql

import graphql.GraphQLContext
import graphql.execution.CoercedVariables
import graphql.language.StringValue
import graphql.language.Value
import graphql.schema.*
import java.net.URI
import java.net.URISyntaxException
import java.util.*

/**
 * Coercing for the [URLScalar]
 */
private val URLCoercing = object : Coercing<URI, String> {

    override fun serialize(input: Any, graphQLContext: GraphQLContext, locale: Locale): String {
        if (input !is URI) {
            throw CoercingSerializeException("Expected URI")
        }
        if (input.scheme.isNullOrEmpty()) {
            throw CoercingSerializeException("URL must specify a scheme.")
        }
        return input.toString()
    }

    override fun parseValue(input: Any, graphQLContext: GraphQLContext, locale: Locale): URI {
        val uri = when (input) {
            is String -> {
                try {
                    URI(input)
                } catch (e: URISyntaxException) {
                    throw CoercingParseValueException(e)
                }
            }

            is URI -> input
            else -> throw CoercingParseValueException("unsupported type")
        }
        if (uri.scheme.isNullOrEmpty()) {
            throw CoercingParseValueException("URL must specify a scheme.")
        }
        return uri
    }

    override fun parseLiteral(
        input: Value<*>,
        variables: CoercedVariables,
        graphQLContext: GraphQLContext,
        locale: Locale
    ): URI {
        if (input !is StringValue) {
            throw CoercingParseLiteralException("Expected AST type 'StringValue'")
        }
        try {
            val uri = URI(input.value)
            if (uri.scheme.isNullOrEmpty()) {
                throw CoercingParseLiteralException("URL must specify a scheme.")
            }
            return uri
        } catch (e: URISyntaxException) {
            throw CoercingParseLiteralException(e)
        }
    }
}

/**
 * A URL scalar backed by a URI which guarantees that a protocol is present
 */
val URLScalar: GraphQLScalarType = GraphQLScalarType.newScalar()
    .name("URL")
    .description("A URL scalar. Must specify a scheme (https, http, ...). E.g. https://example.com")
    .coercing(URLCoercing)
    .build()