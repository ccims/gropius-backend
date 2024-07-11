package gropius.graphql.filter

import gropius.model.user.permission.TrackablePermission
import io.github.graphglue.authorization.Permission
import io.github.graphglue.connection.filter.model.FilterEntry
import org.neo4j.cypherdsl.core.Condition
import org.neo4j.cypherdsl.core.Cypher
import org.neo4j.cypherdsl.core.Node

/**
 * Parsed filter entry of a [AffectedByIssueRelatedToFilterEntryDefinition]
 *
 * @param trackableId the id of the Trackable to which the entity must be related to
 * @param permission the node permission to check
 * @param relatedToFilterEntryDefinition [AffectedByIssueRelatedToFilterEntryDefinition] used to create this entry
 */
class AffectedByIssueRelatedToFilterEntry(
    val trackableId: String,
    private val relatedToFilterEntryDefinition: AffectedByIssueRelatedToFilterEntryDefinition,
    private val permission: Permission?

) : FilterEntry(relatedToFilterEntryDefinition) {

    override fun generateCondition(node: Node): Condition {
        val relatedNode = Cypher.anyNode(node.requiredSymbolicName.value + "_")
        val relationship = node.relationshipTo(relatedNode).min(0)
            .withProperties(mapOf(TrackablePermission.RELATED_ISSUE_AFFECTED_ENTITY to Cypher.literalTrue()))
        val authCondition = if (permission != null) {
            relatedToFilterEntryDefinition.generateAuthorizationCondition(permission).generateCondition(relatedNode)
        } else {
            Cypher.noCondition()
        }
        val idCondition = relatedNode.property("id").isEqualTo(Cypher.anonParameter(trackableId))
        return Cypher.match(relationship).where(idCondition.and(authCondition)).asCondition()
    }

}