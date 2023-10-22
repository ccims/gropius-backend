package gropius.service.common

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import gropius.dto.input.common.CreateExtensibleNodeInput
import gropius.dto.input.common.JSONFieldInput
import gropius.dto.input.common.UpdateExtensibleNodeInput
import gropius.dto.input.orElse
import gropius.model.common.ExtensibleNode
import gropius.repository.GropiusRepository
import gropius.util.JsonNodeMapper
import org.springframework.beans.factory.annotation.Autowired

/**
 * Base class for services for subclasses of [ExtensibleNode]
 *
 * @param repository the associated repository used for CRUD functionality
 * @param T the type of Node this service is used for
 * @param R Repository type associated with [T]
 */
abstract class AbstractExtensibleNodeService<T : ExtensibleNode, R : GropiusRepository<T, String>>(
    repository: R
) : NodeService<T, R>(repository) {
    /**
     * Injected [ObjectMapper], used to parse [JSONFieldInput]
     */
    @Autowired
    lateinit var objectMapper: ObjectMapper

    /**
     * Injected [JsonNodeMapper]
     */
    @Autowired
    private lateinit var jsonNodeMapper: JsonNodeMapper

    /**
     * Updates [node] based on [input]
     * Sets extension fields
     *
     * @param node the node to update
     * @param input defines how to update the provided [node]
     */
    fun updateExtensibleNode(node: ExtensibleNode, input: UpdateExtensibleNodeInput) {
        updateExtensionFields(node, input.extensionFields.orElse(emptyList()))
    }

    /**
     * Updates [node] based on [input]
     * Should be called after the node was constructed
     *
     * @param node the node to update
     * @param input defines how to update the provided [node]
     */
    fun createdExtensibleNode(node: ExtensibleNode, input: CreateExtensibleNodeInput) {
        updateExtensionFields(node, input.extensionFields.orElse(emptyList()))
    }

    /**
     * Updates the extension fields of the provided [node] based on [fields]
     *
     * @param node the node to update
     * @param fields extension fields to set
     */
    private fun updateExtensionFields(node: ExtensibleNode, fields: List<JSONFieldInput>) {
        for (field in fields) {
            node.extensionFields[field.name] = jsonNodeMapper.jsonNodeToDeterministicString(field.value as JsonNode)
        }
    }
}