package gropius.dto.input.issue

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.scalars.ID
import gropius.dto.input.common.CreateNamedNodeInput

@GraphQLDescription("Input for the createIssueBoardColumn mutation")
class CreateIssueBoardColumnInput(
    @GraphQLDescription("ID of issue board the column is added to")
    val issueBoard: ID
) : CreateNamedNodeInput()