package gropius.authorization

import gropius.model.user.GropiusUser
import gropius.model.user.permission.NodePermission
import io.github.graphglue.authorization.Permission
import io.github.graphglue.definition.NodeDefinition
import io.github.graphglue.model.Rule
import org.neo4j.cypherdsl.core.Condition
import org.neo4j.cypherdsl.core.Cypher
import org.neo4j.cypherdsl.core.Node

/**
 * Permission rule generator which checks for a Node that is related via a [NodePermission] which
 * provides a specific permission (specified by `permission.name`) to the User.
 * Additional permission names can be provided via the parameters of the [Rule].
 * In this case, it is sufficient if **ANY** of the permissions is granted.
 * Requires a [GropiusAuthorizationContext] as authorization context
 *
 * As an example:
 * If the following relations and nodes exist:
 * ```
 * (c:Component)<-[:NODE]-(cp:ComponentPermission)<-[:PERMISSIONS]-(g:GropiusUser)
 * ```
 * If the id of `g` is provided via the context, and `"READ"` is in the entries of `cp`,
 * the permission `"READ"` is granted on `c`
 *
 * @param nodePermissionDefinition the definition of the [NodePermission] related to the node
 *   on which the permission is checked and the [GropiusUser]
 */
class RelatedToNodePermissionRuleGenerator(
    private val nodePermissionDefinition: NodeDefinition
) : NodePermissionRuleGenerator() {

    override fun generateRule(
        node: Node,
        rule: Rule,
        permission: Permission
    ): Condition {
        val nodePermissionNode = nodePermissionDefinition.node().named("g_2")
        val permissionNames = rule.options.toList() + permission.name
        val subQueryPredicate = generatePredicateCondition(
            nodePermissionNode, permission, permissionNames
        )
        val newRelationship = node.relationshipFrom(nodePermissionNode, NodePermission.NODE)
        return Cypher.match(newRelationship).where(subQueryPredicate).asCondition()
    }

}