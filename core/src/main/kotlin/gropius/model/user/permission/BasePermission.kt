package gropius.model.user.permission

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import gropius.model.common.NamedNode
import gropius.model.user.GropiusUser
import io.github.graphglue.model.*

/**
 * GraphQL description for permission entries
 */
const val ENTRIES_DESCRIPTION = "All permissions this Permission grants"

/**
 * Base class for all permissions
 *
 * @param entries the granted permission entries as Strings
 * @param allUsers if true, the permission is granted to all users
 */
@DomainNode
abstract class BasePermission(
    name: String,
    description: String,
    // Ignored so that the BasePermission interface does not pin `entries` to [String!]!; every
    // concrete Permission exposes it as its own entry enum instead. Those must declare that field
    // under a different Kotlin name (see e.g. [GlobalPermission.graphQLEntries]): graphql-kotlin
    // drops any property a supertype declares with the same name and type and marks @GraphQLIgnore,
    // and a @TypeGraphQLType type annotation does not make the two KTypes differ.
    @GraphQLIgnore
    @SearchProperty
    open val entries: MutableList<String>,
    @property:GraphQLDescription("If, the permission is granted to all users. Use with caution.")
    @FilterProperty
    @OrderProperty
    var allUsers: Boolean,
) : NamedNode(name, description) {

    @NodeRelationship(GropiusUser.PERMISSION, Direction.INCOMING)
    @GraphQLDescription("GropiusUsers granted this Permission")
    @FilterProperty
    val users by NodeSetProperty<GropiusUser>()

}