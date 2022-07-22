package gropius.service.common

import gropius.dto.input.common.CreateExtensibleNodeInput
import gropius.dto.input.common.UpdateExtensibleNodeInput
import gropius.model.common.AuditedNode
import gropius.model.user.User
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository
import java.time.OffsetDateTime

/**
 * Base class for services for subclasses of [AuditedNode]
 *
 * @param repository the associated repository used for CRUD functionality
 * @param T the type of Node this service is used for
 * @param R Repository type associated with [T]
 */
abstract class AuditedNodeService<T : AuditedNode, R : ReactiveNeo4jRepository<T, String>>(repository: R) :
    AbstractExtensibleNodeService<T, R>(repository) {

    /**
     * Updates [node] based on [input]
     * Calls [updateExtensibleNode]
     * Sets [AuditedNode.lastModifiedBy] and [AuditedNode.lastModifiedAt]
     *
     * @param node the node to update
     * @param input defines how to update the provided [node]
     * @param lastModifiedBy the user who last modified the node
     * @param lastModifiedAt the time when the node was last modified, defaults to `now()`
     */
    suspend fun updateAuditedNode(
        node: AuditedNode,
        input: UpdateExtensibleNodeInput,
        lastModifiedBy: User,
        lastModifiedAt: OffsetDateTime = OffsetDateTime.now()
    ) {
        updateExtensibleNode(node, input)
        node.lastModifiedBy().value = lastModifiedBy
        node.lastModifiedAt = lastModifiedAt
    }

    /**
     * Updates [node] based on [input]
     * Should be called after the node was constructed
     * Calls [createdExtensibleNode]
     * Sets [AuditedNode.createdBy] and [AuditedNode.lastModifiedBy]
     *
     * @param node the node to update
     * @param input defines how to update the provided [node]
     * @param createdBy the user who created the node
     */
    suspend fun createdAuditedNode(node: AuditedNode, input: CreateExtensibleNodeInput, createdBy: User) {
        createdExtensibleNode(node, input)
        node.createdBy().value = createdBy
        node.lastModifiedBy().value = createdBy
    }

}