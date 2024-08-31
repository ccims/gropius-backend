package gropius.dto.input.architecture.layout

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.execution.OptionalInput
import com.expediagroup.graphql.generator.scalars.ID
import gropius.dto.input.common.UpdateNamedNodeInput

@GraphQLDescription("Input for the updateView mutation")
class UpdateViewInput(
    @GraphQLDescription("Defines the new layout of a set of Relations")
    override val relationLayouts: OptionalInput<List<UpdateRelationLayoutInput>>,
    @GraphQLDescription("Defines the new layout of a set of RelationPartners")
    override val relationPartnerLayouts: OptionalInput<List<UpdateRelationPartnerLayoutInput>>,
    @GraphQLDescription("The new filter of the view")
    val filterByTemplate: OptionalInput<List<ID>>
) : UpdateNamedNodeInput(), UpdateLayoutInput {

    override fun validate() {
        super.validate()
        validateLayout()
    }

}