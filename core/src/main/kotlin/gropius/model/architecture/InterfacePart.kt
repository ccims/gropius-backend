package gropius.model.architecture

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import gropius.model.template.BaseTemplate
import gropius.model.template.InterfacePartTemplate
import gropius.model.template.MutableTemplatedNode
import gropius.model.user.permission.NodePermission
import gropius.model.user.permission.TrackablePermission
import io.github.graphglue.model.*
import org.springframework.data.neo4j.core.schema.CompositeProperty

@DomainNode
@GraphQLDescription(
    """Part of an Interface(Specification).
    Its semantics depend on the InterfaceSpecification, e.g. for a REST API interface,
    this could represent a single endpoint of the API.
    Relations can specify for both start and end included InterfaceParts.
    Can be affected by Issues, and be used as start / end of ServiceEffectSpecifications.
    READ is granted if READ is granted on `definedOn`.
    """
)
@Authorization(NodePermission.READ, allowFromRelated = ["partOf"])
@Authorization(NodePermission.ADMIN, allowFromRelated = ["partOf"])
@Authorization(TrackablePermission.AFFECT_ENTITIES_WITH_ISSUES, allowFromRelated = ["partOf"])
@Authorization(TrackablePermission.RELATED_ISSUE_AFFECTED_ENTITY, allowFromRelated = ["partOf"])
class InterfacePart(
    name: String,
    description: String,
    @property:GraphQLIgnore
    @CompositeProperty
    override val templatedFields: MutableMap<String, String>
) : AffectedByIssue(name, description), MutableTemplatedNode {

    @NodeRelationship(BaseTemplate.USED_IN, Direction.INCOMING)
    @GraphQLDescription("The Template of this InterfacePart")
    @FilterProperty
    @OrderProperty
    override val template by NodeProperty<InterfacePartTemplate>()

    @NodeRelationship(Relation.START_PART, Direction.INCOMING)
    @GraphQLDescription("Relations which include this InterfacePart at the start of the Relation")
    @FilterProperty
    val includingOutgoingRelations by NodeSetProperty<Relation>()

    @NodeRelationship(Relation.END_PART, Direction.INCOMING)
    @GraphQLDescription("Relations which include this InterfacePart at the end of the Relation")
    @FilterProperty
    val includingIncomingRelations by NodeSetProperty<Relation>()

    @NodeRelationship(IntraComponentDependencyParticipant.INTERFACE, Direction.INCOMING)
    @GraphQLDescription("Participants of IntraComponentDependencySpecifications where this is used as included part.")
    @FilterProperty
    val includingIntraComponentDependencyParticipants by NodeSetProperty<IntraComponentDependencyParticipant>()

    @NodeRelationship(InterfaceSpecificationVersion.PART, Direction.INCOMING)
    @GraphQLDescription("InterfaceSpecificationVersions where this InterfacePart is active.")
    @FilterProperty
    val partOf by NodeProperty<InterfaceSpecificationVersion>()

}