package gropius.model.architecture

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.user.permission.NodePermission
import io.github.graphglue.model.*
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import gropius.model.template.BaseTemplate
import gropius.model.template.InterfaceSpecificationTemplate
import gropius.model.template.MutableTemplatedNode
import org.springframework.data.annotation.Transient
import org.springframework.data.neo4j.core.schema.CompositeProperty

@DomainNode
@GraphQLDescription(
    """Specification of an Interface.
    Defined on a Component, but can be visible and invisible on different ComponentVersions.
    Can be affected by Issues, and be used as start / end of ServiceEffectSpecifications.
    Defines InterfaceParts, but active parts depend on the InterfaceSpecificationVersion.
    READ is granted if READ is granted on `component`, or any InterfaceSpecificationVersion in `versions`.
    """
)
@Authorization(NodePermission.READ, allowFromRelated = ["component", "versions"])
@Authorization(NodePermission.ADMIN, allowFromRelated = ["component"])
class InterfaceSpecification(
    name: String,
    description: String,
    @property:GraphQLIgnore
    @CompositeProperty
    override val templatedFields: MutableMap<String, String>
) : AffectedByIssue(
    name, description
), MutableTemplatedNode {

    companion object {
        const val VERSION = "VERSION"
        const val COMPONENT = "COMPONENT"
    }

    @NodeRelationship(BaseTemplate.USED_IN, Direction.INCOMING)
    @GraphQLDescription("The Template of this InterfaceSpecification.")
    @FilterProperty
    @delegate:Transient
    override val template by NodeProperty<InterfaceSpecificationTemplate>()

    @NodeRelationship(VERSION, Direction.OUTGOING)
    @GraphQLDescription("Versions of this InterfaceSpecification.")
    @FilterProperty
    @delegate:Transient
    val versions by NodeSetProperty<InterfaceSpecificationVersion>()

    @NodeRelationship(InterfacePart.DEFINED_ON, Direction.INCOMING)
    @GraphQLDescription(
        """InterfaceParts defined by this InterfaceSpecification.
        Note that active parts depend on the InterfaceSpecificationVersion
        """
    )
    @FilterProperty
    @delegate:Transient
    val definedParts by NodeSetProperty<InterfacePart>()

    @NodeRelationship(COMPONENT, Direction.OUTGOING)
    @GraphQLDescription("The Component this InterfaceSpecificaton is part of.")
    @FilterProperty
    @delegate:Transient
    val component by NodeProperty<Component>()
}