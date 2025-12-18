package gropius.dto.input.template

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.dto.input.common.CreateNamedNodeInput

@GraphQLDescription("Input to create an IssuePriority")
class IssuePriorityInput(
    @GraphQLDescription("The value of the created IssuePriority, used to compare/order different IssuePriorities")
    val value: Double,
    @GraphQLDescription("A path that is used as the icon for issues. Used with a 0 0 100 100 viewBox. No stroke, only fill.")
    val iconPath: String
) : CreateNamedNodeInput()