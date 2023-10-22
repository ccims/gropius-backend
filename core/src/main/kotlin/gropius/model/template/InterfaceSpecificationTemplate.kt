package gropius.model.template

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.architecture.InterfaceSpecification
import io.github.graphglue.model.Direction
import io.github.graphglue.model.DomainNode
import io.github.graphglue.model.FilterProperty
import io.github.graphglue.model.NodeRelationship

@DomainNode("interfaceSpecificationTemplates")
@GraphQLDescription(
    """Template for InterfaceSpecifications.
    Defines templated fields with specific types (defined using JSON schema).
    Defines on which Components InterfaceSpecifications with this Template can be (in)visible on.
    Defines SubTemplates for Interfaces and InterfaceSpecificationVersions.
    """
)
class InterfaceSpecificationTemplate(
    name: String, description: String, templateFieldSpecifications: MutableMap<String, String>, isDeprecated: Boolean
) : RelationPartnerTemplate<InterfaceSpecification, InterfaceSpecificationTemplate>(
    name, description, templateFieldSpecifications, isDeprecated
) {

    @NodeRelationship(ComponentTemplate.VISIBLE_INTERFACE_SPECIFICATION, Direction.INCOMING)
    @GraphQLDescription("Templates of Components InterfaceSpecifications with this template can be visible on.")
    @FilterProperty
    val canBeVisibleOnComponents by NodeSetProperty<ComponentTemplate>()

    @NodeRelationship(ComponentTemplate.INVISIBLE_INTERFACE_SPECIFICATION, Direction.INCOMING)
    @GraphQLDescription("Templates of Components InterfaceSpecifications with this template can be invisible on.")
    @FilterProperty
    val canBeInvisibleOnComponents by NodeSetProperty<ComponentTemplate>()

    @NodeRelationship(
        InterfaceSpecificationDerivationCondition.DERIVABLE_INTERFACE_SPECIFICATION, Direction.INCOMING
    )
    @GraphQLDescription(
        """InterfaceSpecificationDerivationConditions which allow to derive InterfaceSpecification with this template.
        """
    )
    @FilterProperty
    val derivableBy by NodeSetProperty<InterfaceSpecificationDerivationCondition>()

    @NodeRelationship(SubTemplate.PART_OF, Direction.INCOMING)
    @GraphQLDescription(
        """SubTemplate applied to all InterfaceSpecificationVersions of InterfaceSpecifications with this Template.
        """
    )
    val interfaceSpecificationVersionTemplate by NodeProperty<InterfaceSpecificationVersionTemplate>()

    @NodeRelationship(SubTemplate.PART_OF, Direction.INCOMING)
    @GraphQLDescription(
        """SubTemplate applied to all InterfaceParts of InterfaceSpecifications with this Template.
        """
    )
    val interfacePartTemplate by NodeProperty<InterfacePartTemplate>()

    @NodeRelationship(SubTemplate.PART_OF, Direction.INCOMING)
    @GraphQLDescription(
        """SubTemplate applied to all Interfaces of InterfaceSpecifications with this Template.
        """
    )
    val interfaceTemplate by NodeProperty<InterfaceTemplate>()

    @NodeRelationship(SubTemplate.PART_OF, Direction.INCOMING)
    @GraphQLDescription(
        """SubTemplate applied to all InterfaceDefinitions of InterfaceSpecifications with this Template.
        """
    )
    val interfaceDefinitionTemplate by NodeProperty<InterfaceDefinitionTemplate>()

}