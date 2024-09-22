package gropius.dto.input.architecture.layout

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.dto.input.common.Input
import gropius.model.architecture.layout.Point

@GraphQLDescription("Input which defines the layout of a RelationPartner")
class RelationPartnerLayoutInput(
    @GraphQLDescription("The position of the RelationPartner")
    val pos: Point
) : Input()