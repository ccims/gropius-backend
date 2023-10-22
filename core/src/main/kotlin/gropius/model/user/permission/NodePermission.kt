package gropius.model.user.permission

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import gropius.authorization.RELATED_TO_ADMIN_NODE_PERMISSION_RULE
import gropius.authorization.RelatedToAdminNodePermissionRuleGenerator
import gropius.graphql.TypeGraphQLType
import io.github.graphglue.model.*

/**
 * Common base class for [IMSPermission], [ComponentPermission] and [ProjectPermission]
 *
 * @param T the type of Node which is affected by this permission
 * @param E the entry type, should be annotated with [TypeGraphQLType]
 */
@DomainNode
@GraphQLIgnore
@Authorization(NodePermission.READ, allow = [Rule(RELATED_TO_ADMIN_NODE_PERMISSION_RULE, "2")])
@Authorization(NodePermission.RELATED_TO_NODE_PERMISSION, allowFromRelated = ["nodesWithPermission"])
abstract class NodePermission<T : Node>(
    name: String, description: String, entries: MutableList<String>, allUsers: Boolean
) : BasePermission(name, description, entries, allUsers) {

    companion object {
        const val NODE = "NODE"

        /**
         * Permission to check if a user is allowed to read a node
         * This Permission is not implied by any other permission!
         */
        const val READ = "READ"

        /**
         * Permission to check if a user is allowed to administer a node
         * If [ADMIN] is granted, [READ] must be granted, too
         */
        const val ADMIN = "ADMIN"

        /**
         * This should not be checked directly!
         * Helper permission to check if a node is related via this permission to a [NodePermission]
         * Used for [RelatedToAdminNodePermissionRuleGenerator]
         */
        const val RELATED_TO_NODE_PERMISSION = "RELATED_TO_NODE_PERMISSION"
    }

    @NodeRelationship(NODE, Direction.OUTGOING)
    @GraphQLDescription("Nodes on which the Permission is granted.")
    @FilterProperty
    val nodesWithPermission by NodeSetProperty<T>()

}