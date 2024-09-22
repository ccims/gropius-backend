package gropius.dto.input.architecture.layout

import com.expediagroup.graphql.generator.execution.OptionalInput
import gropius.dto.input.ifPresent

/**
 * Common input for updating the layout of a Project
 */
interface UpdateLayoutInput {

    /**
     * The layout of the RelationPartners
     */
    val relationPartnerLayouts: OptionalInput<List<UpdateRelationPartnerLayoutInput>>

    /**
     * The layout of the Relations
     */
    val relationLayouts: OptionalInput<List<UpdateRelationLayoutInput>>

    /**
     * Validates the [relationPartnerLayouts] and [relationLayouts]
     * Ensures that there is only one layout per RelationPartner and Relation
     */
    fun validateLayout() {
        relationPartnerLayouts.ifPresent { layouts ->
            layouts.forEach(UpdateRelationPartnerLayoutInput::validate)
            layouts.groupBy { it.relationPartner }.forEach { (id, group) ->
                if (group.size > 1) {
                    throw IllegalArgumentException("Multiple layouts for the same RelationPartner: $id")
                }
            }
        }
        relationLayouts.ifPresent { layouts ->
            layouts.forEach(UpdateRelationLayoutInput::validate)
            layouts.groupBy { it.relation }.forEach { (id, group) ->
                if (group.size > 1) {
                    throw IllegalArgumentException("Multiple layouts for the same Relation: $id")
                }
            }
        }

    }

}