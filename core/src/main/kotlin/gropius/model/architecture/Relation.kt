package gropius.model.architecture

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import gropius.model.common.BaseNode
import gropius.model.template.BaseTemplate
import gropius.model.template.MutableTemplatedNode
import gropius.model.template.RelationTemplate
import gropius.model.user.permission.NodePermission
import io.github.graphglue.model.*
import org.springframework.data.neo4j.core.schema.CompositeProperty

@DomainNode
@GraphQLDescription(
    """A relation between RelationPartners (ComponentVersions and Interfaces).
    Relations are always directional.
    Relations can derive Interfaces from `end` to `start` if both `start` and `end` are ComponentVersions
    and the template of this Relation allows it.
    The template defines which RelationPartners are possible as `start` / `end`.
    For both start and end, if it is an Interface, it is possible to define the InterfaceParts this includes.
    Caution: This is **not** a supertype of IssueRelation.
    READ is granted if READ is granted on `start`.
    """
)
@Authorization(NodePermission.READ, allowFromRelated = ["start"])
class Relation(
    @property:GraphQLIgnore
    @CompositeProperty
    override val templatedFields: MutableMap<String, String>
) : BaseNode(), MutableTemplatedNode {

    companion object {
        const val START_PART = "START_PART"
        const val END_PART = "END_PART"
    }

    @NodeRelationship(BaseTemplate.USED_IN, Direction.INCOMING)
    @GraphQLDescription("The Template of this Relation.")
    @FilterProperty
    @OrderProperty
    override val template by NodeProperty<RelationTemplate>()

    @NodeRelationship(RelationPartner.INCOMING_RELATION, Direction.INCOMING)
    @GraphQLDescription("The end of this Relation.")
    @GraphQLNullable
    @FilterProperty
    @OrderProperty
    val end by NodeProperty<RelationPartner>()

    @NodeRelationship(RelationPartner.OUTGOING_RELATION, Direction.INCOMING)
    @GraphQLDescription("The start of this Relation.")
    @GraphQLNullable
    @FilterProperty
    @OrderProperty
    val start by NodeProperty<RelationPartner>()

    @NodeRelationship(START_PART, Direction.OUTGOING)
    @GraphQLDescription("If the start is an Interface, the parts of that Interface this Relation includes.")
    @FilterProperty
    val startParts by NodeSetProperty<InterfacePart>()

    @NodeRelationship(END_PART, Direction.OUTGOING)
    @GraphQLDescription("If the end is an Interface, the parts of that Interface this Relation includes.")
    @FilterProperty
    val endParts by NodeSetProperty<InterfacePart>()

    @NodeRelationship(InterfaceDefinition.VISIBLE_DERIVED_BY, Direction.INCOMING)
    @GraphQLDescription("InterfaceDefinition this Relation derives visible")
    @FilterProperty
    val derivesVisible by NodeSetProperty<InterfaceDefinition>()

    @NodeRelationship(InterfaceDefinition.INVISIBLE_DERIVED_BY, Direction.INCOMING)
    @GraphQLDescription("InterfaceDefinition this Relation derives invisible")
    @FilterProperty
    val derivesInvisible by NodeSetProperty<InterfaceDefinition>()
}