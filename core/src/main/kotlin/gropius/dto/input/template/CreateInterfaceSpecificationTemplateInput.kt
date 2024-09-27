package gropius.dto.input.template

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.scalars.ID

@GraphQLDescription("Input for the createInterfaceSpecificationTemplate mutation")
class CreateInterfaceSpecificationTemplateInput(
    @GraphQLDescription("The IDs of Templates of Components InterfaceSpecifications with the created template can be visible on")
    val canBeVisibleOnComponents: List<ID>,
    @GraphQLDescription("The IDs of Templates of Components InterfaceSpecifications with the created template can be invisible on")
    val canBeInvisibleOnComponents: List<ID>,
    @GraphQLDescription("SubTemplate for all InterfaceSpecificationVersions of a InterfaceSpecification with the created Template")
    val interfaceSpecificationVersionTemplate: SubTemplateInput,
    @GraphQLDescription("SubTemplate for all InterfaceParts of a InterfaceSpecification with the created Template")
    val interfacePartTemplate: SubTemplateInput
) : CreateRelationPartnerTemplateInput() {

    override fun validate() {
        super.validate()
        interfaceSpecificationVersionTemplate.validate()
        interfacePartTemplate.validate()
    }

}