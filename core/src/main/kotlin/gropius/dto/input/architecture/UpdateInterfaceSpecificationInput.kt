package gropius.dto.input.architecture

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.execution.OptionalInput
import com.expediagroup.graphql.generator.scalars.ID
import gropius.dto.input.common.JSONFieldInput
import gropius.dto.input.common.UpdateNamedNodeInput
import gropius.dto.input.common.validateAndEnsureNoDuplicates
import gropius.dto.input.ifPresent
import gropius.dto.input.template.UpdateTemplatedNodeInput

@GraphQLDescription("Input for the updateInterfaceSpecification mutation")
class UpdateInterfaceSpecificationInput(
    @GraphQLDescription("Values for templatedFields to update")
    override val templatedFields: OptionalInput<List<JSONFieldInput>>,
    @GraphQLDescription(
        """If provided, the id of the new template for the Component
        Use `templatedFields` to update fields so that they conform with the new specifications.
        No longer needed fields are automatically removed.
        """
    )
    val template: OptionalInput<ID>,
    @GraphQLDescription(
        """Values for templatedFields of InterfaceSpecificationVersions to update.
        Only evaluated if `template` is provided!
        Affect all InterfaceSpecificationVersions of the updated InterfaceSpecification
        """
    )
    val interfaceSpecificationVersionTemplatedFields: OptionalInput<List<JSONFieldInput>>,
    @GraphQLDescription(
        """Values for templatedFields of InterfaceParts to update.
        Only evaluated if `template` is provided!
        Affect all InterfaceParts of the updated InterfaceSpecification
        """
    )
    val interfacePartTemplatedFields: OptionalInput<List<JSONFieldInput>>,
) : UpdateNamedNodeInput(), UpdateTemplatedNodeInput {

    override fun validate() {
        super.validate()
        templatedFields.ifPresent {
            it.validateAndEnsureNoDuplicates()
        }
        interfaceSpecificationVersionTemplatedFields.ifPresent {
            it.validateAndEnsureNoDuplicates()
        }
        interfacePartTemplatedFields.ifPresent {
            it.validateAndEnsureNoDuplicates()
        }
    }
}