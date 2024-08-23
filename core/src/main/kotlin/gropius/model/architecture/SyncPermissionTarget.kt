package gropius.model.architecture

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gropius.model.common.NamedNode
import gropius.model.user.GropiusUser
import io.github.graphglue.model.Direction
import io.github.graphglue.model.ExtensionField
import io.github.graphglue.model.FilterProperty
import io.github.graphglue.model.NodeRelationship

/**
 * Name of the bean which provides the sync allowed extension field
 */
const val SYNC_SELF_ALLOWED_FIELD_BEAN = "syncSelfAllowedFieldBean"

/**
 * Name of the bean which provides the sync allowed extension field
 */
const val SYNC_OTHERS_ALLOWED_FIELD_BEAN = "syncOthersAllowedFieldBean"

@GraphQLDescription("A target where users can configure how the sync should behave.")
@ExtensionField(SYNC_SELF_ALLOWED_FIELD_BEAN)
@ExtensionField(SYNC_OTHERS_ALLOWED_FIELD_BEAN)
abstract class SyncPermissionTarget(name: String, description: String) : NamedNode(name, description) {

    @NodeRelationship(GropiusUser.CAN_SYNC_SELF, Direction.INCOMING)
    @GraphQLDescription("The users which allow to sync their data to this target.")
    @FilterProperty
    val syncSelfAllowedBy by NodeSetProperty<GropiusUser>()

    @NodeRelationship(GropiusUser.CAN_SYNC_OTHERS, Direction.INCOMING)
    @GraphQLDescription("The users which allow to sync the data of other users to this target.")
    @FilterProperty
    val syncOthersAllowedBy by NodeSetProperty<GropiusUser>()

}