package gropius.graphql.filter

import gropius.authorization.GropiusAuthorizationContextBase
import io.github.graphglue.authorization.Permission
import io.github.graphglue.connection.filter.model.FilterEntry
import org.neo4j.cypherdsl.core.Condition
import org.neo4j.cypherdsl.core.Cypher
import org.neo4j.cypherdsl.core.Node

/**
 * Parsed filter entry of a [NodePermissionFilterEntryDefinition]
 *
 * @param node the node to check the permission on
 * @param permission the node permission to check
 * @param definition [NodePermissionFilterEntryDefinition] used to create this entry
 */
class NodePermissionFilterEntry(
    private val node: String, private val permission: String, definition: NodePermissionFilterEntryDefinition
) : FilterEntry(definition) {

    override fun generateCondition(node: Node): Condition {
        val definition = definition as NodePermissionFilterEntryDefinition
        val isAdminCondition = node.property("isAdmin").asCondition()
        val nodeDefinition = definition.nodeDefinitionCollection.getNodeDefinition<io.github.graphglue.model.Node>()
        val authorizationContext = object : GropiusAuthorizationContextBase(true) {
            override val userNode: Node
                get() = node
        }
        val hasPermissionCondition = definition.nodeDefinitionCollection.generateAuthorizationCondition(
            nodeDefinition, Permission(permission, authorizationContext)
        ).generateCondition(nodeDefinition.node().withProperties(mapOf("id" to Cypher.anonParameter(this.node))))
        return isAdminCondition.or(hasPermissionCondition)
    }
}