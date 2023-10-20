package gropius.service.issue

import gropius.model.architecture.*
import gropius.model.issue.AggregatedIssue
import gropius.model.issue.Issue
import gropius.model.template.IssueState
import gropius.model.template.IssueType
import gropius.service.NodeBatchUpdateContext
import gropius.service.NodeBatchUpdater

/**
 * Helper class to handle anything AggregatedIssue related.
 * After using, nodes from [deletedNodes] must be deleted, and nodes from [updatedNodes] must be saved
 */
class IssueAggregationUpdater(
    updateContext: NodeBatchUpdater = NodeBatchUpdateContext()
) : NodeBatchUpdater by updateContext {

    /**
     * Should be called when the state or type of an issue was changed.
     * Musts be called AFTER the change was applied to the issue.
     *
     * @param issue the issue that was changed
     * @param oldState the old state of the issue
     * @param oldType the old type of the issue
     */
    suspend fun changedIssueStateOrType(issue: Issue, oldState: IssueState, oldType: IssueType) {
        if (issue.type(cache).value == oldType && issue.state(cache).value.isOpen == oldState.isOpen) {
            return
        }
        val relationPartners = issue.aggregatedBy(cache).map { aggregatedBy ->
            removeIssueFromAggregatedIssue(issue, aggregatedBy)
            aggregatedBy.relationPartner(cache).value
        }
        issue.aggregatedBy(cache).clear()
        relationPartners.forEach {
            createOrUpdateAggregatedIssues(it, setOf(issue))
        }
    }

    /**
     * Should be called when an [Issue] was deleted.
     * Must be called BEFORE the issue is deleted from the database.
     *
     * @param issue the issue that was deleted
     */
    suspend fun deletedIssue(issue: Issue) {
        issue.aggregatedBy(cache).forEach { aggregatedBy ->
            removeIssueFromAggregatedIssue(issue, aggregatedBy)
        }
    }

    /**
     * Should be called when a [ComponentVersion] is deleted.
     * Must be called BEFORE the component version is deleted from the database.
     *
     * @param componentVersion the component version that was deleted
     */
    suspend fun deletedComponentVersion(componentVersion: ComponentVersion) {
        relationPartnerDeleted(componentVersion)
        for (issue in componentVersion.affectingIssues(cache)) {
            aggregateIssueOnComponentIfNecessary(issue, componentVersion.component(cache).value)
        }
    }

    /**
     * Should be called when an [Issue] is added to a [Trackable].
     * Must be called AFTER the issue was added to the trackable.
     *
     * @param issue the issue that was added
     * @param trackable the trackable the issue was added to
     */
    suspend fun addedIssueToTrackable(issue: Issue, trackable: Trackable) {
        if (trackable !is Component) {
            return
        }
        aggregateIssueOnComponentIfNecessary(issue, trackable)
    }

    /**
     * Should be called when an [Issue] is removed from a [Trackable].
     * Must be called AFTER the issue is removed from the trackable.
     *
     * @param issue the issue that was removed
     * @param trackable the trackable the issue was removed from
     */
    suspend fun removedIssueFromTrackable(issue: Issue, trackable: Trackable) {
        if (trackable !is Component) {
            return
        }
        val aggregatedBy = issue.aggregatedBy(cache)
        val removed = mutableSetOf<AggregatedIssue>()
        aggregatedBy.forEach {
            val target = it.relationPartner(cache).value
            if (target is ComponentVersion && target.component(cache).value == trackable) {
                if (!isIssueStillAggregatedByComponentVersion(issue, target)) {
                    removeIssueFromAggregatedIssue(issue, it)
                    removed += it
                }
            }
        }
        aggregatedBy.removeAll(removed)
    }

    /**
     * Should be called when an [Interface] is created
     *
     * @param createdInterface the created interface
     */
    suspend fun createdInterface(createdInterface: Interface) {
        val definition = createdInterface.interfaceDefinition(cache).value
        val specificationVersion = definition.interfaceSpecificationVersion(cache).value
        val affectableEntities = listOf(
            createdInterface, specificationVersion, specificationVersion.interfaceSpecification(cache).value
        ) + specificationVersion.parts(cache)
        val affectedIssues = affectableEntities.flatMap { it.affectingIssues(cache) }.toSet()
        createOrUpdateAggregatedIssues(createdInterface, affectedIssues)
        val component = definition.componentVersion(cache).value.component(cache).value
        for (issue in affectedIssues) {
            unaggregateIssueOnComponentIfNecessary(issue, component)
        }
    }

    /**
     * Should be called when an [Interface] is deleted
     * Must be called BEFORE the interface is deleted from the database.
     *
     * @param deletedInterface the deleted interface
     */
    suspend fun deletedInterface(deletedInterface: Interface) {
        relationPartnerDeleted(deletedInterface)
        val component =
            deletedInterface.interfaceDefinition(cache).value.componentVersion(cache).value.component(cache).value
        for (issue in deletedInterface.affectingIssues(cache)) {
            aggregateIssueOnComponentIfNecessary(issue, component)
        }
    }

    /**
     * Should be called when an [InterfacePart] is deleted
     * Must be called BEFORE the interface part is deleted from the database.
     *
     * @param interfacePart the deleted interface part
     */
    suspend fun deletedInterfacePart(interfacePart: InterfacePart) {
        val interfaces = interfacePart.partOf(cache).flatMap { version ->
            version.interfaceDefinitions(cache).mapNotNull {
                it.visibleInterface(cache).value
            }
        }
        val components = interfaces.map {
            it.interfaceDefinition(cache).value.componentVersion(cache).value.component(cache).value
        }
        interfacePart.affectingIssues(cache).forEach { issue ->
            issue.affects(cache).remove(interfacePart)
            interfaces.forEach {
                if (!isIssueStillAggregatedByInterface(issue, it)) {
                    removeIssueFromAggregatedIssueOnRelationPartner(issue, it)
                }
            }
            for (component in components) {
                aggregateIssueOnComponentIfNecessary(issue, component)
            }
        }
    }

    /**
     * Should be called when the active parts of an [InterfaceSpecificationVersion] were updated.
     * Must be called AFTER the active parts were updated.
     *
     * @param interfaceSpecificationVersion the interface specification version that was updated
     * @param addedParts the parts that were added
     * @param removedParts the parts that were removed
     */
    suspend fun updatedActiveParts(
        interfaceSpecificationVersion: InterfaceSpecificationVersion,
        addedParts: Set<InterfacePart>,
        removedParts: Set<InterfacePart>
    ) {
        val interfaces = interfaceSpecificationVersion.interfaceDefinitions(cache).mapNotNull {
            it.visibleInterface(cache).value
        }
        val newAffectingIssues = addedParts.flatMap { it.affectingIssues(cache) }.toSet()
        val potentialIssuesToRemove = removedParts.flatMap { it.affectingIssues(cache) }.toSet()
        for (inter in interfaces) {
            createOrUpdateAggregatedIssues(inter, newAffectingIssues)
            for (issue in potentialIssuesToRemove) {
                if (!isIssueStillAggregatedByInterface(issue, inter)) {
                    removeIssueFromAggregatedIssueOnRelationPartner(issue, inter)
                }
            }
        }
        val components = interfaces.map {
            it.interfaceDefinition(cache).value.componentVersion(cache).value.component(cache).value
        }.toSet()
        for (component in components) {
            for (issue in newAffectingIssues) {
                unaggregateIssueOnComponentIfNecessary(issue, component)
            }
            for (issue in potentialIssuesToRemove) {
                aggregateIssueOnComponentIfNecessary(issue, component)
            }
        }
    }

    /**
     * Should be called when an [Issue] affects an additional entity.
     * Must be called AFTER the affected entity was added to the issue.
     *
     * @param issue the issue that now affects [affectedEntity]
     * @param affectedEntity the entity that is now affected by [issue]
     */
    suspend fun addedAffectedEntity(issue: Issue, affectedEntity: AffectedByIssue) {
        when (affectedEntity) {
            is Component -> {
                affectedEntity.versions(cache).forEach {
                    createOrUpdateAggregatedIssues(it, setOf(issue))
                }
            }

            is ComponentVersion -> {
                createOrUpdateAggregatedIssues(affectedEntity, setOf(issue))
                unaggregateIssueOnComponentIfNecessary(issue, affectedEntity.component(cache).value)
            }

            is Interface -> {
                addedAffectedInterfaceRelatedEntity(issue, setOf(affectedEntity))
            }

            is InterfacePart -> {
                addedAffectedInterfaceRelatedEntity(issue, affectedEntity.partOf(cache))
            }

            is InterfaceSpecificationVersion -> {
                addedAffectedInterfaceRelatedEntity(issue, setOf(affectedEntity))
            }

            is InterfaceSpecification -> {
                addedAffectedInterfaceRelatedEntity(issue, affectedEntity.versions(cache))
            }

            is Project -> {
                // ignore, does not affect components / interfaces
            }

            else -> {
                error("Unknown affected entity")
            }
        }
    }

    /**
     * Should be called when an [Issue] does not affect an entity anymore.
     * Must be called AFTER the affected entity was removed from the issue.
     *
     * @param issue the issue that does not affect [affectedEntity] anymore
     * @param affectedEntity the entity that is not affected by [issue] anymore
     */
    suspend fun removedAffectedEntity(issue: Issue, affectedEntity: AffectedByIssue) {
        when (affectedEntity) {
            is Component -> {
                if (affectedEntity !in issue.trackables(cache) || doesIssueAffectComponentRelatedEntity(
                        issue, affectedEntity
                    )
                ) {
                    affectedEntity.versions(cache).forEach {
                        if (it !in issue.affects(cache)) {
                            removeIssueFromAggregatedIssueOnRelationPartner(issue, it)
                        }
                    }
                }
            }

            is ComponentVersion -> {
                val component = affectedEntity.component(cache).value
                if (doesIssueAffectComponentRelatedEntity(issue, component)) {
                    if (component !in issue.affects(cache)) {
                        removeIssueFromAggregatedIssueOnRelationPartner(issue, affectedEntity)
                    }
                } else {
                    if (component in issue.trackables(cache)) {
                        for (componentVersion in component.versions(cache)) {
                            createOrUpdateAggregatedIssues(componentVersion, setOf(issue))
                        }
                    } else {
                        removeIssueFromAggregatedIssueOnRelationPartner(issue, affectedEntity)
                    }
                }
            }

            is Interface -> {
                removedAffectedInterfaceRelatedEntity(issue, setOf(affectedEntity))
            }

            is InterfacePart -> {
                removedAffectedInterfaceRelatedEntity(issue, affectedEntity.partOf(cache))
            }

            is InterfaceSpecificationVersion -> {
                removedAffectedInterfaceRelatedEntity(issue, setOf(affectedEntity))
            }

            is InterfaceSpecification -> {
                removedAffectedInterfaceRelatedEntity(issue, affectedEntity.versions(cache))
            }

            is Project -> {
                // ignore, does not affect components / interfaces
            }

            else -> {
                error("Unknown affected entity")
            }
        }
    }

    /**
     * Handles the case of an [Issue] affecting an interface related entity ([InterfacePart], [InterfaceSpecificationVersion], [InterfaceSpecification]).
     * All relevant [InterfaceSpecificationVersion]s must be provided, each related [Interface] will be updated.
     * Relevance depends on the type of affected entity.
     *
     * @param issue the issue that affects the entity
     * @param interfaceSpecificationVersions the interface related entities that are affected by the issue
     */
    private suspend fun addedAffectedInterfaceRelatedEntity(
        issue: Issue, interfaceSpecificationVersions: Set<InterfaceSpecificationVersion>
    ) {
        val interfaces = interfaceSpecificationVersions.flatMap { version ->
            version.interfaceDefinitions(cache).mapNotNull { it.visibleInterface(cache).value }
        }
        addedAffectedInterfaceRelatedEntity(issue, interfaces)
    }

    /**
     * Handles the case of an [Issue] affecting [Interface]s, directly or indirectly.
     *
     * @param issue the issue that affects the entity
     * @param interfaces the interfaces that are affected by the issue
     */
    private suspend fun addedAffectedInterfaceRelatedEntity(
        issue: Issue, interfaces: Collection<Interface>
    ) {
        for (inter in interfaces) {
            createOrUpdateAggregatedIssues(inter, setOf(issue))
        }
        val components = interfaces.map {
            it.interfaceDefinition(cache).value.componentVersion(cache).value.component(cache).value
        }.toSet()
        for (component in components) {
            unaggregateIssueOnComponentIfNecessary(issue, component)
        }
    }

    /**
     * Handles the case of an [Issue] no longer affecting an interface related entity ([InterfacePart], [InterfaceSpecificationVersion], [InterfaceSpecification]).
     * All relevant [InterfaceSpecificationVersion]s must be provided, each related [Interface] will be updated.
     * Relevance depends on the type of affected entity.
     *
     * @param issue the issue that no longer affects the entity
     * @param interfaceSpecificationVersions the interface related entities that may no longer be affected by the issue
     */
    private suspend fun removedAffectedInterfaceRelatedEntity(
        issue: Issue, interfaceSpecificationVersions: Set<InterfaceSpecificationVersion>
    ) {
        val interfaces = interfaceSpecificationVersions.flatMap { version ->
            version.interfaceDefinitions(cache).mapNotNull { it.visibleInterface(cache).value }
        }
        removedAffectedInterfaceRelatedEntity(issue, interfaces)
    }

    /**
     * Handles the case of an [Issue] no longer affecting [Interface]s, directly or indirectly.
     * Handles the case that the [Issue] still affects the [Interface]s.
     *
     * @param issue the issue that no longer affects the entity
     * @param interfaces the interfaces that may no longer be affected by the issue
     */
    private suspend fun removedAffectedInterfaceRelatedEntity(
        issue: Issue, interfaces: Collection<Interface>
    ) {
        val addToComponentPotentially = mutableSetOf<Component>()
        for (inter in interfaces) {
            if (!isIssueStillAggregatedByInterface(issue, inter)) {
                removeIssueFromAggregatedIssueOnRelationPartner(issue, inter)
                addToComponentPotentially += inter.interfaceDefinition(cache).value.componentVersion(cache).value.component(
                    cache
                ).value
            }
        }
        for (component in addToComponentPotentially) {
            aggregateIssueOnComponentIfNecessary(issue, component)
        }
    }

    /**
     * Removes an issue from an [AggregatedIssue] on a [RelationPartner].
     * Handles the case of the [Issue] not being aggregated on the [RelationPartner].
     *
     * @param issue the issue to remove
     * @param relationPartner the relation partner to remove the issue from
     */
    private suspend fun removeIssueFromAggregatedIssueOnRelationPartner(
        issue: Issue, relationPartner: RelationPartner
    ) {
        val type = issue.type(cache).value
        val isOpen = issue.state(cache).value.isOpen
        val aggregatedIssue = relationPartner.aggregatedIssues(cache).find {
            it.type(cache).value == type && it.isOpen == isOpen
        } ?: return
        removeIssueFromAggregatedIssue(issue, aggregatedIssue)
    }

    /**
     * Removes an [Issue] from an [AggregatedIssue].
     * Handles the case of the [Issue] not being aggregated on the [AggregatedIssue].
     *
     * @param issue the issue to remove
     * @param aggregatedIssue the aggregated issue to remove the issue from
     */
    private suspend fun removeIssueFromAggregatedIssue(
        issue: Issue, aggregatedIssue: AggregatedIssue
    ) {
        if (aggregatedIssue.issues(cache).remove(issue)) {
            aggregatedIssue.count--
            internalUpdatedNodes += aggregatedIssue
            if (aggregatedIssue.issues(cache).isEmpty()) {
                deleteAggregatedIssue(aggregatedIssue)
            }
        }
    }

    /**
     * Checks if an [Issue] is still aggregated by a [ComponentVersion].
     *
     * @param issue the issue to check
     * @param componentVersion the component version to check
     */
    private suspend fun isIssueStillAggregatedByComponentVersion(
        issue: Issue, componentVersion: ComponentVersion
    ): Boolean {
        val affected = issue.affects(cache)
        if (componentVersion in affected || componentVersion.component(cache).value in affected) {
            return true
        }
        val component = componentVersion.component(cache).value
        if (component in issue.trackables(cache)) {
            return !doesIssueAffectComponentRelatedEntity(issue, component)
        }
        return false
    }

    /**
     * Checks if an [Issue] is still aggregated by an [Interface].
     *
     * @param issue the issue to check
     * @param inter the interface to check
     * @return true if the issue is still aggregated by the interface, false otherwise
     */
    private suspend fun isIssueStillAggregatedByInterface(
        issue: Issue, inter: Interface
    ): Boolean {
        val specificationVersion = inter.interfaceDefinition(cache).value.interfaceSpecificationVersion(cache).value
        val affectableEntities = listOf(
            inter, specificationVersion, specificationVersion.interfaceSpecification(cache).value
        ) + specificationVersion.parts(cache)
        return issue.affects(cache).any { it in affectableEntities }
    }

    /**
     * Checks if an [Issue] affects an entity related to a [Component].
     *
     * @param issue the issue to check
     * @param component the component to check
     * @return true if the issue affects an entity related to the component, false otherwise
     */
    private suspend fun doesIssueAffectComponentRelatedEntity(issue: Issue, component: Component): Boolean {
        val relatedAffectedEntities = componentRelatedEntities(component)
        return issue.affects(cache).any { it in relatedAffectedEntities }
    }

    /**
     * Gets all entities related to a [Component].
     * This includes
     * - the [Component] itself
     * - all [ComponentVersion]s of the [Component]
     * - all [Interface]s of all [ComponentVersion]s
     * - all [InterfaceSpecificationVersion]s of all [Interface]s
     * - all [InterfaceSpecification]s of all used [InterfaceSpecificationVersion]s
     *
     * @param component the component to get the related entities of
     * @return a set of all related entities
     */
    private suspend fun componentRelatedEntities(component: Component): Set<AffectedByIssue> {
        val affected = mutableSetOf<AffectedByIssue>()
        affected += component
        for (version in component.versions(cache)) {
            affected += version
            for (interfaceDefinition in version.interfaceDefinitions(cache)) {
                val inter = interfaceDefinition.visibleInterface(cache).value
                if (inter != null) {
                    affected += inter
                    val specificationVersion = interfaceDefinition.interfaceSpecificationVersion(cache).value
                    affected += specificationVersion
                    affected += specificationVersion.interfaceSpecification(cache).value
                    affected += specificationVersion.parts(cache)
                }
            }
        }
        return affected
    }

    /**
     * Aggregates a set of [Issue]s on a [RelationPartner].
     * Handles the case of an [Issue] already being aggregated on the [RelationPartner].
     *
     * @param relationPartner the relation partner to aggregate the issues on
     * @param issues the issues to aggregate
     */
    private suspend fun createOrUpdateAggregatedIssues(relationPartner: RelationPartner, issues: Set<Issue>) {
        val aggregatedIssues = relationPartner.aggregatedIssues(cache)
        val aggregatedIssueLookup = aggregatedIssues.associateBy {
            it.type(cache).value to it.isOpen
        }.toMutableMap()
        for (issue in issues) {
            val state = issue.state(cache).value
            val type = issue.type(cache).value
            aggregatedIssueLookup.getOrPut(type to state.isOpen) {
                val aggregatedIssue = AggregatedIssue(0, state.isOpen)
                aggregatedIssue.relationPartner(cache).value = relationPartner
                aggregatedIssue.type(cache).value = type
                aggregatedIssue
            }.let {
                if (it.issues(cache).add(issue)) {
                    it.count++
                }
                internalUpdatedNodes += it
            }
        }
    }

    /**
     * Handles the deletion of an [Interface] or [ComponentVersion]
     * Deletes all associated [AggregatedIssue]s and relations.
     *
     * @param relationPartner the relation partner that was deleted
     */
    private suspend fun relationPartnerDeleted(relationPartner: RelationPartner) {
        relationPartner.aggregatedIssues(cache).forEach {
            deleteAggregatedIssue(it)
        }
        relationPartner.affectingIssues(cache).forEach { issue ->
            issue.affects(cache).remove(relationPartner)
        }
    }

    /**
     * Deletes an [AggregatedIssue] and all relations.
     *
     * @param aggregatedIssue the aggregated issue to delete
     */
    private suspend fun deleteAggregatedIssue(aggregatedIssue: AggregatedIssue) {
        deletedNodes += aggregatedIssue
        deletedNodes += aggregatedIssue.incomingRelations(cache)
        deletedNodes += aggregatedIssue.outgoingRelations(cache)
    }

    /**
     * Aggregates an [Issue] on a [Component] if necessary.
     * It is necessary only if the issue is on the [Component] and does not affect an entity related to said [Component].
     *
     * @param issue the issue to aggregate
     * @param component the component to aggregate the issue on
     */
    private suspend fun aggregateIssueOnComponentIfNecessary(issue: Issue, component: Component) {
        if (component !in issue.trackables(cache)) {
            return
        }
        if (!doesIssueAffectComponentRelatedEntity(issue, component)) {
            for (componentVersion in component.versions(cache)) {
                createOrUpdateAggregatedIssues(componentVersion, setOf(issue))
            }
        }
    }

    /**
     * Unaggregates an [Issue] from a [ComponentVersion] if necessary.
     * It is necessary only if the issue is on the [Component] and affects an entity related to said [Component].
     * It is only unaggregated on [ComponentVersion]s where the issue does not affect the [Component] or [ComponentVersion] itself.
     *
     * @param issue the issue to unaggregate
     * @param component the component to unaggregate the issue from
     */
    private suspend fun unaggregateIssueOnComponentIfNecessary(issue: Issue, component: Component) {
        if (component in issue.trackables(cache)) {
            return
        }
        if (doesIssueAffectComponentRelatedEntity(issue, component) && component !in issue.affects(cache)) {
            for (componentVersion in component.versions(cache)) {
                if (componentVersion !in issue.affects(cache)) {
                    removeIssueFromAggregatedIssueOnRelationPartner(issue, componentVersion)
                }
            }
        }
    }

}