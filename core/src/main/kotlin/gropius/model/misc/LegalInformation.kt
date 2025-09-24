package gropius.model.misc

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.user.permission.NodePermission
import io.github.graphglue.model.Authorization
import io.github.graphglue.model.DomainNode
import io.github.graphglue.model.Node
import io.github.graphglue.model.OrderProperty
import io.github.graphglue.model.SearchProperty

@DomainNode("legalInformation", searchQueryName = "searchLegalInformation")
@GraphQLDescription("Legal Information to be accessible to users")
@Authorization(NodePermission.READ, allowAll = true)
class LegalInformation(
    @property:GraphQLDescription("The label shown to user in the bottom right corner")
    @SearchProperty
    var label : String,
    @property:GraphQLDescription("The markdown text shown to the user when accessing the information")
    @SearchProperty
    var text : String,
    @property:GraphQLDescription("The priority of this information, higher priority items are shown further left.")
    @OrderProperty
    var priority: Int
) : Node()