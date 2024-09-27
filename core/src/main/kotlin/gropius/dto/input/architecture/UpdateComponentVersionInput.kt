package gropius.dto.input.architecture

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.execution.OptionalInput
import gropius.dto.input.common.JSONFieldInput
import gropius.dto.input.common.UpdateNamedNodeInput
import gropius.dto.input.common.UpdateNodeInput
import gropius.dto.input.common.validateAndEnsureNoDuplicates
import gropius.dto.input.ifPresent
import gropius.dto.input.template.UpdateTemplatedNodeInput

@GraphQLDescription("Input for the updateComponentVersion mutation")
class UpdateComponentVersionInput(
    @GraphQLDescription("New version of the ComponentVersion")
    val version: OptionalInput<String>,
    @GraphQLDescription("New tags of the ComponentVersion")
    val tags: OptionalInput<List<String>>,
    @GraphQLDescription("Values for templatedFields to update")
    override val templatedFields: OptionalInput<List<JSONFieldInput>>,
) : UpdateNodeInput(), UpdateTemplatedNodeInput {

    override fun validate() {
        super.validate()
        templatedFields.ifPresent {
            it.validateAndEnsureNoDuplicates()
        }
    }
}