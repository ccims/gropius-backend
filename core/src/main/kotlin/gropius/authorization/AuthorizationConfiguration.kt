package gropius.authorization

import gropius.model.architecture.Component
import gropius.model.architecture.IMS
import gropius.model.architecture.Project
import gropius.model.common.AuditedNode
import gropius.model.user.GropiusUser
import gropius.model.user.permission.GlobalPermission
import gropius.model.user.permission.NodePermission
import io.github.graphglue.authorization.AllowRuleGenerator
import io.github.graphglue.authorization.DisallowRuleGenerator
import io.github.graphglue.definition.NodeDefinition
import io.github.graphglue.definition.NodeDefinitionCollection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * The name of the rule which checks that a Node is related to a [NodePermission]
 */
const val RELATED_TO_NODE_PERMISSION_RULE = "relatedToNodePermissionRule"

/**
 * The name of the [RelatedToAdminNodePermissionRuleGenerator], used to check if a node is related to a
 * [NodePermission] with ADMIN via [NodePermission.RELATED_TO_NODE_PERMISSION]
 */
const val RELATED_TO_ADMIN_NODE_PERMISSION_RULE = "relatedToAdminNodePermission"

/**
 * The name of the [IsDeletedRuleGenerator] used to check that an [AuditedNode] is deleted
 */
const val IS_DELETED_RULE = "isDeletedRule"

/**
 * The name of the [RelatedToGlobalPermissionRuleGenerator] used to check if a [GropiusUser] is related to a
 * [GlobalPermission]
 */
const val RELATED_TO_GLOBAL_PERMISSION_RULE = "relatedToGlobalPermission"

/**
 * Provides Authorization related beans which are necessary to check for permissions
 */
@Configuration
class AuthorizationConfiguration {

    /**
     * Creates the [RelatedToAdminNodePermissionRuleGenerator] to check for READ on [NodePermission]
     * The rule can only be used on [NodePermission], and only for READ
     *
     * @param nodeDefinitionCollection used to obtain [NodeDefinition]s
     * @return the generated [RelatedToAdminNodePermissionRuleGenerator]
     */
    @Bean(RELATED_TO_ADMIN_NODE_PERMISSION_RULE)
    fun relatedToAdminNodePermissionRule(nodeDefinitionCollection: NodeDefinitionCollection): AllowRuleGenerator {
        return RelatedToAdminNodePermissionRuleGenerator(
            nodeDefinitionCollection.getNodeDefinition<NodePermission<*>>(),
            nodeDefinitionCollection.getNodeDefinition<GropiusUser>()
        )
    }

    /**
     * Creates a new [RelatedToNodePermissionRuleGenerator] which can be used to check for permissions
     * on [IMS], [Component] and [Project]
     *
     * @param nodeDefinitionCollection used to get [NodeDefinition]s for [NodePermission] and [GropiusUser]
     * @return the generated [RelatedToNodePermissionRuleGenerator]
     */
    @Bean(RELATED_TO_NODE_PERMISSION_RULE)
    fun relatedToNodePermissionRule(nodeDefinitionCollection: NodeDefinitionCollection): RelatedToNodePermissionRuleGenerator {
        return RelatedToNodePermissionRuleGenerator(
            nodeDefinitionCollection.getNodeDefinition<NodePermission<*>>(),
            nodeDefinitionCollection.getNodeDefinition<GropiusUser>()
        )
    }

    /**
     * Creates a new [RelatedToGlobalPermissionRuleGenerator] which can be used to check for permission on [GropiusUser]
     *
     * @param nodeDefinitionCollection used to get [NodeDefinition]s for [GlobalPermission]
     * @return the generated [RelatedToGlobalPermissionRuleGenerator]
     */
    @Bean(RELATED_TO_GLOBAL_PERMISSION_RULE)
    fun relatedToGlobalPermissionRule(nodeDefinitionCollection: NodeDefinitionCollection): RelatedToGlobalPermissionRuleGenerator {
        return RelatedToGlobalPermissionRuleGenerator(nodeDefinitionCollection.getNodeDefinition<GlobalPermission>())
    }

}