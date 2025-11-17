package gropius.model.template

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.common.NamedNode
import gropius.model.issue.timeline.IssueRelation
import gropius.model.user.permission.NodePermission
import io.github.graphglue.model.*

@DomainNode(searchQueryName = "searchIssueRelationTypes")
@GraphQLDescription(
    """Type for an IssueRelation, like DUPLICATES or DEPENDS_ON. Part of an IssueTemplate.
    READ is always granted.
    """
)
@Authorization(NodePermission.READ, allowAll = true)
class IssueRelationType(
    name: String,
    description: String,
    @property:GraphQLDescription("The name of the relation from the inverse (incoming) perspective")
    @FilterProperty
    @OrderProperty
    var inverseName: String
) : NamedNode(name, description) {

    companion object {
        const val PART_OF = "PART_OF"
    }

    @NodeRelationship(IssueRelation.TYPE, Direction.INCOMING)
    @GraphQLDescription("Relations which use this type.")
    @FilterProperty
    val relationsWithType by NodeSetProperty<IssueRelation>()

    @NodeRelationship(PART_OF, Direction.OUTGOING)
    @GraphQLDescription("IssueTemplates this is part of.")
    @FilterProperty
    val partOf by NodeSetProperty<IssueTemplate>()

}