package gropius.model.template

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.architecture.IMSProject
import io.github.graphglue.model.DomainNode

@DomainNode
@GraphQLDescription(
    """SubTemplate for IMSProject.
    Part of a IMSTemplate.
    Defines templated fields with specific types (defined using JSON schema).
    """
)
class IMSProjectTemplate(
    name: String, description: String, templateFieldSpecifications: MutableMap<String, String>
) : SubTemplate<IMSProject, IMSTemplate, IMSProjectTemplate>(
    name, description, templateFieldSpecifications
)