package gropius.dto.input.template

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.execution.OptionalInput
import gropius.dto.input.common.UpdateNamedNodeInput
import gropius.model.template.style.ShapeType
import kotlin.properties.Delegates

@GraphQLDescription("Input for the updateComponentTemplate mutation")
class UpdateComponentTemplateInput(
    @GraphQLDescription("The corner radius of the shape, ignored for circle/ellipse")
    val shapeRadius: OptionalInput<Double?>,
    @GraphQLDescription("The type of the shape")
    val shapeType: OptionalInput<ShapeType>
) : UpdateNamedNodeInput()
