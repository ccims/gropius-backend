package gropius.dto.input.architecture.layout

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.scalars.ID
import gropius.dto.input.common.Input

@GraphQLDescription("Input to update the layout of a Relation")
class UpdateRelationLayoutInput(
    @GraphQLDescription("The id of the Relation of which to update the layout")
    val relation: ID,
    @GraphQLDescription("The new layout of the Relation, or null if the layout should be reset")
    val layout: RelationLayoutInput?
) : Input() {

    override fun validate() {
        super.validate()
        layout?.validate()
    }

}