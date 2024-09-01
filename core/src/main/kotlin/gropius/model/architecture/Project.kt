package gropius.model.architecture

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.authorization.RELATED_TO_NODE_PERMISSION_RULE
import gropius.model.architecture.layout.Layout
import gropius.model.architecture.layout.RelationLayout
import gropius.model.architecture.layout.RelationPartnerLayout
import gropius.model.architecture.layout.View
import gropius.model.user.permission.NodePermission
import gropius.model.user.permission.NodeWithPermissions
import gropius.model.user.permission.ProjectPermission
import io.github.graphglue.model.*
import java.net.URI

@DomainNode("projects", searchQueryName = "searchProjects")
@GraphQLDescription(
    """A project of the Gropius system.
    Consists of a set of ComponentVersions, which form a graph with the Relations between them.
    Can be affected by issues.
    Can have issues, labels and artefacts as this is a Trackable.
    READ is granted via an associated ProjectPermission.
    """
)
@Authorization(
    ProjectPermission.MANAGE_COMPONENTS,
    allow = [Rule(RELATED_TO_NODE_PERMISSION_RULE, options = [NodePermission.ADMIN])]
)
@Authorization(
    ProjectPermission.MANAGE_VIEWS,
    allow = [Rule(RELATED_TO_NODE_PERMISSION_RULE, options = [NodePermission.ADMIN])]
)
class Project(
    name: String, description: String, repositoryURL: URI?
) : Trackable(name, description, repositoryURL), NodeWithPermissions<ProjectPermission>, Layout {

    companion object {
        const val COMPONENT = "COMPONENT"
        const val RELATION_PARTNER = "RELATION_PARTNER"
        const val RELATION = "RELATION"
        const val VIEW = "VIEW"
        const val DEFAULT_VIEW = "DEFAULT_VIEW"
    }

    @NodeRelationship(COMPONENT, Direction.OUTGOING)
    @GraphQLDescription("The ComponentVersions this consists of.")
    @FilterProperty
    val components by NodeSetProperty<ComponentVersion>()

    @NodeRelationship(RELATION_PARTNER, Direction.OUTGOING)
    @GraphQLDescription("Layouts for relation partners")
    @FilterProperty
    override val relationPartnerLayouts by NodeSetProperty<RelationPartnerLayout>()

    @NodeRelationship(RELATION, Direction.OUTGOING)
    @GraphQLDescription("Layouts for relations")
    @FilterProperty
    override val relationLayouts by NodeSetProperty<RelationLayout>()

    @NodeRelationship(VIEW, Direction.OUTGOING)
    @GraphQLDescription("Views on the architecture graph of this project.")
    @FilterProperty
    val views by NodeSetProperty<View>()

    @NodeRelationship(DEFAULT_VIEW, Direction.OUTGOING)
    @GraphQLDescription("The default view for this project.")
    @FilterProperty
    val defaultView by NodeProperty<View?>()

    @NodeRelationship(NodePermission.NODE, Direction.INCOMING)
    @GraphQLDescription("Permissions for this Project.")
    @FilterProperty
    override val permissions by NodeSetProperty<ProjectPermission>()
}