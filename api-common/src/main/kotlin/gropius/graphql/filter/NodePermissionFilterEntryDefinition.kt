package gropius.graphql.filter

import graphql.Scalars
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLInputType
import graphql.schema.GraphQLNonNull
import graphql.schema.GraphQLTypeReference
import gropius.model.user.permission.ALL_PERMISSION_ENTRY_NAME
import io.github.graphglue.authorization.Permission
import io.github.graphglue.connection.filter.definition.FilterEntryDefinition
import io.github.graphglue.connection.filter.model.FilterEntry
import io.github.graphglue.definition.NodeDefinitionCollection
import io.github.graphglue.util.CacheMap

/**
 * Filter definition entry for users with a specific node permission
 * Takes the id of the node and the permission to check
 *
 * @param nodeDefinitionCollection the [NodeDefinitionCollection] to use for authorization
 */
class NodePermissionFilterEntryDefinition(
    val nodeDefinitionCollection: NodeDefinitionCollection
) : FilterEntryDefinition(
    "hasNodePermission", "Filter for users with a specific permission on a node"
) {

    override fun parseEntry(value: Any?, permission: Permission?): FilterEntry {
        value as Map<*, *>
        val node = value["node"] as String
        val filterPermission = value["permission"] as String
        return NodePermissionFilterEntry(node, filterPermission, this)
    }

    override fun toGraphQLType(inputTypeCache: CacheMap<String, GraphQLInputType>): GraphQLInputType {
        val name = "NodePermissionFilterEntry"
        return inputTypeCache.computeIfAbsent(name, GraphQLTypeReference(name)) {
            GraphQLInputObjectType.newInputObject().name(name).field {
                it.name("node").description("The node where the user must have the permission")
                    .type(GraphQLNonNull(Scalars.GraphQLID))
            }.field {
                it.name("permission").description("The permission the user must have on the node")
                    .type(GraphQLNonNull(GraphQLTypeReference(ALL_PERMISSION_ENTRY_NAME)))
            }.build()
        }
    }
}