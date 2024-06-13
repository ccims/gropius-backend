package gropius.graphql.filter

import graphql.Scalars
import graphql.schema.GraphQLInputType
import gropius.model.architecture.Trackable
import io.github.graphglue.authorization.Permission
import io.github.graphglue.connection.filter.definition.FilterEntryDefinition
import io.github.graphglue.connection.filter.model.FilterEntry
import io.github.graphglue.data.execution.CypherConditionGenerator
import io.github.graphglue.definition.NodeDefinitionCollection
import io.github.graphglue.util.CacheMap

/**
 * Filter definition entry for affected by issue related to a Trackable.
 * Takes the id of the Trackable to which the entities must be related.
 *
 * @param nodeDefinitionCollection the [NodeDefinitionCollection] to use for authorization
 */
class AffectedByIssueRelatedToFilterEntryDefinition(
    private val nodeDefinitionCollection: NodeDefinitionCollection,
) : FilterEntryDefinition("relatedTo", "Filters for AffectedByIssues which are related to a Trackable") {

    /**
     * Provides a condition generator used to filter for Trackables which the Permissions allows to access
     *
     * @param permission the current read permission, used to only consider nodes in filters which match the permission
     * @return the generated condition generator
     */
    fun generateAuthorizationCondition(permission: Permission): CypherConditionGenerator {
        return nodeDefinitionCollection.generateAuthorizationCondition(
            nodeDefinitionCollection.getNodeDefinition<Trackable>(),
            permission
        )
    }

    override fun parseEntry(value: Any?, permission: Permission?): FilterEntry {
        return AffectedByIssueRelatedToFilterEntry(
            value!! as String,
            this,
            permission
        )
    }

    override fun toGraphQLType(inputTypeCache: CacheMap<String, GraphQLInputType>): GraphQLInputType = Scalars.GraphQLID
}