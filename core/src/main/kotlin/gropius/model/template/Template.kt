package gropius.model.template

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import io.github.graphglue.model.*

@DomainNode
@GraphQLDescription(
    """BaseTemplate with composition features.
    Can have SubTemplates.
    Defines templated fields with specific types (defined using JSON schema).
    """
)
abstract class Template<T, S : Template<T, S>>(
    name: String,
    description: String,
    templateFieldSpecifications: MutableMap<String, String>,
    @property:GraphQLDescription("If true, this template is deprecated and cannot be used for new entities any more.")
    @FilterProperty
    var isDeprecated: Boolean
) : BaseTemplate<T, S>(name, description, templateFieldSpecifications) where T : Node, T : TemplatedNode {

    companion object {
        const val EXTENDS = "EXTENDS"
    }

    @NodeRelationship(EXTENDS, Direction.OUTGOING)
    @GraphQLDescription("Template this template extends.")
    @FilterProperty
    val extends by NodeSetProperty<S>()

    @NodeRelationship(EXTENDS, Direction.INCOMING)
    @GraphQLDescription("Templates that extend this template.")
    @FilterProperty
    val extendedBy by NodeSetProperty<S>()
}