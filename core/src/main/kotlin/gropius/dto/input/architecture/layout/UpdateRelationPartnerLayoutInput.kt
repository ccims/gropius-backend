package gropius.dto.input.architecture.layout

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.scalars.ID
import gropius.dto.input.common.Input

@GraphQLDescription("Input to update the layout of a RelationPartner")
class UpdateRelationPartnerLayoutInput(
    @GraphQLDescription("The id of the RelationPartner of which to update the layout")
    val relationPartner: ID,
    @GraphQLDescription("The new layout of the RelationPartner, or null if the layout should be reset")
    val layout: RelationPartnerLayoutInput?
) : Input() {

    override fun validate() {
        super.validate()
        layout?.validate()
    }

}