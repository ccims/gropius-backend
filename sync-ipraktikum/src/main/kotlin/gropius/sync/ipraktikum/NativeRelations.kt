package gropius.sync.ipraktikum

/** GitLab's symmetric pair links are a projection of arbitrary directed, typed relations. */
object NativeRelations {
    data class PairKey(val owner: String, val target: String)

    fun pair(first: String, second: String): PairKey {
        require(first != second) { "GitLab cannot project a relation to the same issue" }
        return if (first < second) PairKey(first, second) else PairKey(second, first)
    }

    fun member(source: String, relation: RelationValue): String = "$source/${relation.id}"

    fun desired(records: List<IssueRecord>): Map<PairKey, Set<String>> {
        val enrolled = records.filter { !it.detached }.associateBy { it.syncId }
        val desired = mutableMapOf<PairKey, MutableSet<String>>()
        for (record in enrolled.values) for (relation in record.baseline?.relations.orEmpty()) {
            if (relation.target !in enrolled || relation.target == record.syncId) continue
            desired
                .getOrPut(pair(record.syncId, relation.target)) { mutableSetOf() }
                .add(member(record.syncId, relation))
        }
        return desired
    }

    fun incoming(
        record: IssueRecord,
        relations: List<RelationValue>,
        records: List<IssueRecord>,
        links: List<GitLabLink>,
    ): List<RelationValue> {
        val result = relations.toMutableList()
        val receipts = records.flatMap { owner ->
            owner.relationLinks.map { (target, receipt) -> pair(owner.syncId, target) to receipt }
        }
        for ((pair, receipt) in receipts) {
            val other =
                when (record.syncId) {
                    pair.owner -> pair.target
                    pair.target -> pair.owner
                    else -> continue
                }
            val destination =
                records.singleOrNull { it.syncId == other && !it.detached } ?: continue
            // A link may have been removed and recreated between polls. Preserve its semantics.
            if (links.any { it.issue_link_id == receipt.linkId || it.id == destination.remoteId })
                continue
            // A shared link cannot express which of several typed/directed relations to remove.
            if (receipt.members.size != 1) continue
            val before =
                record.baseline?.relations?.singleOrNull {
                    member(record.syncId, it) in receipt.members
                } ?: continue
            val current = result.singleOrNull { it.id == before.id } ?: continue
            require(current == before) { "Native relation removed while its metadata changed" }
            result.remove(current)
        }
        for (link in links.filter { it.link_type == "relates_to" }) {
            if (receipts.any { it.second.linkId == link.issue_link_id }) continue
            if (records.any { link.issue_link_id in it.nativeLinks.values }) continue
            val target = records.singleOrNull { it.remoteId == link.id && !it.detached } ?: continue
            // One deterministic source imports a symmetric link; null means an untyped relation.
            if (record.syncId >= target.syncId) continue
            if (
                result.any { it.target == target.syncId } ||
                    target.baseline?.relations.orEmpty().any { it.target == record.syncId }
            )
                continue
            result += RelationValue("gl-link-${link.issue_link_id}", target.syncId)
        }
        return result
    }
}
