package gropius.model.template

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.architecture.IMSIssue
import io.github.graphglue.model.DomainNode

@DomainNode
@GraphQLDescription(
    """SubTemplate for IMSIssue.
    Part of a IMSTemplate.
    Defines templated fields with specific types (defined using JSON schema).
    """
)
class IMSIssueTemplate(
    name: String, description: String, templateFieldSpecifications: MutableMap<String, String>
) : SubTemplate<IMSIssue, IMSTemplate, IMSIssueTemplate>(
    name, description, templateFieldSpecifications
)