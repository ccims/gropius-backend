package gropius.dto.input.architecture.layout

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.execution.OptionalInput
import com.expediagroup.graphql.generator.scalars.ID
import gropius.dto.input.common.CreateNamedNodeInput

@GraphQLDescription("Input for the createView mutation")
class CreateViewInput(
    @GraphQLDescription("Defines the new layout of a set of Relations")
    override val relationLayouts: OptionalInput<List<UpdateRelationLayoutInput>>,
    @GraphQLDescription("Defines the new layout of a set of RelationPartners")
    override val relationPartnerLayouts: OptionalInput<List<UpdateRelationPartnerLayoutInput>>,
    @GraphQLDescription("The new filter of the view")
    val filterByTemplate: OptionalInput<List<ID>>,
    @GraphQLDescription("The id of the project the view belongs to")
    val project: ID
) : CreateNamedNodeInput(), UpdateLayoutInput {

    override fun validate() {
        super.validate()
        validateLayout()
    }

}