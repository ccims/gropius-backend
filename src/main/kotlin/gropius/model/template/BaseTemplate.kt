package gropius.model.template

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.expediagroup.graphql.generator.annotations.GraphQLType
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import gropius.model.common.NamedNode
import io.github.graphglue.model.Direction
import io.github.graphglue.model.DomainNode
import io.github.graphglue.model.Node
import io.github.graphglue.model.NodeRelationship
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.annotation.Transient
import org.springframework.data.neo4j.core.schema.CompositeProperty

/**
 * @param templateFieldSpecifications the schema for all templated fields defined by this template
 */
@DomainNode
@GraphQLDescription(
    """Base type for both Template and SubTemplate.
    Defines templated fields with specific types (defined using JSON schema).
    """
)
abstract class BaseTemplate<T : Node, S : BaseTemplate<T, S>>(
    name: String,
    description: String,
    @property:GraphQLIgnore
    @CompositeProperty
    val templateFieldSpecifications: MutableMap<String, String>
) : NamedNode(name, description) {

    companion object {
        const val USED_IN = "USED_IN"
    }

    @NodeRelationship(USED_IN, Direction.OUTGOING)
    @GraphQLDescription("Entities which use this template.")
    @delegate:Transient
    val usedIn by NodeSetProperty<T>()

    @GraphQLDescription("Schema of a template field by name of the template field. Error if the field does not exist.")
    @GraphQLType("JSON")
    fun templateFieldSpecifications(
        @GraphQLDescription("Name of the extension field")
        name: String,
        @Autowired
        @GraphQLIgnore
        objectMapper: ObjectMapper
    ): JsonNode {
        return templateFieldSpecifications[name]?.let { objectMapper.readTree(it) }
            ?: throw IllegalArgumentException("No field specification found for name $name")
    }

    @GraphQLDescription("All template field specifications, if a `namePrefix` is provided, only those matching it")
    fun templateFieldSpecifications(
        @GraphQLDescription("Name of the extension field.")
        namePrefix: String? = null,
        @Autowired
        @GraphQLIgnore
        objectMapper: ObjectMapper
    ): List<JSONField> {
        val fields = if (namePrefix != null) {
            templateFieldSpecifications.filter { it.key.startsWith(namePrefix) }
        } else {
            templateFieldSpecifications
        }
        return fields.entries.map { JSONField(it.key, objectMapper.readTree(it.value)) }
    }

}