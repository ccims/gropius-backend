package gropius.service.architecture

import com.expediagroup.graphql.generator.scalars.ID
import gropius.authorization.GropiusAuthorizationContext
import gropius.dto.input.architecture.BulkCreateRelationInput
import gropius.dto.input.architecture.CreateRelationInput
import gropius.dto.input.architecture.UpdateRelationInput
import gropius.dto.input.common.DeleteNodeInput
import gropius.dto.input.ifPresent
import gropius.dto.input.isPresent
import gropius.model.architecture.*
import gropius.model.template.RelationTemplate
import gropius.model.user.permission.ComponentPermission
import gropius.model.user.permission.NodePermission
import gropius.repository.architecture.InterfacePartRepository
import gropius.repository.architecture.RelationPartnerRepository
import gropius.repository.architecture.RelationRepository
import gropius.repository.common.NodeRepository
import gropius.repository.findById
import gropius.repository.template.RelationTemplateRepository
import gropius.service.NodeBatchUpdateContext
import gropius.service.common.NodeService
import gropius.service.template.TemplatedNodeService
import io.github.graphglue.authorization.Permission
import org.springframework.stereotype.Service

/**
 * Service for [Relation]s. Provides functions to create, update and delete
 *
 * @param repository the associated repository used for CRUD functionality
 * @param relationPartnerRepository used to get start and end of a Relation by id
 * @param relationTemplateRepository used to get the [RelationTemplate] by id
 * @param templatedNodeService service used to update templatedFields
 * @param interfacePartRepository used to get [InterfacePart]s by id
 * @param nodeRepository used to save/delete any node
 */
@Service
class RelationService(
    repository: RelationRepository,
    private val relationPartnerRepository: RelationPartnerRepository,
    private val relationTemplateRepository: RelationTemplateRepository,
    private val templatedNodeService: TemplatedNodeService,
    private val interfacePartRepository: InterfacePartRepository,
    private val nodeRepository: NodeRepository
) : NodeService<Relation, RelationRepository>(repository) {

    /**
     * Creates a new [Relation] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines the [Relation]
     * @return the saved created [Relation]
     */
    suspend fun createRelation(
        authorizationContext: GropiusAuthorizationContext, input: CreateRelationInput
    ): Relation {
        input.validate()
        val start = relationPartnerRepository.findById(input.start)
        val end = relationPartnerRepository.findById(input.end)
        checkRelationAuthorization(start, end, authorizationContext)
        val template = relationTemplateRepository.findById(input.template)
        validateRelationStartAndEnd(start, end, template)
        val templatedFields = templatedNodeService.validateInitialTemplatedFields(template, input)
        val relation = Relation(templatedFields)
        relation.start().value = start
        relation.end().value = end
        input.startParts.ifPresent { relation.startParts() += getInterfaceParts(start, it) }
        input.endParts.ifPresent { relation.endParts() += getInterfaceParts(end, it) }
        relation.template().value = template
        val graphUpdater = ComponentGraphUpdater()
        graphUpdater.createRelation(relation)
        return graphUpdater.save(relation, nodeRepository)
    }

    /**
     * Creates multiple [Relation]s based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines the [Relation]s
     * @return the saved created [Relation]s
     */
    suspend fun bulkCreateRelation(
        authorizationContext: GropiusAuthorizationContext, input: BulkCreateRelationInput
    ): List<Relation> {
        input.validate()
        val relations = input.relations.map { createRelation(authorizationContext, it) }
        return relations
    }

    /**
     * Checks that the user has the permission to relate from [start] and relate to [end]
     *
     * @param start the start of the [Relation]
     * @param end the end of the [Relation]
     * @param authorizationContext used to check for the required permission
     * @throws IllegalArgumentException if the user is missing any of the required permissions
     */
    private suspend fun checkRelationAuthorization(
        start: RelationPartner,
        end: RelationPartner,
        authorizationContext: GropiusAuthorizationContext
    ) {
        checkPermission(
            start,
            Permission(ComponentPermission.RELATE_FROM_COMPONENT, authorizationContext),
            "use the specified start in Relations"
        )
        checkPermission(
            end,
            Permission(NodePermission.READ, authorizationContext),
            "use the specified end in Relations"
        )
    }

    /**
     * Helper used to get [InterfacePart]s based on a list of ids for a [RelationPartner]
     * The [relationPartner] must be a [Interface], if a non-empty list of ids is provided
     * Each [InterfacePart] must be active on the [InterfaceSpecificationVersion] related to the [Interface]
     *
     * @param relationPartner the start/end of the relation
     * @param partIds the ids of the [InterfacePart]s
     * @return the list of queried [InterfacePart]s from the [partIds], empty list if no ids are provided
     * @throws IllegalArgumentException if any validation fails
     */
    private suspend fun getInterfaceParts(
        relationPartner: RelationPartner, partIds: List<ID>
    ): Set<InterfacePart> {
        if (partIds.isNotEmpty()) {
            if (relationPartner !is Interface) {
                throw IllegalArgumentException("InterfaceParts can only be provided if the side of the Relation uses an Interface")
            }
            return partIds.map { interfacePartRepository.findById(it) }.toSet()
        }
        return emptySet()
    }

    /**
     * Validates that a start, end and relationPartner combination is valid
     * This includes checking that a relation is not created in the context of the same [ComponentVersion]
     *
     * @param start the start of the Relation
     * @param end the end of the Relation
     * @param template the template of the Relation
     * @throws IllegalArgumentException if the combination is invalid
     */
    private suspend fun validateRelationStartAndEnd(
        start: RelationPartner, end: RelationPartner, template: RelationTemplate
    ) {
        val startTemplate = start.relationPartnerTemplate()
        val endTemplate = end.relationPartnerTemplate()
        if (template.relationConditions().none { startTemplate in it.from() && endTemplate in it.to() }) {
            throw IllegalArgumentException("No RelationCondition allows the chosen relationTemplate & start & end combination")
        }
        val startComponentVersion = if (start is Interface) {
            start.interfaceDefinition().value.componentVersion().value
        } else {
            start as ComponentVersion
        }
        val endComponentVersion = if (end is Interface) {
            end.interfaceDefinition().value.componentVersion().value
        } else {
            end as ComponentVersion
        }
        if (startComponentVersion == endComponentVersion) {
            throw IllegalArgumentException("A Relation cannot be created in the context of the same ComponentVersion")
        }
    }

    /**
     * Updates a [Relation] based on the provided [input]
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [Relation] to update and how
     * @return the updated [Relation]
     */
    suspend fun updateRelation(
        authorizationContext: GropiusAuthorizationContext, input: UpdateRelationInput
    ): Relation {
        input.validate()
        val relation = repository.findById(input.id)
        checkPermission(
            relation.start().value,
            Permission(ComponentPermission.RELATE_FROM_COMPONENT, authorizationContext),
            "update the Relation"
        )
        val updateContext = NodeBatchUpdateContext()
        updateRelationTemplate(input, relation, updateContext)
        input.addedStartParts.ifPresent { relation.startParts() += getInterfaceParts(relation.start().value, it) }
        input.addedEndParts.ifPresent { relation.endParts() += getInterfaceParts(relation.end().value, it) }
        input.removedStartParts.ifPresent { relation.startParts() -= getInterfaceParts(relation.start().value, it) }
        input.removedEndParts.ifPresent { relation.endParts() -= getInterfaceParts(relation.end().value, it) }
        templatedNodeService.updateTemplatedFields(relation, input, input.template.isPresent)
        return updateContext.save(relation, nodeRepository)
    }

    /**
     * Updates the template of an [relation], if provided via the [input]
     * Does not check the authorization status
     *
     * @param input defines how to update the template
     * @param relation the [Relation] to update
     * @param updateContext the context used to save the updated nodes
     * @return the updated nodes to save
     */
    private suspend fun updateRelationTemplate(
        input: UpdateRelationInput,
        relation: Relation,
        updateContext: NodeBatchUpdateContext
    ) {
        input.template.ifPresent { templateId ->
            val template = relationTemplateRepository.findById(templateId)
            validateRelationStartAndEnd(relation.start().value, relation.end().value, template)
            relation.template().value = template
            val graphUpdater = ComponentGraphUpdater(updateContext)
            graphUpdater.updateRelationTemplate(relation)
        }
    }

    /**
     * Deletes a [Relation] by id
     * Checks the authorization status
     *
     * @param authorizationContext used to check for the required permission
     * @param input defines which [Relation] to delete
     */
    suspend fun deleteRelation(
        authorizationContext: GropiusAuthorizationContext, input: DeleteNodeInput
    ) {
        input.validate()
        val relation = repository.findById(input.id)
        checkPermission(
            relation.start().value,
            Permission(ComponentPermission.RELATE_FROM_COMPONENT, authorizationContext),
            "delete the Relation"
        )
        val graphUpdater = ComponentGraphUpdater()
        graphUpdater.deleteRelation(relation)
        graphUpdater.save(nodeRepository)
    }

}