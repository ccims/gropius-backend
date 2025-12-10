package gropius.dto.input.issue

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.scalars.ID
import gropius.dto.input.common.Input

@GraphQLDescription("Input for the removeIssueStateFromBoardColumn mutation")
class RemoveIssueStateFromBoardColumnInput(
    @GraphQLDescription("The id of the Issue Board Column from which the Issue State should be removed")
    val column: ID,
    @GraphQLDescription("The id of the Issue State to remove from the board column")
    val state: ID
) : Input()
