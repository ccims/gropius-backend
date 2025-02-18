package gropius.dto.input.issue

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.execution.OptionalInput
import com.expediagroup.graphql.generator.scalars.ID
import gropius.dto.input.common.Input
import gropius.dto.input.common.JSONFieldInput
import gropius.dto.input.common.TypeMappingInput
import gropius.dto.input.common.ensureUniqueMapping
import gropius.dto.input.common.validateAndEnsureNoDuplicates
import gropius.dto.input.ifPresent

@GraphQLDescription("Input for the changeIssueTemplate mutation")
class ChangeIssueTemplateInput(
    @GraphQLDescription("The Issue to update the template of")
    val issue: ID,
    @GraphQLDescription("Values for templatedFields to update, required to ensure compatibility with the new template")
    val templatedFields: OptionalInput<List<JSONFieldInput>>,
    @GraphQLDescription("If provided, the id of the new template for the Issue")
    val template: ID,
    @GraphQLDescription("The new type of the Issue, required if the old one is not compatible with the new template")
    val type: OptionalInput<ID>,
    @GraphQLDescription("The new state of the Issue, required if the old one is not compatible with the new template")
    val state: OptionalInput<ID>,
    @GraphQLDescription("The new priority of the Issue")
    val priority: OptionalInput<ID>,
    @GraphQLDescription("Mapping to map existing Assignment Types to new ones")
    val assignmentTypeMapping: OptionalInput<List<TypeMappingInput>>,
    @GraphQLDescription("Mapping to map existing IssueRelationTypes to new ones")
    val issueRelationTypeMapping: OptionalInput<List<TypeMappingInput>>
) : Input() {

    override fun validate() {
        super.validate()
        templatedFields.ifPresent { it.validateAndEnsureNoDuplicates() }
        assignmentTypeMapping.ifPresent { it.ensureUniqueMapping() }
        issueRelationTypeMapping.ifPresent { it.ensureUniqueMapping() }
    }
}