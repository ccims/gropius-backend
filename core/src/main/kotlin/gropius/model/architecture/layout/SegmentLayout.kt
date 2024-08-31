package gropius.model.architecture.layout

import com.expediagroup.graphql.generator.annotations.GraphQLDescription

@GraphQLDescription("The layout of a path segment")
enum class SegmentLayout {
    @GraphQLDescription("First a vertical segment, then a horizontal segment")
    VERTICAL_HORIZONTAL,

    @GraphQLDescription("First a horizontal segment, then a vertical segment")
    HORIZONTAL_VERTICAL,
}