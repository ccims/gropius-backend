package gropius.model.common

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.expediagroup.graphql.generator.annotations.GraphQLType
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import gropius.model.template.JSONField
import io.github.graphglue.model.DomainNode
import io.github.graphglue.model.ExtensionField
import io.github.graphglue.model.Node
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.neo4j.core.schema.CompositeProperty

/**
 * Name of the bean which provides the permission extension field
 */
const val PERMISSION_FIELD_BEAN = "permissionFieldBean"

@DomainNode
@GraphQLDescription("Entity which provides dynamic extension fields.")
@ExtensionField(PERMISSION_FIELD_BEAN)
abstract class ExtensibleNode : Node() {

    /**
     * Contains the values of all defined extension fields
     */
    @GraphQLIgnore
    @CompositeProperty
    var extensionFields: MutableMap<String, String> = mutableMapOf()

    @GraphQLDescription("Value of an extension field by name of the extension field. Null if the field does not exist.")
    @GraphQLType("JSON")
    fun extensionField(
        @GraphQLDescription("Name of the extension field")
        name: String,
        @Autowired
        @GraphQLIgnore
        objectMapper: ObjectMapper
    ): JsonNode? {
        return extensionFields[name]?.let { objectMapper.readTree(it) } ?: JsonNodeFactory.instance.nullNode()
    }

    @GraphQLDescription("All extension fields, if a `namePrefix` is provided, only those matching it")
    fun extensionFields(
        @GraphQLDescription("Name of the extension field.")
        namePrefix: String? = null,
        @Autowired
        @GraphQLIgnore
        objectMapper: ObjectMapper
    ): List<JSONField> {
        val fields = if (namePrefix != null) {
            extensionFields.filter { it.key.startsWith(namePrefix) }
        } else {
            extensionFields
        }
        return fields.entries.map { JSONField(it.key, objectMapper.readTree(it.value)) }
    }

}