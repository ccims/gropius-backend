package gropius.model.architecture

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.common.NamedNode
import gropius.model.issue.Issue.Companion.TYPE
import gropius.model.template.IntraComponentDependencySpecificationType
import gropius.model.user.permission.NodePermission
import io.github.graphglue.model.*

@DomainNode
@GraphQLDescription(
    """Describes a dependency between Interfaces of a Component.
    Both ends can optionally affected InterfaceParts.
    Semantically, any InterfaceSpecification(Version) in `outgoing` depends on any InterfaceSpecification(Version) in 
    `incoming`.
    This can result in a propagation of Issues, if any location in `in` is in some regard affected by an Issue,
    all locations in `out` are affected by this Issue, too.
    """
)
@Authorization(NodePermission.ADMIN, allowFromRelated = ["componentVersion"])
class IntraComponentDependencySpecification(name: String, description: String) : NamedNode(name, description) {

    companion object {
        const val INCOMING_PARTICIPANT = "INCOMING_PARTICIPANT"
        const val OUTGOING_PARTICIPANT = "OUTGOING_PARTICIPANT"
    }

    @NodeRelationship(TYPE, Direction.OUTGOING)
    @GraphQLDescription(
        """The type of the IntraComponentDependencySpecification, e.g. CALLS.
            Allowed IntraComponentDependencySpecificationTypes are defined by the template.
        """
    )
    @FilterProperty
    @OrderProperty
    val type by NodeProperty<IntraComponentDependencySpecificationType?>()

    @NodeRelationship(ComponentVersion.INTRA_COMPONENT_DEPENDENCY_SPECIFICATION, Direction.INCOMING)
    @GraphQLDescription("The ComponentVersion this is part of")
    @FilterProperty
    val componentVersion by NodeProperty<ComponentVersion>()

    @NodeRelationship(INCOMING_PARTICIPANT, Direction.OUTGOING)
    @GraphQLDescription("The incoming Interfaces of this ServiceEffectSpecification.")
    @FilterProperty
    val incomingParticipants by NodeSetProperty<IntraComponentDependencyParticipant>()

    @NodeRelationship(OUTGOING_PARTICIPANT, Direction.OUTGOING)
    @GraphQLDescription("The outgoing Interfaces of this ServiceEffectSpecification.")
    @FilterProperty
    val outgoingParticipants by NodeSetProperty<IntraComponentDependencyParticipant>()

}