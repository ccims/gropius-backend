package gropius.model.user.permission

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.graphql.TypeGraphQLType
import gropius.model.architecture.ComponentVersion
import gropius.model.architecture.Project
import gropius.model.architecture.RelationPartner
import io.github.graphglue.model.DomainNode

/**
 * The name of the IMSPermissionEntry GraphQL enum
 */
const val PROJECT_PERMISSION_ENTRY_NAME = "ProjectPermissionEntry"

@DomainNode(searchQueryName = "searchProjectPermissions")
@GraphQLDescription("NodePermission to grant specific permissions to a set of Projects.")
class ProjectPermission(
    name: String, description: String, entries: MutableList<String>, allUsers: Boolean
) : TrackablePermission<Project>(name, description, entries, allUsers) {

    companion object {
        /**
         * Permission to check if a user is allowed to add / remove [ComponentVersion]s to / from the [Project]
         */
        const val MANAGE_COMPONENTS = "MANAGE_COMPONENTS"

        /**
         * Permission to manage the views of a [Project]
         */
        const val MANAGE_VIEWS = "MANAGE_VIEWS"

        /**
         * Used to track [RelationPartner]s which are part of the graph of a [Project]
         */
        const val PART_OF_PROJECT = "PART_OF_PROJECT"
    }

    @GraphQLDescription(ENTRIES_DESCRIPTION)
    override val entries: MutableList<@TypeGraphQLType(PROJECT_PERMISSION_ENTRY_NAME) String>
        get() = super.entries

}