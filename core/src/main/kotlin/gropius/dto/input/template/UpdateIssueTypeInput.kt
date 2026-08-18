package gropius.dto.input.template

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.execution.OptionalInput
import gropius.dto.input.common.UpdateNamedNodeInput
import kotlin.properties.Delegates

@GraphQLDescription("Input for the updateIssueType mutation")
class UpdateIssueTypeInput(
    @GraphQLDescription("A path that is used as the icon for issues")
    val iconPath: OptionalInput<String>
) : UpdateNamedNodeInput()
