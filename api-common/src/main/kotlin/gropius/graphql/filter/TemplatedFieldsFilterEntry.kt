package gropius.graphql.filter

import com.fasterxml.jackson.databind.JsonNode
import gropius.dto.input.common.JSONFieldInput
import gropius.model.template.TemplatedNode
import gropius.util.JsonNodeMapper
import io.github.graphglue.connection.filter.model.FilterEntry
import org.neo4j.cypherdsl.core.Condition
import org.neo4j.cypherdsl.core.Cypher
import org.neo4j.cypherdsl.core.Node
import kotlin.reflect.KProperty

/**
 * Parsed filter entry of a [TemplatedFieldsFilterEntryDefinition]
 *
 * @param value the value provided by the user
 * @param jsonNodeMapper used to serialize [JsonNode]s
 * @param definition [TemplatedFieldsFilterEntryDefinition] used to create this entry
 */
class TemplatedFieldsFilterEntry(
    private val value: List<*>, private val jsonNodeMapper: JsonNodeMapper, definition: TemplatedFieldsFilterEntryDefinition
) : FilterEntry(definition) {

    override fun generateCondition(node: Node): Condition {
        return if (value.isEmpty()) {
            Cypher.isTrue()
        } else {
            value.fold(Cypher.noCondition()) { condition, entry ->
                entry as Map<*, *>
                val templatedFieldProperty: KProperty<*> = TemplatedNode::templatedFields
                val property =
                    node.property("${templatedFieldProperty.name}.${entry[JSONFieldInput::name.name] as String}")
                val jsonNode = entry[JSONFieldInput::value.name] as JsonNode?
                val propertyValue = jsonNodeMapper.jsonNodeToDeterministicString(jsonNode)
                condition.and(property.isEqualTo(Cypher.anonParameter(propertyValue)))
            }
        }
    }

}