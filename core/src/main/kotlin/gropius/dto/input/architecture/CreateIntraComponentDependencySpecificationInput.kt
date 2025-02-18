package gropius.dto.input.architecture

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.scalars.ID
import gropius.dto.input.common.CreateNamedNodeInput

@GraphQLDescription("Input for the createIntraComponentDependencySpecification mutation")
class CreateIntraComponentDependencySpecificationInput(
    @GraphQLDescription("The id of the type of the created IntraComponentDependencySpecification, must be compatible with the template of the Component")
    val type: ID?,
    @GraphQLDescription("The id of the ComponentVersion the created IntraComponentDependencySpecification is part of")
    val componentVersion: ID,
    @GraphQLDescription("Initial incomingParticipants, must not be empty")
    val incomingParticipants: List<IntraComponentDependencyParticipantInput>,
    @GraphQLDescription("Initial outgoingParticipants, must not be empty")
    val outgoingParticipants: List<IntraComponentDependencyParticipantInput>
) : CreateNamedNodeInput() {

    override fun validate() {
        super.validate()
        if (incomingParticipants.isEmpty()) {
            throw IllegalArgumentException("incomingParticipants must not be empty")
        }
        if (outgoingParticipants.isEmpty()) {
            throw IllegalArgumentException("outgoingParticipants must not be empty")
        }
    }
}