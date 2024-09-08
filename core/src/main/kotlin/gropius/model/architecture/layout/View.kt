package gropius.model.architecture.layout

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.architecture.Project
import gropius.model.common.NamedNode
import gropius.model.template.ComponentTemplate
import io.github.graphglue.model.Direction
import io.github.graphglue.model.DomainNode
import io.github.graphglue.model.FilterProperty
import io.github.graphglue.model.NodeRelationship

@DomainNode(searchQueryName = "searchViews")
@GraphQLDescription("A view on the architecture graph of a project")
class View(
    name: String, description: String
) : NamedNode(name, description), Layout {

    companion object {
        const val RELATION_PARTNER = "RELATION_PARTNER"
        const val RELATION = "RELATION"
        const val FILTER_TEMPLATE = "FILTER_TEMPLATE"
    }

    @NodeRelationship(Project.VIEW, Direction.INCOMING)
    @GraphQLDescription("The project this view is for")
    @FilterProperty
    val project by NodeProperty<Project>()

    @NodeRelationship(RELATION_PARTNER, Direction.OUTGOING)
    @GraphQLDescription("Layouts for relation partners")
    @FilterProperty
    override val relationPartnerLayouts by NodeSetProperty<RelationPartnerLayout>()

    @NodeRelationship(RELATION, Direction.OUTGOING)
    @GraphQLDescription("Layouts for relations")
    @FilterProperty
    override val relationLayouts by NodeSetProperty<RelationLayout>()

    @NodeRelationship(FILTER_TEMPLATE, Direction.OUTGOING)
    @GraphQLDescription("Filter which ComponentVersions are shown in this view")
    @FilterProperty
    val filterByTemplate by NodeSetProperty<ComponentTemplate>()

}