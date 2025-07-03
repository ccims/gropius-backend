package gropius.dto.input.issue

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.dto.input.common.UpdateNamedNodeInput

@GraphQLDescription("Input for the updateIssueBoard mutation")
class UpdateIssueBoardInput: UpdateNamedNodeInput()