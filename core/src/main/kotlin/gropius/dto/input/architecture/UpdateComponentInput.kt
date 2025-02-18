package gropius.dto.input.architecture

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.execution.OptionalInput
import com.expediagroup.graphql.generator.scalars.ID
import gropius.dto.input.common.JSONFieldInput
import gropius.dto.input.common.TypeMappingInput
import gropius.dto.input.common.ensureUniqueMapping
import gropius.dto.input.common.validateAndEnsureNoDuplicates
import gropius.dto.input.ifPresent
import gropius.dto.input.template.UpdateTemplatedNodeInput

@GraphQLDescription("Input for the updateComponent mutation")
class UpdateComponentInput(
    @GraphQLDescription("Values for templatedFields to update")
    override val templatedFields: OptionalInput<List<JSONFieldInput>>,
    @GraphQLDescription(
        """If provided, the id of the new template for the Component
        Use `templatedFields` to update fields so that they conform with the new specifications.
        Use `componentVersionTemplatedFields` to update the `templatedFields` of ALL ComponentVersions
        No longer needed fields are automatically removed.
        """
    )
    val template: OptionalInput<ID>,
    @GraphQLDescription(
        """Values for templatedFields of ComponentVersions to update.
        Only evaluated if `template` is provided!
        Affect all ComponentVersions of the updated Component
        """
    )
    val componentVersionTemplatedFields: OptionalInput<List<JSONFieldInput>>,
    @GraphQLDescription("Mapping to map existing IntraComponentDependencySpecificationTypes to new ones")
    val intraComponentDependencySpecificationTypeMapping: OptionalInput<List<TypeMappingInput>>
) : UpdateTrackableInput(), UpdateTemplatedNodeInput {

    override fun validate() {
        super.validate()
        templatedFields.ifPresent {
            it.validateAndEnsureNoDuplicates()
        }
        componentVersionTemplatedFields.ifPresent {
            it.validateAndEnsureNoDuplicates()
        }
        intraComponentDependencySpecificationTypeMapping.ifPresent {
            it.ensureUniqueMapping()
        }
    }
}