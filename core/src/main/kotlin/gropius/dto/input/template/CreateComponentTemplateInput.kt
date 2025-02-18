package gropius.dto.input.template

import com.expediagroup.graphql.generator.annotations.GraphQLDescription

@GraphQLDescription("Input for the createComponentTemplate mutation")
class CreateComponentTemplateInput(
    @GraphQLDescription("SubTemplate for all ComponentVersions of a Component with the created Template")
    val componentVersionTemplate: SubTemplateInput,
    @GraphQLDescription("Set of all types IntraComponentDependencySpecifications of Components with the created Template can have")
    val intraComponentDependencySpecificationTypes: List<IntraComponentDependencySpecificationTypeInput>
) : CreateRelationPartnerTemplateInput() {

    override fun validate() {
        super.validate()
        componentVersionTemplate.validate()
        intraComponentDependencySpecificationTypes.forEach { it.validate() }
    }
}