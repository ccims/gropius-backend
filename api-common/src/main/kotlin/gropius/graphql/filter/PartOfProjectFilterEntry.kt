package gropius.graphql.filter

import gropius.model.user.permission.ProjectPermission
import io.github.graphglue.authorization.Permission
import io.github.graphglue.connection.filter.model.FilterEntry
import org.neo4j.cypherdsl.core.Condition
import org.neo4j.cypherdsl.core.Cypher
import org.neo4j.cypherdsl.core.Node

/**
 * Parsed filter entry of a [AffectedByIssueRelatedToFilterEntryDefinition]
 *
 * @param projectId the id of the Project to which the entity must be related to
 * @param permission the node permission to check
 * @param partOfProjectFilterEntryDefinition [PartOfProjectFilterEntryDefinition] used to create this entry
 */
class PartOfProjectFilterEntry(
    val projectId: String,
    private val partOfProjectFilterEntryDefinition: PartOfProjectFilterEntryDefinition,
    private val permission: Permission?

) : FilterEntry(partOfProjectFilterEntryDefinition) {

    override fun generateCondition(node: Node): Condition {
        val relatedNode = Cypher.anyNode(node.requiredSymbolicName.value + "_")
        val relationship = node.relationshipTo(relatedNode).min(0)
            .withProperties(mapOf(ProjectPermission.PART_OF_PROJECT to Cypher.literalTrue()))
        val authCondition = if (permission != null) {
            partOfProjectFilterEntryDefinition.generateAuthorizationCondition(permission).generateCondition(relatedNode)
        } else {
            Cypher.noCondition()
        }
        val idCondition = relatedNode.property("id").isEqualTo(Cypher.anonParameter(projectId))
        return Cypher.match(relationship).where(idCondition.and(authCondition)).asCondition()
    }

}