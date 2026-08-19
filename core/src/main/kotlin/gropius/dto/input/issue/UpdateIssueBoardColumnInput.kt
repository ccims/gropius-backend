package gropius.dto.input.issue

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.execution.OptionalInput
import gropius.dto.input.common.UpdateNamedNodeInput

@GraphQLDescription("Input for the updateIssueBoardColumn mutation")
class UpdateIssueBoardColumnInput(
    @GraphQLDescription("New position (order) for this column on its Issue Board")
    val position: OptionalInput<Double>
) : UpdateNamedNodeInput()
