package gropius.model.user.permission

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import gropius.model.common.Named
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
@GraphQLIgnore
abstract class BasePermission(
    @property:GraphQLDescription("The name of this entity.")
    @FilterProperty
    @OrderProperty
    override var name: String,
    @property:GraphQLDescription("The description of this entity.")
    @FilterProperty
    override var description: String,
    @GraphQLIgnore
    open val entries: MutableList<String>,
    @property:GraphQLDescription("If, the permission is granted to all users. Use with caution.")
    @property:FilterProperty
    @property:OrderProperty
    var allUsers: Boolean,
) : Node(), Named {

    @NodeRelationship(GropiusUser.PERMISSION, Direction.INCOMING)
    @GraphQLDescription("GropiusUsers granted this Permission")
    @FilterProperty
    val users by NodeSetProperty<GropiusUser>()

}