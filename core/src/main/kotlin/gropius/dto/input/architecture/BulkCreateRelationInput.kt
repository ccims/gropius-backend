package gropius.dto.input.architecture

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.dto.input.common.Input

@GraphQLDescription("Input to create multiple Components")
class BulkCreateRelationInput(
    @GraphQLDescription("The input for the createRelation mutation")
    val relations: List<CreateRelationInput>
) : Input() {
    override fun validate() {
        relations.forEach(Input::validate)
    }
}