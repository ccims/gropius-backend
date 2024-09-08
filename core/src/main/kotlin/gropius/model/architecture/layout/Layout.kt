package gropius.model.architecture.layout

import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import gropius.model.architecture.Project
import io.github.graphglue.model.property.LazyLoadingDelegate
import io.github.graphglue.model.property.NodeSetPropertyDelegate

/**
 * Interface for common layout information of [Project]s and [View]s
 */
@GraphQLIgnore
interface Layout {

    /**
     * Layouts for relation partners
     */
    val relationPartnerLayouts: LazyLoadingDelegate<RelationPartnerLayout, NodeSetPropertyDelegate<RelationPartnerLayout>.NodeSetProperty>

    /**
     * Layouts for relations
     */
    val relationLayouts: LazyLoadingDelegate<RelationLayout, NodeSetPropertyDelegate<RelationLayout>.NodeSetProperty>

}