package gropius.model.template

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.expediagroup.graphql.generator.annotations.GraphQLType
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import io.github.graphglue.model.AdditionalFilter
import io.github.graphglue.model.property.LazyLoadingDelegate
import io.github.graphglue.model.property.NodePropertyDelegate
import org.springframework.beans.factory.annotation.Autowired

/**
 * Name of the bean defining the templatedFields filter
 */
const val TEMPLATED_FIELDS_FILTER_BEAN = "templatedFieldsFilter"

@GraphQLDescription("Interface for all types which support templates.")
@AdditionalFilter(TEMPLATED_FIELDS_FILTER_BEAN)
interface TemplatedNode {

    /**
     * The values for all templated fields. The schema is defined by the [Template] of this node
     */
    @GraphQLIgnore
    val templatedFields: MutableMap<String, String>

    /**
     * Template associated with the TemplatedNode
     */
    @GraphQLIgnore
    val template: LazyLoadingDelegate<out BaseTemplate<*, *>, out NodePropertyDelegate<out BaseTemplate<*, *>>.NodeProperty>

    @GraphQLDescription("Value of a field defined by the template. Error if such a field is not defined.")
    @GraphQLType("JSON")
    fun templatedField(
        @GraphQLDescription("Name of the extension field")
        name: String,
        @Autowired
        @GraphQLIgnore
        objectMapper: ObjectMapper
    ): JsonNode? {
        return templatedFields[name]?.let { objectMapper.readTree(it) }
            ?: throw IllegalArgumentException("No field found for name $name")
    }

    @GraphQLDescription(
        """All templatedFields
        If `names` is provided, only those matching the name. If `prefixMatching` is true, matching is done by
        prefix, otherwise by full name.
        """
    )
    fun templatedFields(
        @GraphQLDescription("Names of the templated fields. If not provided, all templatedFields.")
        names: List<String>?,
        @GraphQLDescription(
            """If true, name matching is performed as prefix matching, otherwise as absolute match.
            Defaults to absolute matching
            """
        )
        prefixMatching: Boolean?,
        @Autowired
        @GraphQLIgnore
        objectMapper: ObjectMapper
    ): List<JSONField> {
        val fields = if (names != null) {
            templatedFields.filterKeys { key ->
                if (prefixMatching == true) {
                    names.any { key.startsWith(it) }
                } else {
                    key in names
                }
            }
        } else {
            templatedFields
        }
        return fields.entries.map { JSONField(it.key, objectMapper.readTree(it.value)) }
    }
}