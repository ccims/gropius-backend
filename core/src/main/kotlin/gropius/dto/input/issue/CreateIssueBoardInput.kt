package gropius.dto.input.issue

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.scalars.ID
import gropius.dto.input.common.CreateNamedNodeInput

@GraphQLDescription("Input for the createIssueBoard mutation")
class CreateIssueBoardInput(
    @GraphQLDescription("ID of Trackable the issue board is added to")
    val trackable: ID
) : CreateNamedNodeInput()
