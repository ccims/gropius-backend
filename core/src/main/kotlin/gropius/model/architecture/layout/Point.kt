package gropius.model.architecture.layout

import com.expediagroup.graphql.generator.annotations.GraphQLDescription

@GraphQLDescription("A point in a 2D coordinate system")
class Point(
    @property:GraphQLDescription("The x coordinate of the point")
    val x: Int,
    @property:GraphQLDescription("The y coordinate of the point")
    val y: Int
)