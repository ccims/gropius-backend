package gropius.model.user.permission

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.graphql.TypeGraphQLType
import gropius.model.architecture.Component
import gropius.model.architecture.IMS
import gropius.model.architecture.Project
import gropius.model.template.Template
import io.github.graphglue.model.DomainNode

/**
 * The name of the IMSPermissionEntry GraphQL enum
 */
const val GLOBAL_PERMISSION_ENTRY_NAME = "PermissionEntry"

@DomainNode("globalPermissions", searchQueryName = "searchGlobalPermissions")
@GraphQLDescription(
    """Permission associated with a set of users.
    Can have NodePermissions to grant permissions on specific Nodes.
    READ is granted if the global admin is granted.
    """
)
class GlobalPermission(
    name: String, description: String, entries: MutableList<String>, allUsers: Boolean
) : BasePermission(name, description, entries, allUsers) {

    companion object {
        /**
         * Permission to check if a user can (globally) create [Project]s
         */
        const val CAN_CREATE_PROJECTS = "CAN_CREATE_PROJECTS"

        /**
         * Permission to check if a user can (globally) create [Component]s
         */
        const val CAN_CREATE_COMPONENTS = "CAN_CREATE_COMPONENTS"

        /**
         * Permission to check if a user can (globally) create [IMS]s
         */
        const val CAN_CREATE_IMSS = "CAN_CREATE_IMSS"

        /**
         * Permission to check if a user can (globally) create [Template]s
         */
        const val CAN_CREATE_TEMPLATES = "CAN_CREATE_TEMPLATES"
    }

    @GraphQLDescription(ENTRIES_DESCRIPTION)
    override val entries: MutableList<@TypeGraphQLType(GLOBAL_PERMISSION_ENTRY_NAME) String>
        get() = super.entries
}