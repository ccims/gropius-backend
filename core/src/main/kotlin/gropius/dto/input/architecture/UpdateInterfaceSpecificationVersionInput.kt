package gropius.dto.input.architecture

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.execution.OptionalInput
import gropius.dto.input.common.*
import gropius.dto.input.ifPresent
import gropius.dto.input.template.UpdateTemplatedNodeInput

@GraphQLDescription("Input for the updateInterfaceSpecificationVersion mutation")
class UpdateInterfaceSpecificationVersionInput(
    @GraphQLDescription("Values for templatedFields to update")
    override val templatedFields: OptionalInput<List<JSONFieldInput>>,
    @GraphQLDescription("New version of the InterfaceSpecificationVersion")
    val version: OptionalInput<String>,
    @GraphQLDescription("New tags of the InterfaceSpecificationVersion")
    val tags: OptionalInput<List<String>>,
) : UpdateNodeInput(), UpdateTemplatedNodeInput {

    override fun validate() {
        super.validate()
        templatedFields.ifPresent {
            it.validateAndEnsureNoDuplicates()
        }
    }
}