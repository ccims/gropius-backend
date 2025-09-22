package gropius.dto.input.misc

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.execution.OptionalInput
import gropius.dto.input.common.UpdateNodeInput
import gropius.dto.input.ifPresent

@GraphQLDescription("Input for the updateLegalInformation mutation")
class UpdateLegalInformationInput(
    @GraphQLDescription("The new label of the LegalInformation")
    val label : OptionalInput<String>,
    @GraphQLDescription("The new text of the LegalInformation")
    val text: OptionalInput<String>,
    @GraphQLDescription("The new priority of the LegalInformation")
    val priority: OptionalInput<Int>
) : UpdateNodeInput() {

    override fun validate() {
        super.validate()
        label.ifPresent {
            if (it.isBlank()) {
                throw IllegalArgumentException("Label must not be blank")
            }
        }
        text.ifPresent {
            if (it.isBlank()) {
                throw IllegalArgumentException("Text must not be blank")
            }
        }
    }

}