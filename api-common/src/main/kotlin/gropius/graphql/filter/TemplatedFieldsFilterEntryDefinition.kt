package gropius.graphql.filter

import com.fasterxml.jackson.databind.JsonNode
import graphql.schema.GraphQLInputType
import graphql.schema.GraphQLList
import graphql.schema.GraphQLTypeReference
import gropius.dto.input.common.JSONFieldInput
import gropius.util.JsonNodeMapper
import io.github.graphglue.authorization.Permission
import io.github.graphglue.connection.filter.definition.FilterEntryDefinition
import io.github.graphglue.connection.filter.model.FilterEntry
import io.github.graphglue.util.CacheMap

/**
 * Filter definition entry for templatedFields
 * Allows to filter by set of key-value-pairs of templated fields
 *
 * @param jsonNodeMapper used to serialize [JsonNode]s
 */
class TemplatedFieldsFilterEntryDefinition(private val jsonNodeMapper: JsonNodeMapper) : FilterEntryDefinition(
    "templatedFields", "Filter for templated fields with matching key and values. Entries are joined by AND"
) {

    override fun parseEntry(value: Any?, permission: Permission?): FilterEntry {
        return TemplatedFieldsFilterEntry(value as List<*>, jsonNodeMapper, this)
    }

    override fun toGraphQLType(inputTypeCache: CacheMap<String, GraphQLInputType>): GraphQLInputType {
        return GraphQLList(GraphQLTypeReference(JSONFieldInput::class.simpleName))
    }

}