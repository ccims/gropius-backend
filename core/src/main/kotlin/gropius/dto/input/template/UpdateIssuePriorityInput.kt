package gropius.dto.input.template

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.execution.OptionalInput
import gropius.dto.input.common.UpdateNamedNodeInput
import kotlin.properties.Delegates

@GraphQLDescription("Input for the updateIssuePriority mutation")
class UpdateIssuePriorityInput(
    @GraphQLDescription("The value of the IssuePriority, used to compare/order different IssuePriorities")
    val value: OptionalInput<Double>
) : UpdateNamedNodeInput()
