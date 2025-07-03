package gropius.dto.input.issue

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.scalars.ID
import gropius.dto.input.common.Input

@GraphQLDescription("Input for the removeIssueStateToBoardColumn mutation")
class RemoveIssueStateFromBoardColumnInput(
    @GraphQLDescription("The id of the Issue Board Column where to remove the Issue State")
    val column: ID,
    @GraphQLDescription("The id of the Issue State to remove" )
    val state: ID
):Input()
