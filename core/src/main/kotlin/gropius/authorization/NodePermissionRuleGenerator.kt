package gropius.authorization

import gropius.model.user.GropiusUser
import gropius.model.user.permission.BasePermission
import gropius.model.user.permission.NodePermission
import io.github.graphglue.authorization.AllowRuleGenerator
import io.github.graphglue.authorization.Permission
import org.neo4j.cypherdsl.core.Condition
import org.neo4j.cypherdsl.core.Cypher
import org.neo4j.cypherdsl.core.Node
import org.neo4j.cypherdsl.core.Predicates

/**
 * Base class for all [NodePermission] based [AllowRuleGenerator]s
 * Provides a method which is able to check for a specific permission on a [NodePermission] for a specific
 * [GropiusUser]
 */
abstract class NodePermissionRuleGenerator() : AllowRuleGenerator {

    /**
     * Generates the condition for the authorization
     * Is meant to be used in the where part of an existential subquery
     * If any permission in [permissionNames] is present, the condition evaluates to `true`
     *
     * @param nodePermissionNode the CypherDSL node of the [NodePermission] in the match pattern
     * @param permission used to obtain the [GropiusAuthorizationContextBase] to check for the correct user
     * @param permissionNames list of permissions on
     */
    fun generatePredicateCondition(
        nodePermissionNode: Node, permission: Permission, permissionNames: List<String>
    ): Condition {
        val context = permission.context as GropiusAuthorizationContextBase
        val permissionVariable = Cypher.name("p")
        val nodePermissionPredicate = Predicates.any(permissionVariable).`in`(Cypher.anonParameter(permissionNames))
            .where(permissionVariable.`in`(nodePermissionNode.property(BasePermission::entries.name)))

        val gropiusUserPredicate = nodePermissionNode.property(BasePermission::allUsers.name).isTrue.or(
            nodePermissionNode.relationshipFrom(context.userNode)
        )
        return nodePermissionPredicate.and(gropiusUserPredicate)
    }

}