package gropius.model.issue.timeline

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.template.IssueTemplate
import io.github.graphglue.model.Direction
import io.github.graphglue.model.DomainNode
import io.github.graphglue.model.FilterProperty
import io.github.graphglue.model.NodeRelationship
import org.springframework.data.annotation.Transient
import java.time.OffsetDateTime

@DomainNode
@GraphQLDescription("Event representing the template of an Issue changed")
class TemplateChangedEvent(
    createdAt: OffsetDateTime, lastModifiedAt: OffsetDateTime
) : ParentTimelineItem(createdAt, lastModifiedAt) {

    companion object {
        const val OLD_TEMPLATE = "OLD_TEMPLATE"
        const val NEW_TEMPLATE = "NEW_TEMPLATE"
    }

    @NodeRelationship(TemplateChangedEvent.OLD_TEMPLATE, Direction.OUTGOING)
    @GraphQLDescription("The old template.")
    @FilterProperty
    @delegate:Transient
    val oldTemplate by NodeProperty<IssueTemplate>()

    @NodeRelationship(TemplateChangedEvent.NEW_TEMPLATE, Direction.OUTGOING)
    @GraphQLDescription("The new template.")
    @FilterProperty
    @delegate:Transient
    val newTemplate by NodeProperty<IssueTemplate>()

}