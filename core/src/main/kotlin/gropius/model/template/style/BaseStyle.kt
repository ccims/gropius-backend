package gropius.model.template.style

import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import gropius.model.common.BaseNode
import gropius.model.user.permission.NodePermission
import io.github.graphglue.model.Authorization
import io.github.graphglue.model.DomainNode

@DomainNode
@GraphQLIgnore
@Authorization(NodePermission.READ, allowAll = true)
abstract class BaseStyle : BaseNode()