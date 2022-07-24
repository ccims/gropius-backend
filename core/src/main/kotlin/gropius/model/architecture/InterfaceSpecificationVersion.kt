package gropius.model.architecture

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import gropius.model.template.BaseTemplate
import gropius.model.template.InterfaceSpecificationVersionTemplate
import gropius.model.template.MutableTemplatedNode
import gropius.model.user.permission.NodePermission
import io.github.graphglue.model.*
import org.springframework.data.annotation.Transient
import org.springframework.data.neo4j.core.schema.CompositeProperty

@DomainNode
@GraphQLDescription(
    """A specific version of an InterfaceSpecification.
    Defines which InterfaceParts are active.
    Can be both visible (generates an Interface) and invisible (does not generate an Interface)
    on different Components.
    Can be derived by Relations, and affected by Issues.
    READ is granted if READ is granted on `interfaceSpecification`,
    or any InterfaceDefinition in `definitions`
    """
)
@Authorization(
    NodePermission.READ,
    allowFromRelated = ["interfaceSpecification", "definitions"]
)
class InterfaceSpecificationVersion(
    name: String,
    description: String,
    @property:GraphQLDescription("The version of this InterfaceSpecificationVersion.")
    @FilterProperty
    @OrderProperty
    override var version: String,
    @property:GraphQLIgnore
    @CompositeProperty
    override val templatedFields: MutableMap<String, String>
) : ServiceEffectSpecificationLocation(name, description), Versioned, MutableTemplatedNode {

    companion object {
        const val ACTIVE_PART = "ACTIVE_PART"
    }

    @NodeRelationship(BaseTemplate.USED_IN, Direction.INCOMING)
    @GraphQLDescription("The Template of this InterfaceSpecificationVersion")
    @FilterProperty
    @delegate:Transient
    override val template by NodeProperty<InterfaceSpecificationVersionTemplate>()

    @NodeRelationship(ACTIVE_PART, Direction.OUTGOING)
    @GraphQLDescription(
        """InterfaceParts which are active on this InterfaceSpecificationVersion
        Semantically, only the active parts on an InterfaceSpecificationVersion exist on the Interfaces
        defined by the InterfaceSpecificationVersion.
        """
    )
    @FilterProperty
    @delegate:Transient
    val activeParts by NodeSetProperty<InterfacePart>()

    @NodeRelationship(InterfaceSpecification.VERSION, Direction.INCOMING)
    @GraphQLDescription("The InterfaceSpecification this is part of.")
    @FilterProperty
    @delegate:Transient
    val interfaceSpecification by NodeProperty<InterfaceSpecification>()

    @NodeRelationship(
        InterfaceDefinition.INTERFACE_SPECIFICATION_VERSION,
        Direction.INCOMING
    )
    @GraphQLDescription("Defines on which ComponentVersions this InterfaceSpecificationVersion is used")
    @FilterProperty
    @delegate:Transient
    val definitions by NodeSetProperty<InterfaceDefinition>()

}