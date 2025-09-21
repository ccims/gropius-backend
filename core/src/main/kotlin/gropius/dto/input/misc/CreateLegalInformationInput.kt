package gropius.dto.input.misc

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.dto.input.common.Input

@GraphQLDescription("Input for the createLegalInformation mutation")
class CreateLegalInformationInput(
    @GraphQLDescription("Initial label of the LegalInformation")
    val label : String,
    @GraphQLDescription("Initial text of the LegalInformation")
    val text : String,
    @GraphQLDescription("Initial priority of the LegalInformation")
    val priority : Int
) : Input() {

    override fun validate() {
        super.validate()
        if (label.isBlank()) {
            throw IllegalArgumentException("Label must not be blank")
        }
        if (text.isBlank()) {
            throw IllegalArgumentException("Text must not be blank")
        }
    }

}