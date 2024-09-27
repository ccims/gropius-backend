package gropius.dto.input.architecture

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.execution.OptionalInput
import gropius.dto.input.common.Input
import gropius.dto.input.common.JSONFieldInput
import gropius.dto.input.common.validateAndEnsureNoDuplicates
import gropius.dto.input.ifPresent
import gropius.dto.input.template.CreateTemplatedNodeInput
import kotlin.properties.Delegates

@GraphQLDescription("Input to create an InterfaceSpecificationVersion")
open class InterfaceSpecificationVersionInput : Input(), CreateTemplatedNodeInput {

    @GraphQLDescription("Initial values for all templatedFields")
    override var templatedFields: List<JSONFieldInput> by Delegates.notNull()

    @GraphQLDescription("Initial InterfaceParts")
    var parts: OptionalInput<List<InterfacePartInput>> by Delegates.notNull()

    @GraphQLDescription("The version of the created InterfaceSpecificationVersion")
    var version: String by Delegates.notNull()

    @GraphQLDescription("The tags of the created InterfaceSpecificationVersion")
    var tags: List<String> by Delegates.notNull()

    override fun validate() {
        super.validate()
        templatedFields.validateAndEnsureNoDuplicates()
        parts.ifPresent {
            it.forEach(Input::validate)
        }
    }

}