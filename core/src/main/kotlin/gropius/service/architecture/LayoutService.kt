package gropius.service.architecture

import gropius.dto.input.architecture.layout.UpdateLayoutInput
import gropius.dto.input.orElse
import gropius.model.architecture.layout.Layout
import gropius.model.architecture.layout.RelationLayout
import gropius.model.architecture.layout.RelationPartnerLayout
import gropius.repository.architecture.RelationPartnerRepository
import gropius.repository.architecture.RelationRepository
import gropius.repository.findById
import gropius.service.NodeBatchUpdater
import org.springframework.stereotype.Service

/**
 * Service for updating the layout of a [Layout]
 *
 * @param relationPartnerRepository The [RelationPartnerRepository] to use for updating the layout
 * @param relationRepository The [RelationRepository] to use for updating the layout
 */
@Service
class LayoutService(
    private val relationPartnerRepository: RelationPartnerRepository,
    private val relationRepository: RelationRepository,
) {

    /**
     * Updates the layout of a [Layout]
     *
     * @param layout The layout to update
     * @param input Defines new layout values
     * @param batchUpdater The [NodeBatchUpdater] to use for updating the layout
     */
    suspend fun updateLayout(layout: Layout, input: UpdateLayoutInput, batchUpdater: NodeBatchUpdater) {
        updateRelationPartnerLayouts(layout, input, batchUpdater)
        updateRelationLayouts(layout, input, batchUpdater)
    }

    /**
     * Updates the layout of the [RelationPartnerLayout]s of a [Layout]
     *
     * @param layout The layout to update
     * @param input Defines new layout values
     * @param batchUpdater The [NodeBatchUpdater] to use for updating the layout
     */
    private suspend fun updateRelationPartnerLayouts(
        layout: Layout, input: UpdateLayoutInput, batchUpdater: NodeBatchUpdater,
    ) {
        val cache = batchUpdater.cache
        val relationPartnerLayouts =
            layout.relationPartnerLayouts(cache).associateBy { it.relationPartner(cache).value.rawId!! }
        input.relationPartnerLayouts.orElse(emptyList()).forEach {
            if (it.layout != null) {
                val existingLayout = relationPartnerLayouts[it.relationPartner.value]
                if (existingLayout == null) {
                    val newLayout = RelationPartnerLayout(it.layout.pos.x, it.layout.pos.y)
                    newLayout.relationPartner(cache).value = relationPartnerRepository.findById(it.relationPartner)
                    batchUpdater.internalUpdatedNodes += newLayout
                    layout.relationPartnerLayouts() += newLayout
                } else {
                    existingLayout.x = it.layout.pos.x
                    existingLayout.y = it.layout.pos.y
                    batchUpdater.internalUpdatedNodes += existingLayout
                }
            } else {
                relationPartnerLayouts[it.relationPartner.value]?.let { layout ->
                    batchUpdater.deletedNodes += layout
                }
            }
        }
    }

    /**
     * Updates the layout of the [RelationLayout]s of a [Layout]
     *
     * @param layout The layout to update
     * @param input Defines new layout values
     * @param batchUpdater The [NodeBatchUpdater] to use for updating the layout
     */
    private suspend fun updateRelationLayouts(
        layout: Layout, input: UpdateLayoutInput, batchUpdater: NodeBatchUpdater,
    ) {
        val cache = batchUpdater.cache
        val relationLayouts = layout.relationLayouts(cache).associateBy { it.relation(cache).value.rawId!! }
        input.relationLayouts.orElse(emptyList()).forEach {
            if (it.layout != null) {
                val existingLayout = relationLayouts[it.relation.value]
                val xCoordinates = it.layout.points.map { point -> point.x }.toIntArray()
                val yCoordinates = it.layout.points.map { point -> point.y }.toIntArray()
                if (existingLayout == null) {
                    val newLayout = RelationLayout(xCoordinates, yCoordinates)
                    newLayout.relation(cache).value = relationRepository.findById(it.relation)
                    batchUpdater.internalUpdatedNodes += newLayout
                    layout.relationLayouts() += newLayout
                } else {
                    existingLayout.xCoordinates = xCoordinates
                    existingLayout.yCoordinates = yCoordinates
                    batchUpdater.internalUpdatedNodes += existingLayout
                }
            } else {
                relationLayouts[it.relation.value]?.let { layout ->
                    batchUpdater.deletedNodes += layout
                }
            }
        }
    }
}