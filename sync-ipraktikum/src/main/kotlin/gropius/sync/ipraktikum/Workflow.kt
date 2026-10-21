package gropius.sync.ipraktikum

object Workflow {
    fun bucket(state: String, policy: TemplatePolicy): String =
        policy.states[state]
            ?: if (policy.autoStates) state else error("State $state needs a workflow mapping")

    fun defaultState(
        bucket: String,
        policy: TemplatePolicy,
        catalog: Catalog,
        template: String,
    ): String {
        val state =
            policy.bucketDefaults[bucket]
                ?: if (policy.autoStates && bucket !in policy.states.values) bucket
                else error("Choose a default Gropius state for workflow/$bucket")
        catalog.entry("state", state, template)
        require(bucket(state, policy) == bucket) { "Workflow default belongs to another bucket" }
        return state
    }

    fun validate(
        policy: TemplatePolicy,
        catalog: Catalog,
        template: String,
        nativeRelationLinks: Boolean = false,
    ) {
        catalog.entry("type", policy.defaultType, template)
        for (state in listOf(policy.initialState, policy.closedState, policy.reopenState)) catalog
            .entry("state", state, template)
        require(catalog.entry("state", policy.closedState, template).open == false) {
            "Closed default must be closed"
        }
        require(
            catalog.entry("state", policy.initialState, template).open == true &&
                catalog.entry("state", policy.reopenState, template).open == true
        ) {
            "Initial/reopen defaults must be open"
        }
        val states = catalog.entries.filter { it.kind == "state" && it.template == template }
        states
            .groupBy { bucket(it.key, policy) }
            .forEach { (bucket, entries) ->
                require(entries.map { it.open }.distinct().size == 1) {
                    "workflow/$bucket mixes open and closed states"
                }
                defaultState(bucket, policy, catalog, template)
            }
        if (policy.nativeAssignmentEnabled && policy.nativeAssignmentRole != null)
            catalog.entry("role", policy.nativeAssignmentRole, template)
        if (
            !nativeRelationLinks &&
                policy.genericRelationEnabled &&
                policy.genericRelationType != null
        )
            catalog.entry("relation", policy.genericRelationType, template)
    }

    fun incoming(
        exact: String,
        labels: Set<String>,
        open: Boolean,
        record: IssueRecord,
        policy: TemplatePolicy,
        catalog: Catalog,
        template: String,
    ): String {
        val workflows = labels.filter { it.startsWith("workflow/") }
        require(workflows.size == 1) { "Choose exactly one workflow label" }
        val bucket = workflows.single().removePrefix("workflow/")
        val base = record.baseline
        val exactChanged = base != null && exact != base.state
        val bucketChanged =
            record.lastLabels.filter { it.startsWith("workflow/") }.toSet() != workflows.toSet()
        val openChanged = record.lastOpen != null && open != record.lastOpen
        val candidate =
            when {
                exactChanged -> exact
                bucketChanged -> defaultState(bucket, policy, catalog, template)
                openChanged -> if (open) policy.reopenState else policy.closedState
                else -> exact
            }
        val state = catalog.entry("state", candidate, template)
        if (exactChanged && bucketChanged)
            require(bucket(candidate, policy) == bucket) {
                "Exact state and workflow edit conflict"
            }
        if ((exactChanged || bucketChanged) && openChanged)
            require(state.open == open) { "State and open/closed edits conflict" }
        return candidate
    }
}
