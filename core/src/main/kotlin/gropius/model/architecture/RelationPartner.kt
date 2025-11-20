package gropius.model.architecture

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import gropius.model.architecture.layout.RelationPartnerLayout
import gropius.model.issue.AggregatedIssue
import gropius.model.template.RelationPartnerTemplate
import io.github.graphglue.model.*
import io.github.graphglue.model.property.NodeCache

/**
 * Name of the bean defining the partOfProject filter
 */
const val PART_OF_PROJECT_FILTER = "partOfProject"

@DomainNode
@GraphQLDescription("Entity which can be used as start / end of Relations. Can be affected by Issues.")
@AdditionalFilter(PART_OF_PROJECT_FILTER)
abstract class RelationPartner : AffectedByIssue() {
    companion object {
        const val INCOMING_RELATION = "INCOMING_RELATION"
        const val OUTGOING_RELATION = "OUTGOING_RELATION"
        const val AGGREGATED_ISSUE = "AGGREGATED_ISSUE"
        const val LAYOUT = "LAYOUT"
    }

    @NodeRelationship(INCOMING_RELATION, Direction.OUTGOING)
    @GraphQLDescription("Relations which use this as the end of the Relation.")
    @FilterProperty
    val incomingRelations by NodeSetProperty<Relation>()

    @NodeRelationship(OUTGOING_RELATION, Direction.OUTGOING)
    @GraphQLDescription("Relations which use this as the start of the Relation.")
    @FilterProperty
    val outgoingRelations by NodeSetProperty<Relation>()

    @NodeRelationship(AGGREGATED_ISSUE, Direction.OUTGOING)
    @GraphQLDescription("AggregatedIssues on this RelationPartner.")
    @FilterProperty
    val aggregatedIssues by NodeSetProperty<AggregatedIssue>()

    @NodeRelationship(LAYOUT, Direction.OUTGOING)
    @GraphQLIgnore
    val layouts by NodeSetProperty<RelationPartnerLayout>()

    /**
     * Helper function to get the associated [RelationPartnerTemplate]
     *
     * @param cache cache used for accessing properties
     * @return the found template
     */
    @GraphQLIgnore
    abstract suspend fun relationPartnerTemplate(cache: NodeCache? = null): RelationPartnerTemplate<*, *>
}