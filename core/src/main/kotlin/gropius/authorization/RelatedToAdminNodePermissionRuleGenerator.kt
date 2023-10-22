package gropius.authorization

import gropius.model.architecture.IMSProject
import gropius.model.user.permission.NodePermission
import io.github.graphglue.authorization.Permission
import io.github.graphglue.definition.NodeDefinition
import io.github.graphglue.model.Rule
import org.neo4j.cypherdsl.core.Condition
import org.neo4j.cypherdsl.core.Cypher
import org.neo4j.cypherdsl.core.Node
import org.neo4j.cypherdsl.core.RelationshipPattern

/**
 * Permission rule generator used to check if a node is connected via [NodePermission.RELATED_TO_NODE_PERMISSION]
 * to an [NodePermission] with [NodePermission.ADMIN]
 * The Permission must be declared with a parameter containing the max amount of relationships
 * to traverse until the [NodePermission]
 *
 * Used e.g. to check for [NodePermission.READ] on [NodePermission] and [IMSProject]
 *
 * @param nodePermissionDefinition used to generate Cypher DSL node for [NodePermission]
 */
class RelatedToAdminNodePermissionRuleGenerator(
    private val nodePermissionDefinition: NodeDefinition
) : NodePermissionRuleGenerator() {

    override fun generateRule(
        node: Node, currentRelationship: RelationshipPattern, rule: Rule, permission: Permission
    ): Pair<RelationshipPattern, Condition> {
        val relatedNodePermissionNode = nodePermissionDefinition.node().named("g_2")
        val subQueryPredicate = generatePredicateCondition(
            relatedNodePermissionNode, permission, listOf(NodePermission.ADMIN)
        )
        val maxLength = rule.options.first().toInt()
        val newRelationship = currentRelationship.relationshipBetween(relatedNodePermissionNode)
            .properties(NodePermission.RELATED_TO_NODE_PERMISSION, Cypher.literalTrue()).length(0, maxLength)
        return newRelationship to subQueryPredicate
    }

}