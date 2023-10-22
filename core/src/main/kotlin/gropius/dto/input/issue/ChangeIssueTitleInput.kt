package gropius.dto.input.issue

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.scalars.ID
import gropius.dto.input.common.Input

@GraphQLDescription("Input for the changeIssueTitle mutation")
class ChangeIssueTitleInput(
    @GraphQLDescription("The id of the Issue of which the title is updated")
    val issue: ID,
    @GraphQLDescription("The new title")
    val title: String
) : Input()