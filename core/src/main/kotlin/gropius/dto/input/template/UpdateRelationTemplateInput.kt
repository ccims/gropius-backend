package gropius.dto.input.template

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.execution.OptionalInput
import gropius.dto.input.common.UpdateNamedNodeInput
import gropius.model.template.style.MarkerType
import kotlin.properties.Delegates

@GraphQLDescription("Input for the updateRelationTemplate mutation")
class UpdateRelationTemplateInput(
    @GraphQLDescription("The type of the marker at the end of the relation")
    val markerType: OptionalInput<MarkerType>
) : UpdateNamedNodeInput()
