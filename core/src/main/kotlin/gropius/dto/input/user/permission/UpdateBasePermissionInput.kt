package gropius.dto.input.user.permission

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.execution.OptionalInput
import com.expediagroup.graphql.generator.scalars.ID
import gropius.dto.input.common.UpdateNamedNodeInput
import gropius.dto.input.ensureDisjoint
import gropius.dto.input.ifPresent
import gropius.model.user.permission.BasePermission
import kotlin.properties.Delegates

/**
 * Fragment for update mutation inputs for classes extending [BasePermission]
 */
abstract class UpdateBasePermissionInput : UpdateNamedNodeInput() {

    @GraphQLDescription("The new value for allUsers")
    var allUsers: OptionalInput<Boolean> by Delegates.notNull()

    @GraphQLDescription("Ids of affected users to add")
    var addedUsers: OptionalInput<List<ID>> by Delegates.notNull()

    @GraphQLDescription("Ids of affected users to remove")
    var removedUsers: OptionalInput<List<ID>> by Delegates.notNull()

    /**
     * Entries to add, must be disjoint with [removedEntries]
     */
    abstract val addedEntries: OptionalInput<List<String>>

    /**
     * Entries to remove, must be disjoint with [addedEntries]
     */
    abstract val removedEntries: OptionalInput<List<String>>

    override fun validate() {
        super.validate()
        name.ifPresent {
            if (it.isBlank()) {
                throw IllegalArgumentException("If name is defined, it must not be blank")
            }
        }
        ::addedEntries ensureDisjoint ::removedEntries
        ::addedUsers ensureDisjoint ::removedUsers
    }

}