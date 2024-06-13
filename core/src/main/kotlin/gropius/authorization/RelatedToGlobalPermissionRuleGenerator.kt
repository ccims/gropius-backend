package gropius.authorization

import gropius.model.user.GropiusUser
import gropius.model.user.permission.GlobalPermission
import io.github.graphglue.authorization.AllowRuleGenerator
import io.github.graphglue.authorization.Permission
import io.github.graphglue.definition.NodeDefinition
import io.github.graphglue.model.Rule
import org.neo4j.cypherdsl.core.Condition
import org.neo4j.cypherdsl.core.Cypher
import org.neo4j.cypherdsl.core.Node

/**
 * Permission to check that a [GropiusUser] is connected to a [GlobalPermission] with the specified [Permission]
 * Assumes that node is a GropiusUser node
 * Caution: does use the node, not the gropiusUser from the authorization context!
 *
 * @param nodePermissionDefinition definition of [GlobalPermission]
 */
class RelatedToGlobalPermissionRuleGenerator(
    private val nodePermissionDefinition: NodeDefinition
) : AllowRuleGenerator {
    override fun generateRule(
        node: Node, rule: Rule, permission: Permission
    ): Condition {
        val nodePermissionNode = nodePermissionDefinition.node().named("g_2")

        val nodePermissionPredicate =
            Cypher.anonParameter(permission.name).`in`(nodePermissionNode.property(GlobalPermission::entries.name))
        val userSpecificPermissionCondition = Cypher.match(
            node.relationshipTo(nodePermissionNode, GropiusUser.PERMISSION)
        ).where(nodePermissionPredicate).asCondition()

        val allUsersPredicate = nodePermissionNode.property(GlobalPermission::allUsers.name).isTrue
        val allUsersPermissionCondition = Cypher.match(
            nodePermissionNode
        ).where(nodePermissionPredicate.and(allUsersPredicate)).asCondition()

        return allUsersPermissionCondition.or(userSpecificPermissionCondition)
    }
}