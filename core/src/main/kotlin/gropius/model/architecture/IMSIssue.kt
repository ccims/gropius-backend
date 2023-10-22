package gropius.model.architecture

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import gropius.model.common.ExtensibleNode
import gropius.model.issue.Issue
import gropius.model.template.BaseTemplate
import gropius.model.template.IMSIssueTemplate
import gropius.model.template.TemplatedNode
import gropius.model.user.permission.NodePermission
import io.github.graphglue.model.*
import org.springframework.data.neo4j.core.schema.CompositeProperty

@DomainNode
@GraphQLDescription(
    """Issue on an IMS, represents an Issue synced to an IMS.
    The representation of the Issue on the IMS depends on the type of IMS.
    READ is granted if read is granted on `issue`.
    """
)
@Authorization(NodePermission.READ, allowFromRelated = ["imsProject"])
class IMSIssue(
    @property:GraphQLIgnore
    @CompositeProperty
    override val templatedFields: MutableMap<String, String>
) : ExtensibleNode(), TemplatedNode {

    companion object {
        const val PROJECT = "PROJECT"
        const val ISSUE = "ISSUE"
    }

    @NodeRelationship(BaseTemplate.USED_IN, Direction.INCOMING)
    @GraphQLDescription("The Template of this Component.")
    @FilterProperty
    override val template by NodeProperty<IMSIssueTemplate>()

    @NodeRelationship(PROJECT, Direction.OUTGOING)
    @GraphQLDescription("The IMSProject the issue is synced with.")
    @FilterProperty
    val imsProject by NodeProperty<IMSProject>()

    @NodeRelationship(ISSUE, Direction.OUTGOING)
    @GraphQLDescription("The Issue that is synced by the IMSProject")
    @FilterProperty
    val issue by NodeProperty<Issue>()
}