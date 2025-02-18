package gropius.model.template

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.common.NamedNode
import gropius.model.issue.Issue
import gropius.model.user.permission.NodePermission
import io.github.graphglue.model.Authorization
import io.github.graphglue.model.Direction
import io.github.graphglue.model.DomainNode
import io.github.graphglue.model.FilterProperty
import io.github.graphglue.model.NodeRelationship

@DomainNode(searchQueryName = "searchIntraComponentDependencySpecificationTypes")
@GraphQLDescription(
    """Type of an IntraComponentDependencySpecification like CALLS.
    Part of a ComponentTemplate.
    READ is always granted.
    """
)
@Authorization(NodePermission.READ, allowAll = true)
class IntraComponentDependencySpecificationType(
    name: String,
    description: String
) : NamedNode(name, description) {
    companion object {
        const val PART_OF = "PART_OF"
    }

    @NodeRelationship(Issue.TYPE, Direction.INCOMING)
    @GraphQLDescription("IntraComponentDependencySpecifications with this type.")
    @FilterProperty
    val intraComponentDependencySpecificationsWithType by NodeSetProperty<Issue>()

    @NodeRelationship(IntraComponentDependencySpecificationType.PART_OF, Direction.OUTGOING)
    @GraphQLDescription("ComponentTemplates this is a part of.")
    @FilterProperty
    val partOf by NodeSetProperty<ComponentTemplate>()

}