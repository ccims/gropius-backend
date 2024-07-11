package gropius.model.architecture

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import gropius.model.template.BaseTemplate
import gropius.model.template.ComponentVersionTemplate
import gropius.model.template.MutableTemplatedNode
import gropius.model.template.RelationPartnerTemplate
import gropius.model.user.permission.ComponentPermission
import gropius.model.user.permission.NodePermission
import gropius.model.user.permission.ProjectPermission
import gropius.model.user.permission.TrackablePermission
import io.github.graphglue.model.*
import io.github.graphglue.model.property.NodeCache
import org.springframework.data.neo4j.core.schema.CompositeProperty

@DomainNode(searchQueryName = "searchComponentVersions")
@GraphQLDescription(
    """Version of a component. 
    Can specifies visible/invisible InterfaceSpecifications.
    Can be used in Relations, affected by issues and included by Projects.
    READ is granted if READ is granted on `component`.
    """
)
@Authorization(NodePermission.READ, allowFromRelated = ["component", "includingProjects"])
@Authorization(NodePermission.ADMIN, allowFromRelated = ["component"])
@Authorization(ComponentPermission.RELATE_FROM_COMPONENT, allowFromRelated = ["component"])
@Authorization(ComponentPermission.ADD_TO_PROJECTS, allowFromRelated = ["component"])
@Authorization(TrackablePermission.AFFECT_ENTITIES_WITH_ISSUES, allowFromRelated = ["component"])
@Authorization(TrackablePermission.RELATED_ISSUE_AFFECTED_ENTITY, allowFromRelated = ["component"])
@Authorization(ProjectPermission.PART_OF_PROJECT, allowFromRelated = ["includingProjects"])
class ComponentVersion(
    name: String,
    description: String,
    @property:GraphQLDescription("The version of this ComponentVersion")
    @FilterProperty
    @OrderProperty
    override var version: String,
    @property:GraphQLIgnore
    @CompositeProperty
    override val templatedFields: MutableMap<String, String>
) : RelationPartner(name, description), Versioned, MutableTemplatedNode {

    companion object {
        const val INTRA_COMPONENT_DEPENDENCY_SPECIFICATION = "INTRA_COMPONENT_DEPENDENCY_SPECIFICATION"
    }

    @NodeRelationship(BaseTemplate.USED_IN, Direction.INCOMING)
    @GraphQLDescription("The Template of this ComponentVersion")
    @FilterProperty
    @OrderProperty
    override val template by NodeProperty<ComponentVersionTemplate>()

    @NodeRelationship(Component.VERSION, Direction.INCOMING)
    @GraphQLDescription("The Component which defines this ComponentVersions")
    @FilterProperty
    val component by NodeProperty<Component>()

    @NodeRelationship(Project.COMPONENT, Direction.INCOMING)
    @GraphQLDescription("Projects which include this ComponentVersion")
    @FilterProperty
    val includingProjects by NodeSetProperty<Project>()

    @NodeRelationship(InterfaceDefinition.COMPONENT_VERSION, Direction.INCOMING)
    @GraphQLDescription("InterfaceDefinitions on this ComponentVersion.")
    @FilterProperty
    val interfaceDefinitions by NodeSetProperty<InterfaceDefinition>()

    @NodeRelationship(INTRA_COMPONENT_DEPENDENCY_SPECIFICATION, Direction.OUTGOING)
    @GraphQLDescription("IntraComponentDependencySpecifications associated with this ComponentVersion")
    @FilterProperty
    val intraComponentDependencySpecifications by NodeSetProperty<IntraComponentDependencySpecification>()

    @GraphQLIgnore
    override suspend fun relationPartnerTemplate(cache: NodeCache?): RelationPartnerTemplate<*, *> {
        return component(cache).value.template(cache).value
    }

}