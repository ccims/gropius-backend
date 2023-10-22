package gropius.model.architecture

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.authorization.RELATED_TO_NODE_PERMISSION_RULE
import gropius.model.user.permission.NodePermission
import gropius.model.user.permission.NodeWithPermissions
import gropius.model.user.permission.ProjectPermission
import io.github.graphglue.model.*
import java.net.URI

@DomainNode("projects")
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
class Project(
    name: String, description: String, repositoryURL: URI?
) : Trackable(name, description, repositoryURL), NodeWithPermissions<ProjectPermission> {

    companion object {
        const val COMPONENT = "COMPONENT"
    }

    @NodeRelationship(COMPONENT, Direction.OUTGOING)
    @GraphQLDescription("The ComponentVersions this consists of.")
    @FilterProperty
    val components by NodeSetProperty<ComponentVersion>()

    @NodeRelationship(NodePermission.NODE, Direction.INCOMING)
    @GraphQLDescription("Permissions for this Project.")
    @FilterProperty
    override val permissions by NodeSetProperty<ProjectPermission>()
}