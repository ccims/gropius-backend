package gropius.service.issue

import gropius.model.architecture.*
import gropius.model.issue.AggregatedIssue
import gropius.model.issue.AggregatedIssueRelation
import gropius.model.issue.Issue
import gropius.model.issue.timeline.IssueRelation
import gropius.model.template.IssueState
import gropius.model.template.IssueType
import gropius.service.NodeBatchUpdateContext
import gropius.service.NodeBatchUpdater
import gropius.service.architecture.GraphUpdater

/**
 * Helper class to handle anything AggregatedIssue related.
 * After using, nodes from [deletedNodes] must be deleted, and nodes from [updatedNodes] must be saved
 */
class IssueAggregationUpdater(
    updateContext: NodeBatchUpdater = NodeBatchUpdateContext()
) : GraphUpdater(updateContext) {

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
            createOrUpdateAggregatedIssue(it, issue)
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
        val interfaceSpecificationVersions = issue.affects(cache).flatMap {
            when (it) {
                is InterfaceSpecificationVersion -> listOf(it)
                is InterfaceSpecification -> it.versions(cache)
                is InterfacePart -> listOf(it.partOf(cache).value)
                else -> emptyList()
            }
        }
        val interfaces = interfaceSpecificationVersions.flatMap { version ->
            version.interfaceDefinitions(cache).filter {
                it.componentVersion(cache).value.component(cache).value == trackable
            }.mapNotNull { it.visibleInterface(cache).value }
        } + issue.affects(cache).filterIsInstance<Interface>().filter {
            it.interfaceDefinition(cache).value.componentVersion(cache).value.component(cache).value == trackable
        }
        interfaces.forEach {
            createOrUpdateAggregatedIssue(it, issue)
        }
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
            val targetComponent = if (target is ComponentVersion) {
                target.component(cache).value
            } else if (target is Interface) {
                target.interfaceDefinition(cache).value.componentVersion(cache).value.component(cache).value
            } else {
                error("Unknown relation partner")
            }
            if (targetComponent == trackable) {
                removeIssueFromAggregatedIssue(issue, it)
                removed += it
            }
        }
        aggregatedBy.removeAll(removed)
    }

    /**
     * Should be called when a [ComponentVersion] is created.
     *
     * @param componentVersion the created component version
     */
    suspend fun createdComponentVersion(componentVersion: ComponentVersion) {
        val component = componentVersion.component(cache).value
        component.issues(cache) {
            load(Issue::trackables)
            load(Issue::affects)
            load(Issue::type)
            load(Issue::state)
            load(Issue::outgoingRelations) {
                load(IssueRelation::relatedIssue) {
                    load(Issue::aggregatedBy)
                }
            }
            load(Issue::incomingRelations) {
                load(IssueRelation::issue) {
                    load(Issue::aggregatedBy)
                }
            }
        }.forEach {
            if (!doesIssueAffectComponentRelatedEntity(it, component)) {
                createOrUpdateAggregatedIssue(componentVersion, it)
            }
        }
    }

    /**
     * Should be called when an [Interface] is created
     *
     * @param createdInterface the created interface
     */
    suspend fun createdInterface(createdInterface: Interface) {
        val definition = createdInterface.interfaceDefinition(cache).value
        val component = definition.componentVersion(cache).value.component(cache).value
        val specificationVersion = definition.interfaceSpecificationVersion(cache).value
        val affectableEntities = listOf(
            createdInterface, specificationVersion, specificationVersion.interfaceSpecification(cache).value
        ) + specificationVersion.parts(cache)
        val affectedIssues = affectableEntities.flatMap { it.affectingIssues(cache) }.filter {
            component in it.trackables(cache)
        }.toSet()
        affectedIssues.forEach {
            createOrUpdateAggregatedIssue(createdInterface, it)
        }
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
        val interfaces = interfacePart.partOf(cache).value.interfaceDefinitions(cache).mapNotNull {
            it.visibleInterface(cache).value
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
     * Should be called when an [IssueRelation] is created.
     * Must be called AFTER the relation was created.
     *
     * @param issueRelation the created issue relation
     */
    suspend fun createdIssueRelation(issueRelation: IssueRelation) {
        val issue = issueRelation.issue(cache).value
        val relatedIssue = issueRelation.relatedIssue(cache).value
        if (relatedIssue != null) {
            val startAggregatedIssues = issue.aggregatedBy(cache)
            val endAggregatedIssues = relatedIssue.aggregatedBy(cache)
            startAggregatedIssues.forEach { start ->
                endAggregatedIssues.forEach { end ->
                    createOrUpdateAggregatedIssueRelation(start, end, issueRelation)
                }
            }
        }
    }

    /**
     * Should be called when an [IssueRelation] is deleted.
     *
     * @param issueRelation the deleted issue relation
     */
    suspend fun deletedIssueRelation(issueRelation: IssueRelation) {
        issueRelation.aggregatedBy(cache).forEach { aggregatedBy ->
            removeIssueRelationFromAggregatedIssueRelation(issueRelation, aggregatedBy)
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
                    createOrUpdateAggregatedIssue(it, issue)
                }
            }

            is ComponentVersion -> {
                createOrUpdateAggregatedIssue(affectedEntity, issue)
                unaggregateIssueOnComponentIfNecessary(issue, affectedEntity.component(cache).value)
            }

            is Interface -> {
                addedAffectedInterfaceRelatedEntity(issue, setOf(affectedEntity))
            }

            is InterfacePart -> {
                addedAffectedInterfaceRelatedEntity(issue, setOf(affectedEntity.partOf(cache).value))
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
                            createOrUpdateAggregatedIssue(componentVersion, issue)
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
                removedAffectedInterfaceRelatedEntity(issue, setOf(affectedEntity.partOf(cache).value))
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
            version.interfaceDefinitions(cache).filter {
                it.componentVersion(cache).value.component(cache).value in issue.trackables(cache)
            }.mapNotNull { it.visibleInterface(cache).value }
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
        val filteredInterfaces = interfaces.filter {
            it.interfaceDefinition(cache).value.componentVersion(cache).value.component(cache).value in issue.trackables(
                cache
            )
        }
        for (inter in filteredInterfaces) {
            createOrUpdateAggregatedIssue(inter, issue)
        }
        val components = filteredInterfaces.map {
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
            issue.outgoingRelations(cache).forEach { relation ->
                val aggregatedBy = relation.aggregatedBy(cache).filter {
                    it.start(cache).value == aggregatedIssue
                }
                aggregatedBy.forEach {
                    removeIssueRelationFromAggregatedIssueRelation(relation, it)
                }
            }
            issue.incomingRelations(cache).forEach { relation ->
                val aggregatedBy = relation.aggregatedBy(cache).filter {
                    it.end(cache).value == aggregatedIssue
                }
                aggregatedBy.forEach {
                    removeIssueRelationFromAggregatedIssueRelation(relation, it)
                }
            }
        }
    }

    /**
     * Removes an [IssueRelation] from an [AggregatedIssueRelation].
     * Deletes the [AggregatedIssueRelation] if it is no longer required.
     *
     * @param issueRelation the issue relation to remove from the aggregated issue relation
     * @param aggregatedIssueRelation the aggregated issue relation to remove the issue relation from
     */
    private suspend fun removeIssueRelationFromAggregatedIssueRelation(
        issueRelation: IssueRelation, aggregatedIssueRelation: AggregatedIssueRelation
    ) {
        if (aggregatedIssueRelation.issueRelations(cache).remove(issueRelation)) {
            aggregatedIssueRelation.count--
            internalUpdatedNodes += aggregatedIssueRelation
            if (aggregatedIssueRelation.issueRelations(cache).isEmpty()) {
                deletedNodes += aggregatedIssueRelation
            }
        }
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
        if (component !in issue.trackables(cache)) {
            return false
        }
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
     * Aggregates an [Issue] on a [RelationPartner].
     * Handles the case of an [Issue] already being aggregated on the [RelationPartner].
     *
     * @param relationPartner the relation partner to aggregate the issues on
     * @param issue the issue to aggregate
     */
    private suspend fun createOrUpdateAggregatedIssue(relationPartner: RelationPartner, issue: Issue) {
        val aggregatedIssues = relationPartner.aggregatedIssues(cache)
        val aggregatedIssue = aggregatedIssues.firstOrNull {
            it.type(cache).value == issue.type(cache).value && it.isOpen == issue.state(cache).value.isOpen
        } ?: AggregatedIssue(0, issue.state(cache).value.isOpen).also {
            it.relationPartner(cache).value = relationPartner
            it.type(cache).value = issue.type(cache).value
            aggregatedIssues += it
        }
        if (aggregatedIssue.issues(cache).add(issue)) {
            aggregatedIssue.count++
        }
        internalUpdatedNodes += aggregatedIssue
        issue.outgoingRelations(cache).forEach { relation ->
            val relatedIssue = relation.relatedIssue(cache).value ?: return@forEach
            for (relatedAggregatedIssue in relatedIssue.aggregatedBy(cache)) {
                createOrUpdateAggregatedIssueRelation(aggregatedIssue, relatedAggregatedIssue, relation)
            }
        }
        issue.incomingRelations(cache).forEach { relation ->
            val relatedIssue = relation.issue(cache).value
            for (relatedAggregatedIssue in relatedIssue.aggregatedBy(cache)) {
                createOrUpdateAggregatedIssueRelation(relatedAggregatedIssue, aggregatedIssue, relation)
            }
        }
    }

    /**
     * Creates or updates an [AggregatedIssueRelation] between two [AggregatedIssue]s.
     * Handles the case of an [IssueRelation] already being aggregated on the [AggregatedIssueRelation].
     *
     * @param from the start aggregated issue relation
     * @param to the end aggregated issue relation
     * @param issueRelation the issue relation to aggregate
     */
    private suspend fun createOrUpdateAggregatedIssueRelation(
        from: AggregatedIssue, to: AggregatedIssue, issueRelation: IssueRelation
    ) {
        if (from == to) {
            return
        }
        val type = issueRelation.type(cache).value
        val aggregatedIssueRelation = from.outgoingRelations(cache).find {
            it.end(cache).value == to && it.type(cache).value == type
        } ?: AggregatedIssueRelation(0).also {
            it.start(cache).value = from
            it.end(cache).value = to
            it.type(cache).value = type
        }
        internalUpdatedNodes += aggregatedIssueRelation
        if (aggregatedIssueRelation.issueRelations(cache).add(issueRelation)) {
            aggregatedIssueRelation.count++
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
                createOrUpdateAggregatedIssue(componentVersion, issue)
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
        if (component !in issue.trackables(cache) || component in issue.affects(cache)) {
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