package gropius.dto.input.architecture

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.execution.OptionalInput
import com.expediagroup.graphql.generator.scalars.ID
import gropius.dto.input.architecture.layout.UpdateLayoutInput
import gropius.dto.input.architecture.layout.UpdateRelationLayoutInput
import gropius.dto.input.architecture.layout.UpdateRelationPartnerLayoutInput

@GraphQLDescription("Input for the updateProject mutation")
class UpdateProjectInput(
    @GraphQLDescription("The default view for the project")
    val defaultView: OptionalInput<ID?>,
    @GraphQLDescription("Defines the new layout of a set of Relations")
    override val relationLayouts: OptionalInput<List<UpdateRelationLayoutInput>>,
    @GraphQLDescription("Defines the new layout of a set of RelationPartners")
    override val relationPartnerLayouts: OptionalInput<List<UpdateRelationPartnerLayoutInput>>,
) : UpdateTrackableInput(), UpdateLayoutInput {

    override fun validate() {
        super.validate()
        validateLayout()
    }

}