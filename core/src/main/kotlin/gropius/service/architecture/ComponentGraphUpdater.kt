package gropius.service.architecture

import gropius.model.architecture.*
import gropius.model.template.ComponentTemplate
import gropius.model.template.InterfaceSpecificationDerivationCondition
import gropius.model.template.InterfaceSpecificationTemplate
import gropius.model.template.RelationPartnerTemplate
import gropius.service.NodeBatchUpdateContext
import gropius.service.NodeBatchUpdater
import gropius.service.issue.IssueAggregationUpdater

/**
 * Helper class to handle anything InterfaceSpecification derivation related
 * When used to update / delete nodes, also handles updating/deleting of the following kinds of nodes.
 * - [Component]
 * - [ComponentVersion]
 * - [InterfaceSpecification]
 * - [InterfaceSpecificationVersion]
 * - [InterfacePart]
 * - [InterfaceDefinition]
 * - [Interface]
 * - [Relation]
 * - [IntraComponentDependencyParticipant]
 * - [IntraComponentDependencySpecification]
 * After using, nodes from [deletedNodes] must be deleted, and nodes from [updatedNodes] must be saved
 *
 * @param updateContext the [NodeBatchUpdater] to use for updating/deleting nodes
 */
class ComponentGraphUpdater(updateContext: NodeBatchUpdater = NodeBatchUpdateContext()) : GraphUpdater(updateContext) {

    /**
     * Helper for updating aggregated issues
     */
    val issueAggregationUpdater = IssueAggregationUpdater(updateContext)

    /**
     * Deletes a [Component]
     *
     * @param component the [Component] to delete
     */
    suspend fun deleteComponent(component: Component) {
        cache.add(component)
        deletedNodes += component
        component.interfaceSpecifications(cache).forEach {
            deleteInterfaceSpecification(it)
        }
        val versions = component.versions(cache).toList()
        versions.forEach {
            deleteComponentVersion(it)
        }
    }

    /**
     * Deletes a [ComponentVersion]
     *
     * @param componentVersion the [ComponentVersion] to delete
     */
    suspend fun deleteComponentVersion(componentVersion: ComponentVersion) {
        cache.add(componentVersion)
        lockIncomingAndOutgoingRelationPartners(componentVersion)
        deletedNodes += componentVersion
        componentVersion.intraComponentDependencySpecifications(cache).forEach { deleteIntraComponentDependencySpecification(it) }
        componentVersion.interfaceDefinitions(cache).toSet().forEach { deleteInterfaceDefinition(it) }
        deletedNodes += componentVersion.layouts(cache)
        val relations = componentVersion.outgoingRelations(cache) + componentVersion.incomingRelations(cache)
        relations.forEach {
            deleteRelation(it)
        }
        issueAggregationUpdater.deletedComponentVersion(componentVersion)
    }

    /**
     * Deletes a [Relation]
     *
     * @param relation the [Relation] to delete
     */
    suspend fun deleteRelation(relation: Relation) {
        if (relation !in deletedNodes) {
            cache.add(relation)
            lockIncomingAndOutgoingRelationPartners(relation.start(cache).value)
            lockIncomingAndOutgoingRelationPartners(relation.end(cache).value)
            deletedNodes += relation
            deletedNodes += relation.layouts(cache)
            val startNode = relation.start(cache).value
            startNode.outgoingRelations(cache) -= relation
            relation.end(cache).value.incomingRelations(cache) -= relation
            relation.derivesVisible(cache).forEach {
                it.visibleDerivedBy(cache) -= relation
            }
            relation.derivesInvisible(cache).forEach {
                it.invisibleDerivedBy(cache) -= relation
            }
            if (startNode is ComponentVersion) {
                validateComponentVersion(startNode)
            }
        }
    }

    /**
     * Deletes an [InterfaceSpecification]
     *
     * @param interfaceSpecification the [InterfaceSpecification] to delete
     */
    suspend fun deleteInterfaceSpecification(interfaceSpecification: InterfaceSpecification) {
        cache.add(interfaceSpecification)
        deletedNodes += interfaceSpecification
        interfaceSpecification.versions(cache).forEach {
            deleteInterfaceSpecificationVersion(it)
        }
    }

    /**
     * Deletes an [InterfaceSpecificationVersion]
     *
     * @param interfaceSpecificationVersion the [InterfaceSpecificationVersion] to delete
     */
    suspend fun deleteInterfaceSpecificationVersion(interfaceSpecificationVersion: InterfaceSpecificationVersion) {
        cache.add(interfaceSpecificationVersion)
        deletedNodes += interfaceSpecificationVersion
        deletedNodes += interfaceSpecificationVersion.parts(cache)
        interfaceSpecificationVersion.interfaceDefinitions(cache).toSet().forEach {
            deleteInterfaceDefinition(it)
        }
    }

    /**
     * Called after the template of a [Component] has been updated
     * Validates [InterfaceDefinition]s and [Relation]s
     * Does NOT handle updating SubTemplates
     *
     * @param component the [Component] of which the template has been updated
     */
    suspend fun updateComponentTemplate(component: Component) {
        cache.add(component)
        component.versions(cache).forEach {
            lockIncomingAndOutgoingRelationPartners(it)
        }
        component.versions(cache).forEach { componentVersion ->
            val updatedDefinitions = componentVersion.incomingRelations(cache).flatMap { relation ->
                addForUpdatedRelation(relation)
            }.toSet()
            addForUpdatedComponentVersion(componentVersion, updatedDefinitions)
        }
        val template = component.template(cache).value
        component.versions(cache).forEach { componentVersion ->
            val updatedInterfaceSpecificationVersions = mutableSetOf<InterfaceSpecificationVersion>()
            componentVersion.interfaceDefinitions(cache).toSet().forEach {
                if (validateInterfaceDefinitionAfterTemplateUpdate(it, template)) {
                    updatedInterfaceSpecificationVersions += it.interfaceSpecificationVersion(cache).value
                }
            }
            validateRelationsAfterTemplateUpdate(componentVersion)
            validateRelatedComponentVersions(componentVersion, updatedInterfaceSpecificationVersions)
        }
    }

    /**
     * Called after the template of an [InterfaceSpecification]
     * Validates [InterfaceDefinition]s and [Relation]s
     * oes NOT handle updating SubTemplates
     *
     * @param interfaceSpecification the [InterfaceSpecification] of which the template has been updated
     */
    suspend fun updateInterfaceSpecificationTemplate(interfaceSpecification: InterfaceSpecification) {
        cache.add(interfaceSpecification)
        val definitions = interfaceSpecification.versions(cache).flatMap { it.interfaceDefinitions(cache) }
        definitions.mapNotNull { it.visibleInterface(cache).value }.forEach {
            lockIncomingAndOutgoingRelationPartners(it)
        }
        for (definition in definitions) {
            val componentVersion = definition.componentVersion(cache).value
            val template = componentVersion.component(cache).value.template(cache).value
            if (validateInterfaceDefinitionAfterTemplateUpdate(definition, template)) {
                validateRelatedComponentVersions(
                    componentVersion, setOf(definition.interfaceSpecificationVersion(cache).value)
                )
            }
        }
    }

    /**
     * Removes an [InterfaceSpecificationVersion] from a [ComponentVersion]
     * Updates the [InterfaceDefinition] and deletes it if necessary
     *
     * @param interfaceSpecificationVersion the [InterfaceSpecificationVersion] to remove
     * @param componentVersion the [ComponentVersion] from which to remove [interfaceSpecificationVersion]
     * @param visible if `true`, [interfaceSpecificationVersion] should no longer be visibleSelfDefined on [componentVersion]
     * @param invisible if `true`, [interfaceSpecificationVersion] should no longer be invisibleSelfDefined on [componentVersion]
     */
    suspend fun removeInterfaceSpecificationVersionFromComponentVersion(
        interfaceSpecificationVersion: InterfaceSpecificationVersion,
        componentVersion: ComponentVersion,
        visible: Boolean,
        invisible: Boolean
    ) {
        cache.add(interfaceSpecificationVersion)
        cache.add(componentVersion)
        lockIncomingAndOutgoingRelationPartners(componentVersion)
        val definition = componentVersion.interfaceDefinitions(cache).firstOrNull {
            it.interfaceSpecificationVersion(cache).value == interfaceSpecificationVersion
        }
        if (definition != null) {
            if (visible) {
                definition.visibleSelfDefined = false
            }
            if (invisible) {
                definition.invisibleSelfDefined = false
            }
            internalUpdatedNodes += definition
            handleUpdatedInterfaceDefinition(definition)
            validateRelatedComponentVersions(componentVersion, setOf(interfaceSpecificationVersion))
        }
    }

    /**
     * Adds an [InterfaceSpecificationVersion] to a [ComponentVersion]
     * Updates the [InterfaceDefinition] and creates it if necessary
     *
     * @param interfaceSpecificationVersion the [InterfaceSpecificationVersion] to add
     * @param componentVersion the [ComponentVersion] to which to add [interfaceSpecificationVersion]
     * @param visible if `true`, [interfaceSpecificationVersion] should be visibleSelfDefined on [componentVersion] afterwards
     * @param invisible if `true`, [interfaceSpecificationVersion] should be invisibleSelfDefined on [componentVersion] afterwards
     */
    suspend fun addInterfaceSpecificationVersionToComponentVersion(
        interfaceSpecificationVersion: InterfaceSpecificationVersion,
        componentVersion: ComponentVersion,
        visible: Boolean,
        invisible: Boolean
    ) {
        cache.add(interfaceSpecificationVersion)
        cache.add(componentVersion)
        lockIncomingAndOutgoingRelationPartners(componentVersion)
        val definition = getOrCreateInterfaceDefinition(componentVersion, interfaceSpecificationVersion)
        if (visible) {
            definition.visibleSelfDefined = true
        }
        if (invisible) {
            definition.invisibleSelfDefined = true
        }
        internalUpdatedNodes += definition
        handleUpdatedInterfaceDefinition(definition)
        addForUpdatedComponentVersion(componentVersion, setOf(definition))
    }

    /**
     * Called after a [Relation] has been created
     * Handles the derivation of [InterfaceSpecificationVersion]s
     *
     * @param relation the [Relation] which was created
     */
    suspend fun createRelation(relation: Relation) {
        cache.add(relation)
        lockIncomingAndOutgoingRelationPartners(relation.start(cache).value)
        lockIncomingAndOutgoingRelationPartners(relation.end(cache).value)
        addForUpdatedRelationTransitive(relation)
    }

    /**
     * Called after a template of a [Relation] has been updated
     * Updates the derivation of [InterfaceSpecificationVersion]s
     * Does not validate if the [Relation] can still exist
     *
     * @param relation the [Relation] of which the template has been updated
     */
    suspend fun updateRelationTemplate(relation: Relation) {
        cache.add(relation)
        lockIncomingAndOutgoingRelationPartners(relation.start(cache).value)
        lockIncomingAndOutgoingRelationPartners(relation.end(cache).value)
        addForUpdatedRelationTransitive(relation)
        val startNode = relation.start(cache).value
        if (startNode is ComponentVersion) {
            validateComponentVersion(startNode)
        }
    }

    /**
     * Called after the template of a [Component] or an [InterfaceSpecification] was updated
     * Validates that [interfaceDefinition] can still be present on the associated [ComponentVersion],
     * and if not, removes it (maybe partially)
     * Does NOT perform any validation of the graph
     *
     * @param interfaceDefinition the [InterfaceDefinition] to validate
     * @param template the template of the [Component] associated with the [ComponentVersion] associated with [interfaceDefinition]
     * @return `true` if the [interfaceDefinition] was updated, and a graph update should be triggered
     */
    private suspend fun validateInterfaceDefinitionAfterTemplateUpdate(
        interfaceDefinition: InterfaceDefinition, template: ComponentTemplate
    ): Boolean {
        val interfaceSpecificationVersion = interfaceDefinition.interfaceSpecificationVersion(cache).value
        val interfaceSpecification = interfaceSpecificationVersion.interfaceSpecification(cache).value
        val specificationTemplate = interfaceSpecification.template(cache).value
        var updated = false
        if (template !in specificationTemplate.canBeVisibleOnComponents(cache)) {
            updated = true
            interfaceDefinition.visibleSelfDefined = false
            interfaceDefinition.visibleDerivedBy(cache).clear()
        }
        if (template !in specificationTemplate.canBeInvisibleOnComponents(cache)) {
            updated = true
            interfaceDefinition.invisibleSelfDefined = false
            interfaceDefinition.invisibleDerivedBy(cache).clear()
        }
        if (updated) {
            handleUpdatedInterfaceDefinition(interfaceDefinition)
        }
        val visibleInterface = interfaceDefinition.visibleInterface(cache).value
        if (visibleInterface != null) {
            validateRelationsAfterTemplateUpdate(visibleInterface)
        }
        return updated
    }

    /**
     * Validates both incoming and outgoing relations of a [RelationPartner] after the template of said
     * [RelationPartner] has been updated
     * Deletes [Relation]s if necessary, and handles updates to [InterfaceSpecificationVersion] derivation
     * Does NOT validate [InterfaceSpecificationVersion] derivation independent of [Relation] deletion
     *
     * @param relationPartner the [RelationPartner] of which the template has been updated
     */
    private suspend fun validateRelationsAfterTemplateUpdate(
        relationPartner: RelationPartner
    ) {
        val template = relationPartner.relationPartnerTemplate(cache)
        relationPartner.incomingRelations(cache).toSet().forEach {
            validateRelation(it, it.start(cache).value.relationPartnerTemplate(cache), template)
        }
        relationPartner.outgoingRelations(cache).toSet().forEach {
            validateRelation(it, template, it.end(cache).value.relationPartnerTemplate(cache))
        }
    }

    /**
     * Validates if a [Relation] an still exist after the template of either its start or end has been updated
     * If not, deletes the relation and handles the resulting graph update
     *
     * @param relation the [Relation] to validate
     * @param startTemplate the template of its start
     * @param endTemplate the template of its end
     */
    private suspend fun validateRelation(
        relation: Relation, startTemplate: RelationPartnerTemplate<*, *>, endTemplate: RelationPartnerTemplate<*, *>
    ) {
        if (relation.template(cache).value.relationConditions(cache)
                .none { startTemplate in it.from(cache) && endTemplate in it.to(cache) }
        ) {
            deleteRelation(relation)
        }
    }

    /**
     * Handles the graph update ([InterfaceSpecificationVersion] derivation) after a [Relation] has been
     * created / updated
     * Does NOT validate if the [Relation] can still exist
     * Only ensures derivation of additional [InterfaceSpecificationVersion], but does not validate that no longer
     * derived [InterfaceSpecificationVersion] are updated/removed
     *
     * @param relation the created/updated [Relation]
     */
    private suspend fun addForUpdatedRelationTransitive(relation: Relation) {
        val updatedDefinitions = addForUpdatedRelation(relation)
        val startNode = relation.start(cache).value
        if (startNode is ComponentVersion) {
            addForUpdatedComponentVersion(startNode, updatedDefinitions)
        }
    }

    /**
     * Updates the [InterfaceSpecificationVersion] derivation for a single [Relation]
     * Does NOT perform a graph update
     * Only derives additional [InterfaceSpecificationVersion], but does not ensure that all already existing
     * [InterfaceDefinition] on the end of the relation are still derived
     *
     * @param relation the [Relation] for which to derive [InterfaceSpecificationVersion]s
     * @param endDefinitions if provided, a set of [InterfaceDefinition]s on the end of the relation to
     *   maybe derive. If not provided, all are evaluated
     * @return the derived [InterfaceDefinition]s on the end node, can be used to perform a transitive graph update
     */
    private suspend fun addForUpdatedRelation(
        relation: Relation, endDefinitions: Set<InterfaceDefinition>? = null
    ): Set<InterfaceDefinition> {
        if (relation in deletedNodes) {
            return emptySet()
        }
        val startNode = relation.start(cache).value
        val endNode = relation.end(cache).value
        if (startNode !is ComponentVersion || endNode !is ComponentVersion) {
            return emptySet()
        }
        val startTemplate = startNode.relationPartnerTemplate(cache)
        val endTemplate = endNode.relationPartnerTemplate(cache)
        val derivationConditions =
            mutableMapOf<InterfaceSpecificationTemplate, MutableList<InterfaceSpecificationDerivationCondition>>()
        relation.template(cache).value.relationConditions(cache).forEach { relationCondition ->
            if (startTemplate in relationCondition.from(cache) && endTemplate in relationCondition.to(cache)) {
                relationCondition.interfaceSpecificationDerivationConditions(cache).forEach { condition ->
                    condition.derivableInterfaceSpecifications(cache).forEach {
                        derivationConditions.computeIfAbsent(it) { mutableListOf() }.add(condition)
                    }
                }
            }
        }
        return addForUpdatedRelation(endDefinitions, startNode, derivationConditions, startTemplate, endNode, relation)
    }

    /**
     * Updates the [InterfaceSpecificationVersion] derivation for a single [Relation]
     * Does NOT perform a graph update
     * Only derives additional [InterfaceSpecificationVersion], but does not ensure that all already existing
     * [InterfaceDefinition] on the end of the relation are still derived
     *
     * @param relation the [Relation] for which to derive [InterfaceSpecificationVersion]s
     * @param endDefinitions if provided, a set of [InterfaceDefinition]s on the end of the relation to
     *   maybe derive. If not provided, all are evaluated
     * @param startNode the start of the [relation]
     * @param endNode the end of the [relation]
     * @param derivationConditionsByTemplate mapping from [InterfaceSpecification] template
     *   to [InterfaceSpecificationDerivationCondition]
     * @param startTemplate template of [startNode]
     * @return the derived [InterfaceDefinition]s on the end node, can be used to perform a transitive graph update
     */
    private suspend fun addForUpdatedRelation(
        endDefinitions: Set<InterfaceDefinition>?,
        startNode: ComponentVersion,
        derivationConditionsByTemplate: MutableMap<InterfaceSpecificationTemplate, MutableList<InterfaceSpecificationDerivationCondition>>,
        startTemplate: RelationPartnerTemplate<*, *>,
        endNode: ComponentVersion,
        relation: Relation
    ): MutableSet<InterfaceDefinition> {
        val updatedDefinitions = mutableSetOf<InterfaceDefinition>()
        for (definition in endDefinitions ?: endNode.interfaceDefinitions(cache)) {
            val version = definition.interfaceSpecificationVersion(cache).value
            val interfaceSpecification = version.interfaceSpecification(cache).value
            val interfaceSpecificationTemplate = interfaceSpecification.template(cache).value
            val conditions = derivationConditionsByTemplate[interfaceSpecificationTemplate] ?: emptySet()
            val canBeVisible = startTemplate in interfaceSpecificationTemplate.canBeVisibleOnComponents(cache)
            val canBeInvisible = startTemplate in interfaceSpecificationTemplate.canBeInvisibleOnComponents(cache)
            conditions.filter {
                var derives = it.derivesVisibleSelfDefined && definition.visibleSelfDefined
                derives = derives || it.derivesInvisibleSelfDefined && definition.invisibleSelfDefined
                derives = derives || it.derivesVisibleDerived && definition.visibleDerivedBy(cache).isNotEmpty()
                derives = derives || it.derivesInvisibleDerived && definition.invisibleDerivedBy(cache).isNotEmpty()
                derives
            }.forEach {
                updatedDefinitions += deriveInterfaceSpecificationVersion(
                    it, canBeVisible, canBeInvisible, startNode, version, relation
                )
            }
        }
        return updatedDefinitions
    }

    /**
     * Derives an [interfaceSpecificationVersion] via a [relation] to a [componentVersion]
     * Does not perform a graph update
     *
     * @param condition the [InterfaceSpecificationDerivationCondition] allowing the derivation of the
     *   [interfaceSpecificationVersion]
     * @param canBeInvisible if `true`, [interfaceSpecificationVersion] can be visible on [componentVersion]
     * @param canBeInvisible if `true`, [interfaceSpecificationVersion] can be invisible on [componentVersion]
     * @param componentVersion the [ComponentVersion] where to possibly create/update the [InterfaceDefinition]
     * @param interfaceSpecificationVersion the [InterfaceSpecificationVersion] to possibly add to [componentVersion]
     * @param relation the [Relation] causing the [InterfaceSpecificationVersion] derivation
     * @return updated/created [InterfaceDefinition]s
     */
    private suspend fun deriveInterfaceSpecificationVersion(
        condition: InterfaceSpecificationDerivationCondition,
        canBeVisible: Boolean,
        canBeInvisible: Boolean,
        componentVersion: ComponentVersion,
        interfaceSpecificationVersion: InterfaceSpecificationVersion,
        relation: Relation
    ): Set<InterfaceDefinition> {
        val updatedDefinitions = mutableSetOf<InterfaceDefinition>()
        if (condition.isVisibleDerived && canBeVisible || condition.isInvisibleDerived && canBeInvisible) {
            val targetDefinition = getOrCreateInterfaceDefinition(
                componentVersion, interfaceSpecificationVersion
            )
            if (condition.isVisibleDerived && targetDefinition.visibleDerivedBy(cache).add(relation)) {
                internalUpdatedNodes += targetDefinition
                updatedDefinitions += targetDefinition
                if (!targetDefinition.visibleSelfDefined && targetDefinition.visibleDerivedBy(cache).size == 1) {
                    handleUpdatedInterfaceDefinition(targetDefinition)
                }
            }
            if (condition.isInvisibleDerived && targetDefinition.invisibleDerivedBy(cache).add(relation)) {
                internalUpdatedNodes += targetDefinition
                updatedDefinitions += targetDefinition
            }
        }
        return updatedDefinitions
    }

    /**
     * derives [InterfaceSpecificationVersion] for a specified [ComponentVersion]
     * derives via all incoming [Relation]
     * Performs a transitive graph update
     *
     * @param componentVersion the [ComponentVersion] from which to derive  [InterfaceSpecificationVersion]s
     * @param updatedDefinitions if provided, the [InterfaceDefinition]s on [componentVersion] to derive
     *   if not provided, all are derived
     */
    private suspend fun addForUpdatedComponentVersion(
        componentVersion: ComponentVersion, updatedDefinitions: Set<InterfaceDefinition>
    ) {
        val updatedComponentVersions = componentVersion.incomingRelations(cache).flatMap {
            addForUpdatedRelation(it, updatedDefinitions)
        }.groupBy { it.componentVersion(cache).value }.mapValues { it.value.toSet() }

        updatedComponentVersions.forEach { (componentVersion, definitions) ->
            addForUpdatedComponentVersion(componentVersion, definitions)
        }
    }

    /**
     * Gets or creates if non-existing a [InterfaceDefinition] for a [ComponentVersion] and an [InterfaceSpecificationVersion]
     * If created, adds it to the updated nodes
     *
     * @param componentVersion hosts the returned [InterfaceDefinition]
     * @param interfaceSpecificationVersion associated with the returned [InterfaceDefinition]
     * @return the [InterfaceDefinition] associated with [componentVersion] and [interfaceSpecificationVersion]
     */
    private suspend fun getOrCreateInterfaceDefinition(
        componentVersion: ComponentVersion, interfaceSpecificationVersion: InterfaceSpecificationVersion
    ): InterfaceDefinition {
        return componentVersion.interfaceDefinitions(cache).firstOrNull {
            it.interfaceSpecificationVersion(cache).value == interfaceSpecificationVersion
        } ?: run {
            val newDefinition = InterfaceDefinition(
                visibleSelfDefined = false,
                invisibleSelfDefined = false,
            )
            newDefinition.interfaceSpecificationVersion(cache).value = interfaceSpecificationVersion
            newDefinition.componentVersion(cache).value = componentVersion
            componentVersion.interfaceDefinitions(cache) += newDefinition
            interfaceSpecificationVersion.interfaceDefinitions(cache) += newDefinition
            internalUpdatedNodes += newDefinition
            newDefinition
        }
    }

    /**
     * Deletes an [InterfaceDefinition]
     *
     * @param definition the [InterfaceDefinition] to delete
     */
    private suspend fun deleteInterfaceDefinition(definition: InterfaceDefinition) {
        definition.visibleSelfDefined = false
        definition.invisibleSelfDefined = false
        definition.visibleDerivedBy(cache).clear()
        definition.invisibleDerivedBy(cache).clear()
        handleUpdatedInterfaceDefinition(definition)
    }

    /**
     * Handles the update of an [InterfaceDefinition].
     * Creates / deletes the associated [Interface] if necessary
     * Deletes the provided [InterfaceDefinition] if necessary
     *
     * @param definition the [InterfaceDefinition] which was updated
     */
    private suspend fun handleUpdatedInterfaceDefinition(definition: InterfaceDefinition) {
        internalUpdatedNodes += definition
        if (definition.visibleSelfDefined || definition.visibleDerivedBy(cache).isNotEmpty()) {
            if (definition.visibleInterface(cache).value == null) {
                createInterface(definition)
            }
        }
        if (!definition.visibleSelfDefined && definition.visibleDerivedBy(cache).isEmpty()) {
            val visibleInterface = definition.visibleInterface(cache).value
            if (visibleInterface != null) {
                deleteInterface(visibleInterface)
                definition.visibleInterface(cache).value = null
                issueAggregationUpdater.deletedInterface(visibleInterface)
            }
            if (!definition.invisibleSelfDefined && definition.invisibleDerivedBy(cache).isEmpty()) {
                deletedNodes += definition
                definition.componentVersion(cache).value.interfaceDefinitions(cache) -= definition
                definition.interfaceSpecificationVersion(cache).value.interfaceDefinitions(cache) -= definition
            }
        }
    }

    /**
     * Creates an [Interface] for an [InterfaceDefinition], which has become visible
     *
     * @param definition the [InterfaceDefinition] for which to create an [Interface]
     */
    private suspend fun createInterface(definition: InterfaceDefinition) {
        val specificationVersion = definition.interfaceSpecificationVersion(cache).value
        val template = specificationVersion.interfaceSpecification(cache).value.template(cache).value
        val newInterface = Interface()
        internalUpdatedNodes += definition
        definition.visibleInterface(cache).value = newInterface
        newInterface.interfaceDefinition(cache).value = definition
        issueAggregationUpdater.createdInterface(newInterface)
    }

    /**
     * Deletes an [Interface]
     * Handles deletion of associated [IntraComponentDependencyParticipant]s and, if necessary,
     * [IntraComponentDependencySpecification]s
     *
     * @param node the [Interface] to delete
     */
    private suspend fun deleteInterface(node: Interface) {
        deletedNodes += node
        deletedNodes += node.incomingRelations(cache)
        deletedNodes += node.outgoingRelations(cache)
        deletedNodes += node.layouts(cache)
        val intraComponentDependencyParticipants = node.intraComponentDependencyParticipants(cache)
        deletedNodes += intraComponentDependencyParticipants
        for (participant in intraComponentDependencyParticipants) {
            participant.usedAsIncomingAt(cache).value?.let {
                it.incomingParticipants(cache) -= participant
                if (it.incomingParticipants(cache).isEmpty()) {
                    deleteIntraComponentDependencySpecification(it)
                }
            }
            participant.usedAsOutgoingAt(cache).value?.let {
                it.outgoingParticipants(cache) -= participant
                if (it.outgoingParticipants(cache).isEmpty()) {
                    deleteIntraComponentDependencySpecification(it)
                }
            }
        }
    }

    /**
     * Deletes a [IntraComponentDependencySpecification] and all its associated [IntraComponentDependencyParticipant]s
     *
     * @param node the [IntraComponentDependencySpecification] to delete
     */
    private suspend fun deleteIntraComponentDependencySpecification(node: IntraComponentDependencySpecification) {
        deletedNodes += node
        deletedNodes += node.incomingParticipants(cache)
        deletedNodes += node.outgoingParticipants(cache)
    }

    /**
     * Validates [InterfaceDefinition]s on [ComponentVersion]s incoming-related to the provided [componentVersion]
     * Removes / updates those if [InterfaceSpecificationVersion]s are no longer derived
     * Performs a graph update
     *
     * @param componentVersion used to get incoming-related [ComponentVersion]s to validate [InterfaceDefinition]s on
     * @param endInterfaceSpecificationVersions used to identify [InterfaceDefinition] to validate, if not provided
     *   all are validated
     */
    private suspend fun validateRelatedComponentVersions(
        componentVersion: ComponentVersion,
        endInterfaceSpecificationVersions: Set<InterfaceSpecificationVersion>? = null
    ) {
        for (relation in componentVersion.incomingRelations(cache)) {
            val startNode = relation.start(cache).value
            if (startNode is ComponentVersion) {
                if (endInterfaceSpecificationVersions != null) {
                    validateComponentVersion(startNode, endInterfaceSpecificationVersions)
                } else {
                    validateComponentVersion(componentVersion)
                }
            }
        }
    }

    /**
     * Validates [InterfaceDefinition]s on [componentVersion]
     * Removes / updates those if [InterfaceSpecificationVersion]s are no longer derived
     * Performs a graph update
     *
     * @param componentVersion hosting [InterfaceDefinition]s to validate
     * @param endInterfaceSpecificationVersions used to identify [InterfaceDefinition] to validate, if not provided
     *   all are validated
     */
    private suspend fun validateComponentVersion(
        componentVersion: ComponentVersion,
        endInterfaceSpecificationVersions: Set<InterfaceSpecificationVersion>? = null
    ) {
        if (endInterfaceSpecificationVersions != null) {
            validateInterfaceDefinitionsOnComponentVersion(componentVersion,
                componentVersion.interfaceDefinitions(cache)
                    .filter { it.interfaceSpecificationVersion(cache).value in endInterfaceSpecificationVersions }
                    .toSet())
        } else {
            validateInterfaceDefinitionsOnComponentVersion(componentVersion)
        }

    }

    /**
     * Validates [InterfaceDefinition]s on a [ComponentVersion]
     * Removes / updates those if [InterfaceSpecificationVersion]s are no longer derived
     * Uses [CircularDerivationChecker] to remove despite circular derivation
     * Performs a graph update
     *
     * @param componentVersion hosting [InterfaceDefinition]s to validate
     * @param endDefinitions if provided, the [InterfaceDefinition]s to validate, otherwise all are validated
     */
    private suspend fun validateInterfaceDefinitionsOnComponentVersion(
        componentVersion: ComponentVersion, endDefinitions: Set<InterfaceDefinition>? = null
    ) {
        val nextInterfaceSpecificationVersions = mutableSetOf<InterfaceSpecificationVersion>()
        var anyUpdated = false
        for (definition in endDefinitions ?: componentVersion.interfaceDefinitions(cache).toSet()) {
            val circularDerivationChecker = CircularDerivationChecker(definition)
            var updated = false
            if (!circularDerivationChecker.checkIsStillDerived(true)) {
                definition.visibleDerivedBy(cache).clear()
                updated = true
            }
            if (!circularDerivationChecker.checkIsStillDerived(false)) {
                definition.invisibleDerivedBy(cache).clear()
                updated = true
            }
            if (updated) {
                nextInterfaceSpecificationVersions += definition.interfaceSpecificationVersion(cache).value
                handleUpdatedInterfaceDefinition(definition)
            }
            anyUpdated = anyUpdated || updated
        }
        if (anyUpdated) {
            validateRelatedComponentVersions(componentVersion, nextInterfaceSpecificationVersions)
        }
    }

    /**
     * Helper to validate if the [interfaceDefinition] is still visible/invisible derived
     * As [InterfaceSpecificationVersion] can be derived in a circle, this helper maintains a set of
     * already visited nodes to identify circles
     * A [InterfaceSpecificationVersion] is only derived if a path the origial source is identified
     *
     * @param interfaceDefinition the [InterfaceDefinition] to validate
     */
    inner class CircularDerivationChecker(
        private val interfaceDefinition: InterfaceDefinition
    ) {
        /**
         * already visited [ComponentVersion] for visible derivation
         */
        private val checkedVisibleComponentVersions: MutableSet<ComponentVersion> = mutableSetOf()

        /**
         * already visited [ComponentVersion] for invisible derivation
         */
        private val checkedInvisibleComponentVersions: MutableSet<ComponentVersion> = mutableSetOf()

        /**
         * [InterfaceSpecificationVersion] associated with [interfaceDefinition]
         */
        private lateinit var interfaceSpecificationVersion: InterfaceSpecificationVersion

        /**
         * [InterfaceSpecificationTemplate] associated with the [InterfaceSpecification]
         * associated with [interfaceSpecificationVersion]
         */
        private lateinit var interfaceSpecificationTemplate: InterfaceSpecificationTemplate

        /**
         * Checks if [interfaceDefinition] is still derived visible/invisible
         * Only checks, performs no updates!
         *
         * @param visible if `true`, checks if [interfaceDefinition] is derived visible, otherwise invisible
         * @return `true` if still derived, otherwise `false`
         */
        suspend fun checkIsStillDerived(visible: Boolean): Boolean {
            interfaceSpecificationVersion = interfaceDefinition.interfaceSpecificationVersion(cache).value
            interfaceSpecificationTemplate =
                interfaceSpecificationVersion.interfaceSpecification(cache).value.template(cache).value
            return if (visible) {
                checkIsDerivedVisible(interfaceDefinition)
            } else {
                checkIsDerivedInvisible(interfaceDefinition)
            }
        }

        /**
         * Checks if [definition] is still derived visible
         * Only checks, performs no updates!
         *
         * @param definition the [InterfaceDefinition] to check if it is still derived
         * @return `true` if still derived, otherwise `false`
         */
        private suspend fun checkIsDerivedVisible(definition: InterfaceDefinition): Boolean {
            val componentVersion = definition.componentVersion(cache).value
            if (componentVersion in checkedVisibleComponentVersions) {
                return false
            }
            checkedVisibleComponentVersions += componentVersion
            definition.visibleDerivedBy(cache).forEach {
                if (doesRelationStillDerive(it, true)) {
                    return true
                }
            }
            return false
        }

        /**
         * Checks if [definition] is still derived invisible
         * Only checks, performs no updates!
         *
         * @param definition the [InterfaceDefinition] to check if it is still derived
         * @return `true` if still derived, otherwise `false`
         */
        private suspend fun checkIsDerivedInvisible(definition: InterfaceDefinition): Boolean {
            val componentVersion = definition.componentVersion(cache).value
            if (componentVersion in checkedInvisibleComponentVersions) {
                return false
            }
            checkedInvisibleComponentVersions += componentVersion
            definition.invisibleDerivedBy(cache).forEach {
                if (doesRelationStillDerive(it, false)) {
                    return true
                }
            }
            return false
        }

        /**
         * Checks if a [Relation] still derives [interfaceSpecificationVersion]
         *
         * @param relation used to check for derivation
         * @param derivedVisible if `true`, checks for visible derivation, otherwise for invisible derivation
         * @return `true` [relation] still derives [interfaceSpecificationVersion]
         */
        private suspend fun doesRelationStillDerive(
            relation: Relation, derivedVisible: Boolean
        ): Boolean {
            assert(relation !in deletedNodes)
            val startNode = relation.start(cache).value
            val endNode = relation.end(cache).value
            if (startNode !is ComponentVersion || endNode !is ComponentVersion) {
                return false
            }
            val endDefinition = endNode.interfaceDefinitions(cache).firstOrNull {
                assert(it !in deletedNodes)
                it.interfaceSpecificationVersion(cache).value == interfaceSpecificationVersion
            } ?: return false

            val startTemplate = startNode.relationPartnerTemplate(cache)
            val endTemplate = endNode.relationPartnerTemplate(cache)

            return doesRelationStillDerive(relation, startTemplate, endTemplate, derivedVisible, endDefinition)
        }

        /**
         * Checks if a [Relation] still derives [interfaceSpecificationVersion]
         *
         * @param relation used to check for derivation
         * @param startTemplate the template of the start of [relation]
         * @param endTemplate the template of the end of [relation]
         * @param derivedVisible if `true`, checks for visible derivation, otherwise for invisible derivation
         * @param endDefinition the [InterfaceDefinition] representing [interfaceSpecificationVersion] on the end
         *   of the [relation]
         * @return `true` [relation] still derives [interfaceSpecificationVersion]
         */
        private suspend fun doesRelationStillDerive(
            relation: Relation,
            startTemplate: RelationPartnerTemplate<*, *>,
            endTemplate: RelationPartnerTemplate<*, *>,
            derivedVisible: Boolean,
            endDefinition: InterfaceDefinition
        ): Boolean {
            var byVisibleDerived = false
            var byInvisibleDerived = false
            relation.template(cache).value.relationConditions(cache).forEach { relationCondition ->
                if (startTemplate in relationCondition.from(cache) && endTemplate in relationCondition.to(cache)) {
                    relationCondition.interfaceSpecificationDerivationConditions(cache).forEach {
                        if (it.derivableInterfaceSpecifications(cache).contains(interfaceSpecificationTemplate)) {
                            if (it.isVisibleDerived && derivedVisible || it.isInvisibleDerived && !derivedVisible) {
                                byVisibleDerived = byVisibleDerived || it.derivesVisibleDerived
                                byInvisibleDerived = byInvisibleDerived || it.derivesInvisibleDerived
                                if (derivedBySelfDefined(endDefinition, it)) {
                                    return true
                                }
                            }
                        }
                    }
                }
            }
            return when {
                byVisibleDerived && checkIsDerivedVisible(endDefinition) -> true
                byInvisibleDerived && checkIsDerivedInvisible(endDefinition) -> true
                else -> false
            }
        }

        /**
         * If [interfaceDefinition] derives (in)visible by self-defined and [endDefinition] is (in)visibleSelfDefined,
         * returns `true`, otherwise `false`
         *
         * @param endDefinition [InterfaceSpecificationVersion] on the end of the [Relation] to check for derivation
         * @param derivationCondition [InterfaceSpecificationDerivationCondition] on the [Relation]
         * @return `true` if the [Relation] derives [endDefinition] by self-defined
         */
        private fun derivedBySelfDefined(
            endDefinition: InterfaceDefinition, derivationCondition: InterfaceSpecificationDerivationCondition
        ): Boolean {
            return when {
                endDefinition.visibleSelfDefined && derivationCondition.derivesInvisibleSelfDefined -> true
                endDefinition.invisibleSelfDefined && derivationCondition.derivesInvisibleSelfDefined -> true
                else -> false
            }
        }
    }
}