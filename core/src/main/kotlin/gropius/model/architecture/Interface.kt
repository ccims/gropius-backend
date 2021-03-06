package gropius.model.architecture

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.user.permission.NodePermission
import io.github.graphglue.model.*
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import gropius.model.template.BaseTemplate
import gropius.model.template.InterfaceTemplate
import gropius.model.template.MutableTemplatedNode
import org.springframework.data.annotation.Transient
import org.springframework.data.neo4j.core.schema.CompositeProperty

@DomainNode
@GraphQLDescription(
    """An interface which is part of a specific ComponentVersion.
    Its semantics depend on the InterfaceSpecification it is specified by, e.g. an Interface can represent a REST API.
    Can be used in Relations and affected by Issues.
    READ is granted if READ is granted on `component`.
    """
)
@Authorization(NodePermission.READ, allowFromRelated = ["component"])
class Interface(
    name: String,
    description: String,
    @property:GraphQLIgnore
    @CompositeProperty
    override val templatedFields: MutableMap<String, String>
) : RelationPartner(name, description), MutableTemplatedNode {

    companion object {
        const val COMPONENT = "COMPONENT"
        const val SPECIFICATION = "SPECIFICATION"
    }

    @NodeRelationship(BaseTemplate.USED_IN, Direction.INCOMING)
    @GraphQLDescription("The Template of this Interface.")
    @FilterProperty
    @delegate:Transient
    val template by NodeProperty<InterfaceTemplate>()

    @NodeRelationship(COMPONENT, Direction.OUTGOING)
    @GraphQLDescription("The ComponentVersion this Interface is part of.")
    @FilterProperty
    @delegate:Transient
    val component by NodeProperty<ComponentVersion>()

    @NodeRelationship(SPECIFICATION, Direction.OUTGOING)
    @GraphQLDescription("The InterfaceSpecification which specifies this Interface and thereby defines its semantics.")
    @FilterProperty
    @delegate:Transient
    val specification by NodeProperty<InterfaceSpecificationVersion>()
}