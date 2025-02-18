package gropius.dto.input.architecture

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.execution.OptionalInput
import com.expediagroup.graphql.generator.scalars.ID
import gropius.dto.input.common.UpdateNamedNodeInput

@GraphQLDescription("Input for the updateIntraComponentDependencySpecification mutation")
class UpdateIntraComponentDependencySpecificationInput(
    @GraphQLDescription("If provided, the new type of the IntraComponentDependencySpecification")
    val type: OptionalInput<ID?>,
    @GraphQLDescription("Added incomingParticipants")
    val addedIncomingParticipants: OptionalInput<List<IntraComponentDependencyParticipantInput>>,
    @GraphQLDescription("Added outgoingParticipants")
    val addedOutgoingParticipants: OptionalInput<List<IntraComponentDependencyParticipantInput>>,
    @GraphQLDescription("Ids of incomingParticipants to remove / delete")
    val removedIncomingParticipants: OptionalInput<List<ID>>,
    @GraphQLDescription("Ids of outgoingParticipants to remove / delete")
    val removedOutgoingParticipants: OptionalInput<List<ID>>
) : UpdateNamedNodeInput()