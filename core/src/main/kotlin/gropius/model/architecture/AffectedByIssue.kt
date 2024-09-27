package gropius.model.architecture

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.common.BaseNode
import gropius.model.issue.Issue
import io.github.graphglue.model.*

/**
 * Name of the bean defining the relatedTo filter
 */
const val RELATED_TO_FILTER_BEAN = "relatedToFilter"

@DomainNode(searchQueryName = "searchAffectedByIssues")
@GraphQLDescription(
    """Entities that can be affected by an Issue, meaning that this entity is in some regard 
    impacted by e.g. a bug described by an issue, or the non-present of a feature described by an issue.
    """
)
@AdditionalFilter(RELATED_TO_FILTER_BEAN)
abstract class AffectedByIssue : BaseNode() {

    @NodeRelationship(Issue.AFFECTS, Direction.INCOMING)
    @GraphQLDescription("The issues which affect this entity")
    @FilterProperty
    val affectingIssues by NodeSetProperty<Issue>()

}