package gropius.model.architecture

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.common.NamedNode
import gropius.model.issue.Issue
import io.github.graphglue.model.Direction
import io.github.graphglue.model.DomainNode
import io.github.graphglue.model.FilterProperty
import io.github.graphglue.model.NodeRelationship
import org.springframework.data.annotation.Transient

@DomainNode
@GraphQLDescription(
    """Entities that can be affected by an Issue, meaning that this entity is in some regard 
    impacted by e.g. a bug described by an issue, or the non-present of a feature described by an issue.
    """
)
abstract class AffectedByIssue(name: String, description: String) : NamedNode(name, description) {

    companion object {
        const val AFFECTS = "AFFECTS"
    }

    @NodeRelationship(AFFECTS, Direction.OUTGOING)
    @GraphQLDescription("The issues which affect this entity")
    @FilterProperty
    @delegate:Transient
    val affectingIssues by NodeSetProperty<Issue>()

}