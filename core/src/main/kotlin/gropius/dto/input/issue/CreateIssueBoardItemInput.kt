package gropius.dto.input.issue

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.scalars.ID
import gropius.dto.input.common.Input

@GraphQLDescription("Input for the createIssueBoardItem mutation")
class CreateIssueBoardItemInput(
    @GraphQLDescription("ID of the IssueBoard to add this item to")
    val issueBoard: ID,
    @GraphQLDescription("ID of the Issue to represent in this board item")
    val issue: ID,
    @GraphQLDescription("Initial position of the new board item")
    val position: Double
):Input() {
}