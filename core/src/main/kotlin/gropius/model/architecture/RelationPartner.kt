package gropius.model.architecture

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import gropius.model.template.RelationPartnerTemplate
import gropius.model.template.TemplatedNode
import io.github.graphglue.model.Direction
import io.github.graphglue.model.DomainNode
import io.github.graphglue.model.FilterProperty
import io.github.graphglue.model.NodeRelationship
import io.github.graphglue.model.property.NodeCache

@DomainNode
@GraphQLDescription("Entity which can be used as start / end of Relations. Can be affected by Issues.")
abstract class RelationPartner(name: String, description: String) : AffectedByIssue(name, description), TemplatedNode {
    companion object {
        const val INCOMING_RELATION = "INCOMING_RELATION"
        const val OUTGOING_RELATION = "OUTGOING_RELATION"
    }

    @NodeRelationship(INCOMING_RELATION, Direction.OUTGOING)
    @GraphQLDescription("Relations which use this as the end of the Relation.")
    @FilterProperty
    val incomingRelations by NodeSetProperty<Relation>()

    @NodeRelationship(OUTGOING_RELATION, Direction.OUTGOING)
    @GraphQLDescription("Relations which use this as the start of the Relation.")
    @FilterProperty
    val outgoingRelations by NodeSetProperty<Relation>()

    /**
     * Helper function to get the associated [RelationPartnerTemplate]
     *
     * @param cache cache used for accessing properties
     * @return the found template
     */
    @GraphQLIgnore
    abstract suspend fun relationPartnerTemplate(cache: NodeCache? = null): RelationPartnerTemplate<*, *>
}