package gropius.dto.input.user.permission

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.scalars.ID
import gropius.dto.input.common.Input
import gropius.model.user.permission.BasePermission
import kotlin.properties.Delegates

/**
 * Fragment for create mutation inputs for classes extending [BasePermission]
 */
abstract class CreateBasePermissionInput : Input() {

    @GraphQLDescription("The name of the BasePermission, must not be blank")
    var name: String by Delegates.notNull()

    @GraphQLDescription("The description of the BasePermission")
    var description: String by Delegates.notNull()

    @GraphQLDescription("If `true`, the created BasePermission affects all users")
    var allUsers: Boolean by Delegates.notNull()

    @GraphQLDescription("Ids of GropiusUsers this BasePermission affects")
    var users: List<ID> by Delegates.notNull()

    /**
     * Entries granted to the users
     */
    abstract val entries: List<String>

    override fun validate() {
        super.validate()
        if (name.isBlank()) {
            throw IllegalArgumentException("Name must not be blank")
        }
    }

}