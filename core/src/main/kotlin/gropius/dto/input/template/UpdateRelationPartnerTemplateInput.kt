package gropius.dto.input.template

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.execution.OptionalInput
import gropius.dto.input.common.UpdateNamedNodeInput
import gropius.dto.input.ifPresent
import gropius.dto.input.template.style.FillStyleInput
import gropius.dto.input.template.style.StrokeStyleInput
import gropius.model.template.RelationPartnerTemplate
import gropius.model.template.style.ShapeType
import kotlin.properties.Delegates

/**
 * Fragment for update mutation inputs for classes extending [RelationPartnerTemplate]
 */
abstract class UpdateRelationPartnerTemplateInput : UpdateNamedNodeInput() {

    @GraphQLDescription("The corner radius of the shape, ignored for circle/ellipse")
    var shapeRadius: OptionalInput<Double?> by Delegates.notNull()

    @GraphQLDescription("The type of the shape")
    var shapeType: OptionalInput<ShapeType> by Delegates.notNull()

    @GraphQLDescription("Style of the fill, null removes the fill")
    var fill: OptionalInput<FillStyleInput?> by Delegates.notNull()

    @GraphQLDescription("Style of the stroke, null removes the stroke")
    var stroke: OptionalInput<StrokeStyleInput?> by Delegates.notNull()

    override fun validate() {
        super.validate()
        fill.ifPresent { it?.validate() }
        stroke.ifPresent { it?.validate() }
    }

}
