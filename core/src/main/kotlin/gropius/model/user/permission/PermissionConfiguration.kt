package gropius.model.user.permission

import graphql.schema.GraphQLEnumType
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Name of the GraphQL enum providing all non-global permission entries
 */
const val ALL_PERMISSION_ENTRY_NAME = "AllPermissionEntry"

/**
 * Configuration used to provide Permission entry enums
 */
@Configuration
class PermissionConfiguration {

    /**
     * Default [NodePermission] entries
     */
    private val nodePermissionEntries = setOf(
        PermissionEntry(
            NodePermission.READ, """
                Allows to read the Node (obtain it via the API) and to read certain related Nodes.
                See documentation for specific Node for the specific conditions.
            """.trimIndent()
        ), PermissionEntry(
            NodePermission.ADMIN, """
                Grants all other permissions on the Node except READ.
            """.trimIndent()
        )
    )

    /**
     * Default [TrackablePermission] entries
     */
    private val trackablePermissionEntries = nodePermissionEntries + setOf(
        PermissionEntry(
            TrackablePermission.MANAGE_IMS, """
                Allows to add, remove, and update IMSProjects on this Trackable.
                Note: for adding, `IMSPermissionEntry.SYNC_TRACKABLES` is required additionally
            """.trimIndent()
        ), PermissionEntry(
            TrackablePermission.CREATE_ISSUES, """
                Allows to create new Issues on the Trackable.
                This includes adding Issues from other Trackables.
            """.trimIndent()
        ), PermissionEntry(
            TrackablePermission.MODERATOR, """
                Allows to moderate Issues on this Trackable.
                This allows everything `MANAGE_ISSUES` allows.
                Additionally, it allows editing and deleting Comments of other Users
            """.trimIndent()
        ), PermissionEntry(
            TrackablePermission.COMMENT, """
                Allows to create Comments on Issues on this Trackable.
                Also allows editing of your own Comments.
            """.trimIndent()
        ), PermissionEntry(
            TrackablePermission.MANAGE_LABELS, """
                Allows to add, remove, and update Labels on this Trackable.
                Also allows to delete a Label, but only if it is allowed on all Trackable the Label is on.
            """.trimIndent()
        ), PermissionEntry(
            TrackablePermission.MANAGE_ARTEFACTS, """
                Allows to add, remove, and update Artefacts on this Trackable.
            """.trimIndent()
        ), PermissionEntry(
            TrackablePermission.MANAGE_ISSUES, """
                Allows to manage issues.
                This includes `CREATE_ISSUES` and `COMMENT`.
                This does NOT include `LINK_TO_ISSUES` and `LINK_FROM_ISSUES`.
                Additionaly includes
                  - change the Template
                  - add / remove Labels
                  - add / remove Artefacts
                  - change any field on the Issue (title, ...)
                  - change templated fields
                In contrast to `MODERATOR`, this does not allow editing / removing Comments of other users
            """.trimIndent()
        ), PermissionEntry(
            TrackablePermission.EXPORT_ISSUES, """
                Allows adding Issues on this Trackable to other Trackables.
            """.trimIndent()
        ), PermissionEntry(
            TrackablePermission.EXPORT_LABELS, """
                Allows adding Labels on this Trackable to other Trackables.
            """.trimIndent()
        ), PermissionEntry(
            TrackablePermission.AFFECT_ENTITIES_WITH_ISSUES, """
                Allows affecting entities part of this Trackable with any Issues.
                Affectable entitites include
                  - the Trackable itself
                  - in case the Trackable is a Component
                    - InterfaceSpecifications, their InterfaceSpecificationVersions and their InterfaceParts of the Component (not inherited ones)
                    - Interfaces on the Component
                    - ComponentVersions of the Component
            """.trimIndent()
        )
    )

    /**
     * Default [GlobalPermission] entries
     *
     * @return the generated entries
     */
    @Bean
    fun permissionEntries() = PermissionEntryCollection<GlobalPermission>(
        setOf(
            PermissionEntry(
                GlobalPermission.CAN_CREATE_PROJECTS, """
                    Allows to create new Projects.
                """.trimIndent()
            ), PermissionEntry(
                GlobalPermission.CAN_CREATE_COMPONENTS, """
                    Allows to create new Components.
                """.trimIndent()
            ), PermissionEntry(
                GlobalPermission.CAN_CREATE_IMSS, """
                    Allows to create new IMSs.
                """.trimIndent()
            ), PermissionEntry(
                GlobalPermission.CAN_CREATE_TEMPLATES, """
                    Allows to create new Templates.
                """.trimIndent()
            )
        )
    )

    /**
     * Default [IMSPermission] entries
     *
     * @return the generated entries
     */
    @Bean
    fun imsPermissionEntries() = PermissionEntryCollection<IMSPermission>(
        nodePermissionEntries + setOf(
            PermissionEntry(
                IMSPermission.SYNC_TRACKABLES, """
                    Allows to create IMSProjects with this IMS.
                """.trimIndent()
            )
        )
    )

    /**
     * Default [ComponentPermission] entries
     *
     * @return the generated entries
     */
    @Bean
    fun componentPermissionEntries() = PermissionEntryCollection<ComponentPermission>(
        trackablePermissionEntries + setOf(
            PermissionEntry(
                ComponentPermission.RELATE_FROM_COMPONENT, """
                    Allows to create Relations with a version of this Component or an Interface of this Component
                    as start.
                    Note: as these Relations cannot cause new Interfaces on this Component, this can be granted
                    more permissively compared to `RELATE_TO_COMPONENT`.
                """.trimIndent()
            ), PermissionEntry(
                ComponentPermission.ADD_TO_PROJECTS, """
                    Allows to add the Component to Projects
                    Note: this should be handled very carefully, as adding a Component to a Project gives
                    all users with READ access to the Project READ access to the Component
                """.trimIndent()
            )
        )
    )

    /**
     * Default [ProjectPermission] entries
     *
     * @return the generated entries
     */
    @Bean
    fun projectPermissionEntries() = PermissionEntryCollection<ProjectPermission>(
        trackablePermissionEntries + setOf(
            PermissionEntry(
                ProjectPermission.MANAGE_COMPONENTS, """
                    Allows to add / remove ComponentVersions to / from this Project.
                """.trimIndent()
            ),
            PermissionEntry(
                ProjectPermission.MANAGE_VIEWS, """
                    Allows to manage the views of this Project.
                """.trimIndent()
            ),
        )
    )

    /**
     * [GlobalPermission] entry enum generator
     * Generates the enum with name [GLOBAL_PERMISSION_ENTRY_NAME]
     *
     * @param entryCollections all permission entry defining collections
     * @return the generated enum type
     */
    @Bean
    fun permissionEntryType(entryCollections: List<PermissionEntryCollection<GlobalPermission>>): GraphQLEnumType {
        return generatePermissionEntryEnum(
            GLOBAL_PERMISSION_ENTRY_NAME, "Permission entry enum type.", entryCollections
        )
    }

    /**
     * [IMSPermission] entry enum generator
     * Generates the enum with name [IMS_PERMISSION_ENTRY_NAME]
     *
     * @param entryCollections all permission entry defining collections
     * @return the generated enum type
     */
    @Bean
    fun imsPermissionEntryType(entryCollections: List<PermissionEntryCollection<IMSPermission>>): GraphQLEnumType {
        return generatePermissionEntryEnum(
            IMS_PERMISSION_ENTRY_NAME, "IMSPermission entry enum type.", entryCollections
        )
    }

    /**
     * [ComponentPermission] entry enum generator
     * Generates the enum with name [COMPONENT_PERMISSION_ENTRY_NAME]
     *
     * @param entryCollections all permission entry defining collections
     * @return the generated enum type
     */
    @Bean
    fun componentPermissionEntryType(entryCollections: List<PermissionEntryCollection<ComponentPermission>>): GraphQLEnumType {
        return generatePermissionEntryEnum(
            COMPONENT_PERMISSION_ENTRY_NAME, "ComponentPermission entry enum type.", entryCollections
        )
    }

    /**
     * [ProjectPermission] entry enum generator
     * Generates the enum with name [PROJECT_PERMISSION_ENTRY_NAME]
     *
     * @param entryCollections all permission entry defining collections
     * @return the generated enum type
     */
    @Bean
    fun projectPermissionEntryType(entryCollections: List<PermissionEntryCollection<ProjectPermission>>): GraphQLEnumType {
        return generatePermissionEntryEnum(
            PROJECT_PERMISSION_ENTRY_NAME, "ProjectPermission entry enum type.", entryCollections
        )
    }

    /**
     * Entry for all permissions used on nodes
     * Generates the enum with name [ALL_PERMISSION_ENTRY_NAME]
     *
     * @param componentEntries all [ComponentPermission] entry defining collections
     * @param imsEntries all [IMSPermission] entry defining collections
     * @param projectEntries all [ProjectPermission] entry defining collections
     * @return the generated enum type
     */
    @Bean
    fun allPermissionEntryType(
        componentEntries: List<PermissionEntryCollection<ComponentPermission>>,
        imsEntries: List<PermissionEntryCollection<IMSPermission>>,
        projectEntries: List<PermissionEntryCollection<ProjectPermission>>,
    ): GraphQLEnumType {
        return generatePermissionEntryEnum(ALL_PERMISSION_ENTRY_NAME,
            "Non global permission entries",
            listOf(PermissionEntryCollection<BasePermission>((componentEntries + imsEntries + projectEntries).flatMap { it.entries }
                .toSet())))
    }

    /**
     * Generates a [GraphQLEnumType] based on a list of [PermissionEntryCollection]
     *
     * @param name the name of the enum type
     * @param description the description of the enum type
     * @param entryCollections used to build enum entries, must not contain duplicate names
     * @return the generated enum type
     * @throws IllegalArgumentException if duplicate names are found
     */
    private fun generatePermissionEntryEnum(
        name: String, description: String, entryCollections: Collection<PermissionEntryCollection<*>>
    ): GraphQLEnumType {
        val entries = entryCollections.flatMap { it.entries }
        entries.groupBy { it.name }.entries.firstOrNull { it.value.size > 1 }?.also {
            throw IllegalArgumentException("Duplicate name ${it.key} found for entries for $name")
        }
        val builder = GraphQLEnumType.newEnum().name(name).description(description)
        for (entry in entries) {
            builder.value(entry.name, entry.name, entry.description)
        }
        return builder.build()
    }

}