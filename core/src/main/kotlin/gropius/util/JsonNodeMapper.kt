package gropius.util

import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.node.JsonNodeFactory
import org.springframework.stereotype.Component

/**
 * Helper to deterministically map [JsonNode] to [String]
 */
@Component
class JsonNodeMapper {

    /**
     * Used [ObjectMapper]
     */
    private val objectMapper: ObjectMapper =
        JsonMapper.builder().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true).build()

    /**
     * Helper which converts a [JsonNode] to a deterministic [String]
     *
     * @param jsonNode to convert
     * @return the conversion result
     */
    fun jsonNodeToDeterministicString(jsonNode: JsonNode?): String {
        val value = objectMapper.treeToValue(jsonNode ?: JsonNodeFactory.instance.nullNode(), Object::class.java)
        return objectMapper.writeValueAsString(value)
    }

}