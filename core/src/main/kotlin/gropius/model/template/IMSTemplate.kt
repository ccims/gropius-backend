package gropius.model.template

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.architecture.IMS
import io.github.graphglue.model.Direction
import io.github.graphglue.model.DomainNode
import io.github.graphglue.model.NodeRelationship

@DomainNode("imsTemplates", searchQueryName = "searchIMSTemplates")
@GraphQLDescription(
    """Template for imss
    Defines templated fields with specific types (defined using JSON schema).
    """
)
class IMSTemplate(
    name: String, description: String, templateFieldSpecifications: MutableMap<String, String>, isDeprecated: Boolean
) : Template<IMS, IMSTemplate>(name, description, templateFieldSpecifications, isDeprecated) {

    @NodeRelationship(SubTemplate.PART_OF, Direction.INCOMING)
    @GraphQLDescription("SubTemplate applied to all IMSProjects with this Template")
    val imsProjectTemplate by NodeProperty<IMSProjectTemplate>()

    @NodeRelationship(SubTemplate.PART_OF, Direction.INCOMING)
    @GraphQLDescription("SubTemplate applied to all IMSIssues with this Template")
    val imsIssueTemplate by NodeProperty<IMSIssueTemplate>()

    @NodeRelationship(SubTemplate.PART_OF, Direction.INCOMING)
    @GraphQLDescription("SubTemplate applied to all IMSUsers with this Template")
    val imsUserTemplate by NodeProperty<IMSUserTemplate>()

}