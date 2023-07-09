package gropius.dto.input.user

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.execution.OptionalInput
import java.net.URI

@GraphQLDescription("Input for the updateGropiusUserMutation")
class UpdateGropiusUserInput(
    @GraphQLDescription("The new value for isAdmin of the GropiusUser to update")
    val isAdmin: OptionalInput<Boolean>,
    @GraphQLDescription("The new avatar of the user")
    val avatar: OptionalInput<URI?>
) : UpdateUserInput()