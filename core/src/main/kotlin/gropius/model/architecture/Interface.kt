package gropius.model.architecture

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import gropius.model.template.RelationPartnerTemplate
import gropius.model.user.permission.ComponentPermission
import gropius.model.user.permission.NodePermission
import gropius.model.user.permission.ProjectPermission
import gropius.model.user.permission.TrackablePermission
import io.github.graphglue.model.*
import io.github.graphglue.model.property.NodeCache

@DomainNode
@GraphQLDescription(
    """An interface which is part of a specific ComponentVersion.
    Its semantics depend on the InterfaceSpecification it is specified by, e.g. an Interface can represent a REST API.
    Can be used in Relations and affected by Issues.
    READ is granted if READ is granted on `interfaceDefinition`.
    """
)
@Authorization(NodePermission.READ, allowFromRelated = ["interfaceDefinition"])
@Authorization(NodePermission.ADMIN, allowFromRelated = ["interfaceDefinition"])
@Authorization(ComponentPermission.RELATE_FROM_COMPONENT, allowFromRelated = ["interfaceDefinition"])
@Authorization(TrackablePermission.AFFECT_ENTITIES_WITH_ISSUES, allowFromRelated = ["interfaceDefinition"])
@Authorization(TrackablePermission.RELATED_ISSUE_AFFECTED_ENTITY, allowFromRelated = ["interfaceDefinition"])
@Authorization(ProjectPermission.PART_OF_PROJECT, allowFromRelated = ["interfaceDefinition"])
class Interface : RelationPartner() {

    companion object {
        const val DEFINITION = "DEFINITION"
    }

    @NodeRelationship(DEFINITION, Direction.OUTGOING)
    @GraphQLDescription("The definition of this interface.")
    @FilterProperty
    val interfaceDefinition by NodeProperty<InterfaceDefinition>()

    @NodeRelationship(IntraComponentDependencyParticipant.INCLUDED_PART, Direction.INCOMING)
    @GraphQLDescription("Participants of IntraComponentDependencySpecifications where this is used.")
    @FilterProperty
    val intraComponentDependencyParticipants by NodeSetProperty<IntraComponentDependencyParticipant>()

    @GraphQLIgnore
    override suspend fun relationPartnerTemplate(cache: NodeCache?): RelationPartnerTemplate<*, *> {
        val interfaceSpecificationVersion = interfaceDefinition(cache).value.interfaceSpecificationVersion(cache).value
        return interfaceSpecificationVersion.interfaceSpecification(cache).value.template(cache).value
    }

}