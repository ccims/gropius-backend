package gropius.model.architecture

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.user.permission.NodePermission
import gropius.model.user.permission.ProjectPermission
import io.github.graphglue.model.Direction
import io.github.graphglue.model.DomainNode
import io.github.graphglue.model.FilterProperty
import io.github.graphglue.model.NodeRelationship
import org.springframework.data.annotation.Transient
import java.net.URI

@DomainNode("projects")
@GraphQLDescription(
    """A project of the Gropius system.
    Consists of a set of ComponentVersions, which form a graph with the Relations between them.
    Can be affected by issues.
    Can have issues, labels and artefacts as this is a Trackable.
    """
)
class Project(name: String, description: String, repositoryURL: URI) : Trackable(name, description, repositoryURL) {

    companion object {
        const val COMPONENT = "COMPONENT"
    }

    @NodeRelationship(COMPONENT, Direction.OUTGOING)
    @GraphQLDescription("The ComponentVersions this consists of.")
    @FilterProperty
    @delegate:Transient
    val components by NodeSetProperty<ComponentVersion>()

    @NodeRelationship(NodePermission.NODE, Direction.INCOMING)
    @GraphQLDescription("Permissions for this Project.")
    @FilterProperty
    @delegate:Transient
    val permissions by NodeSetProperty<ProjectPermission>()
}