package gropius.model.architecture

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.common.ExtensibleNode
import gropius.model.user.permission.NodePermission
import io.github.graphglue.model.*

@DomainNode
@GraphQLDescription(
    """Participant of a a IntraComponentDependencySpecification
    Consists of an Interface it refers to, and optionally a subset of its active InterfaceParts.
    READ is granted if READ is granted on the associated ComponentVersion
    """
)
@Authorization(NodePermission.READ, allowFromRelated = ["interface", "usedAsIncomingAt", "usedAsOutgoingAt"])
class IntraComponentDependencyParticipant : ExtensibleNode() {

    companion object {
        const val INTERFACE = "INTERFACE"
        const val INCLUDED_PART = "INCLUDING_PART"
    }

    @NodeRelationship(INTERFACE, Direction.OUTGOING)
    @GraphQLDescription("The Interface this IntraComponentDependencyParticipant refers to")
    @FilterProperty
    val `interface` by NodeProperty<Interface>()

    @NodeRelationship(INCLUDED_PART, Direction.OUTGOING)
    @GraphQLDescription(
        """If not empty, the InterfaceParts this IntraComponentDependencyParticipant refers to
        Otherwise, it refers to the referenced `interface` in general.
        Must all be active on `interface`.
        """
    )
    @FilterProperty
    val includedParts by NodeSetProperty<InterfacePart>()

    @NodeRelationship(IntraComponentDependencySpecification.INCOMING_PARTICIPANT, Direction.INCOMING)
    @GraphQLDescription("If this is used as incoming, the IntraComponentDependencySpecification where it is used")
    @FilterProperty
    val usedAsIncomingAt by NodeProperty<IntraComponentDependencySpecification?>()

    @NodeRelationship(IntraComponentDependencySpecification.OUTGOING_PARTICIPANT, Direction.INCOMING)
    @GraphQLDescription("If this is used as outgoing, the IntraComponentDependencySpecification where it is used")
    @FilterProperty
    val usedAsOutgoingAt by NodeProperty<IntraComponentDependencySpecification?>()

}