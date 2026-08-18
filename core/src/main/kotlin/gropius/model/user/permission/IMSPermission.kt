package gropius.model.user.permission

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLName
import gropius.graphql.TypeGraphQLType
import gropius.model.architecture.IMS
import gropius.model.architecture.IMSProject
import io.github.graphglue.model.DomainNode

/**
 * The name of the IMSPermissionEntry GraphQL enum
 */
const val IMS_PERMISSION_ENTRY_NAME = "IMSPermissionEntry"

@DomainNode(searchQueryName = "searchIMSPermissions")
@GraphQLDescription("NodePermission to grant specific permissions to a set of IMSs.")
class IMSPermission(
    name: String, description: String, entries: MutableList<String>, allUsers: Boolean
) : NodePermission<IMS>(name, description, entries, allUsers) {

    companion object {
        /**
         * Permission to check if a user can create [IMSProject]s with the [IMS]
         */
        const val SYNC_TRACKABLES = "SYNC_TRACKABLES"
    }

    /**
     * [entries], but typed as [IMS_PERMISSION_ENTRY_NAME].
     * Must not be named `entries` itself, see [BasePermission].
     */
    @GraphQLDescription(ENTRIES_DESCRIPTION)
    @GraphQLName("entries")
    val graphQLEntries: List<@TypeGraphQLType(IMS_PERMISSION_ENTRY_NAME) String>
        get() = entries

}