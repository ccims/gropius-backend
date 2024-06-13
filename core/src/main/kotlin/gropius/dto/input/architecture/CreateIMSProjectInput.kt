package gropius.dto.input.architecture

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.scalars.ID
import gropius.dto.input.common.CreateNamedNodeInput
import gropius.dto.input.common.JSONFieldInput
import gropius.dto.input.common.validateAndEnsureNoDuplicates
import gropius.dto.input.template.CreateTemplatedNodeInput

@GraphQLDescription("Input for the createIMSProject mutation")
class CreateIMSProjectInput(
    @GraphQLDescription("Initial values for all templatedFields")
    override val templatedFields: List<JSONFieldInput>,
    @GraphQLDescription("The id of the IMS the created project is part of")
    val ims: ID,
    @GraphQLDescription("The id of the Trackable which is synced")
    val trackable: ID
) : CreateNamedNodeInput(), CreateTemplatedNodeInput {

    override fun validate() {
        super.validate()
        templatedFields.validateAndEnsureNoDuplicates()
    }
}