package gropius.model.architecture

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import gropius.model.template.BaseTemplate
import gropius.model.template.InterfaceSpecificationVersionTemplate
import gropius.model.template.MutableTemplatedNode
import gropius.model.user.permission.NodePermission
import gropius.model.user.permission.TrackablePermission
import io.github.graphglue.model.*
import org.springframework.data.neo4j.core.schema.CompositeProperty

@DomainNode(searchQueryName = "searchInterfaceSpecificationVersions")
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
    NodePermission.READ, allowFromRelated = ["interfaceSpecification", "interfaceDefinitions"]
)
@Authorization(NodePermission.ADMIN, allowFromRelated = ["interfaceSpecification"])
@Authorization(TrackablePermission.AFFECT_ENTITIES_WITH_ISSUES, allowFromRelated = ["interfaceSpecification"])
@Authorization(
    TrackablePermission.RELATED_ISSUE_AFFECTED_ENTITY,
    allowFromRelated = ["interfaceDefinitions", "interfaceSpecification"]
)
class InterfaceSpecificationVersion(
    @property:GraphQLDescription("The version of this InterfaceSpecificationVersion.")
    @FilterProperty
    @SearchProperty
    @OrderProperty
    override var version: String,
    @property:GraphQLDescription("The tags of this InterfaceSpecificationVersion")
    @SearchProperty
    override var tags: List<String>,
    @property:GraphQLIgnore
    @CompositeProperty
    override val templatedFields: MutableMap<String, String>
) : AffectedByIssue(), Versioned, MutableTemplatedNode {

    companion object {
        const val PART = "PART"
    }

    @NodeRelationship(BaseTemplate.USED_IN, Direction.INCOMING)
    @GraphQLDescription("The Template of this InterfaceSpecificationVersion")
    @FilterProperty
    @OrderProperty
    override val template by NodeProperty<InterfaceSpecificationVersionTemplate>()

    @NodeRelationship(PART, Direction.OUTGOING)
    @GraphQLDescription(
        """InterfaceParts which are part of this InterfaceSpecificationVersion
        """
    )
    @FilterProperty
    val parts by NodeSetProperty<InterfacePart>()

    @NodeRelationship(InterfaceSpecification.VERSION, Direction.INCOMING)
    @GraphQLDescription("The InterfaceSpecification this is part of.")
    @FilterProperty
    val interfaceSpecification by NodeProperty<InterfaceSpecification>()

    @NodeRelationship(
        InterfaceDefinition.INTERFACE_SPECIFICATION_VERSION, Direction.INCOMING
    )
    @GraphQLDescription("Defines on which ComponentVersions this InterfaceSpecificationVersion is used")
    @FilterProperty
    val interfaceDefinitions by NodeSetProperty<InterfaceDefinition>()

}