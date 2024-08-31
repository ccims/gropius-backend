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
     */
    fun validateLayout() {
        relationPartnerLayouts.ifPresent {
            it.forEach(UpdateRelationPartnerLayoutInput::validate)
        }
        relationLayouts.ifPresent {
            it.forEach(UpdateRelationLayoutInput::validate)
        }
    }

}