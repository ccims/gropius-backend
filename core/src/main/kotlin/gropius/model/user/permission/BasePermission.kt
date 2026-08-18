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
 * @param entries the granted permission entries as Strings, ignored in GraphQL so that concrete
 *   Permissions can expose them as their own entry enum. Those must use a different Kotlin name
 *   (e.g. [GlobalPermission.graphQLEntries]), as graphql-kotlin drops a property redeclared with
 *   the same name and type as an ignored supertype property.
 * @param allUsers if true, the permission is granted to all users
 */
@DomainNode
abstract class BasePermission(
    name: String,
    description: String,
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