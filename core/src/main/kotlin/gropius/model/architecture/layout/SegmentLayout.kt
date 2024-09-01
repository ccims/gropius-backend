package gropius.model.architecture.layout

import com.expediagroup.graphql.generator.annotations.GraphQLDescription

@GraphQLDescription("The layout of a path segment")
enum class SegmentLayout(val value: Boolean) {
    @GraphQLDescription("First a vertical segment, then a horizontal segment")
    VERTICAL_HORIZONTAL(true),

    @GraphQLDescription("First a horizontal segment, then a vertical segment")
    HORIZONTAL_VERTICAL(false);

    companion object {

        /**
         * Returns the [SegmentLayout] for the given value
         */
        fun from(value: Boolean): SegmentLayout {
            return if (value) {
                VERTICAL_HORIZONTAL
            } else {
                HORIZONTAL_VERTICAL
            }
        }
    }
}