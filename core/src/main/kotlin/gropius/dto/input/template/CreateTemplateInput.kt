package gropius.dto.input.template

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.execution.OptionalInput
import com.expediagroup.graphql.generator.scalars.ID
import gropius.model.template.Template
import kotlin.properties.Delegates

/**
 * Fragment for create mutation inputs for classes extending [Template]
 */
abstract class CreateTemplateInput : CreateBaseTemplateInput() {

    @GraphQLDescription("IDs of Templates the created template extends. Must be templates of the same type.")
    var extends: OptionalInput<List<ID>> by Delegates.notNull()

}