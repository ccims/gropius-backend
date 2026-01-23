package gropius.model.template

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.issue.Artefact
import io.github.graphglue.model.DomainNode

@DomainNode("artefactTemplates")
@GraphQLDescription(
    """Template for Artefacts
    Defines templated fields with specific types (defined using JSON schema).
    """
)
class ArtefactTemplate(
    name: String, description: String, templateFieldSpecifications: MutableMap<String, String>, isDeprecated: Boolean, isAbstract: Boolean
) : Template<Artefact, ArtefactTemplate>(name, description, templateFieldSpecifications, isDeprecated, isAbstract)