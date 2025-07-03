package gropius.dto.input.issue

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.execution.OptionalInput
import com.expediagroup.graphql.generator.scalars.ID
import gropius.dto.input.common.Input
import gropius.dto.input.common.UpdateNodeInput

@GraphQLDescription("Input for the updateIssueBoardItem mutation")
class UpdateIssueBoardItemInput(
    @GraphQLDescription("New position (order) for this board item")
    val position: OptionalInput<Double>
):UpdateNodeInput()