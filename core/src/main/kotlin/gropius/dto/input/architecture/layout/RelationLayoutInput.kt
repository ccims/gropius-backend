package gropius.dto.input.architecture.layout

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.dto.input.common.Input
import gropius.model.architecture.layout.Point

@GraphQLDescription("Input which defines the layout of a Relation")
class RelationLayoutInput(
    @GraphQLDescription("List of intermediate points of the relation")
    val points: List<Point>,
) : Input()