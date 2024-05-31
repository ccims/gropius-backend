package gropius.dto.input.architecture

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.dto.input.common.Input

@GraphQLDescription("Input for the bulkCreateComponent mutation")
class BulkCreateComponentInput(
    @GraphQLDescription("The input for the createComponent mutation")
    val components: List<CreateComponentInput>
) : Input() {
    override fun validate() {
        components.forEach(Input::validate)
    }
}