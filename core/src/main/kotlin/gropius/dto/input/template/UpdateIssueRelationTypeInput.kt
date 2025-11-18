package gropius.dto.input.template

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.execution.OptionalInput
import gropius.dto.input.common.UpdateNamedNodeInput
import kotlin.properties.Delegates

@GraphQLDescription("Input for the updateIssueRelationType mutation")
class UpdateIssueRelationTypeInput(
    @GraphQLDescription("The name of the relation from the inverse (incoming) perspective")
    val inverseName: OptionalInput<String>
) : UpdateNamedNodeInput()
