package gropius.model.issue.timeline

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import io.github.graphglue.model.*
import java.time.OffsetDateTime

/**
 * Abstract superclass for multiple type changed events
 *
 * @param T the Type of the type
 */
@DomainNode
@GraphQLIgnore
abstract class AbstractTypeChangedEvent<T : Node?>(
    createdAt: OffsetDateTime, lastModifiedAt: OffsetDateTime
) : PublicTimelineItem(createdAt, lastModifiedAt) {

    companion object {
        const val OLD_TYPE = "OLD_TYPE"
        const val NEW_TYPE = "NEW_TYPE"
    }

    @NodeRelationship(OLD_TYPE, Direction.OUTGOING)
    @GraphQLDescription("The old type.")
    @FilterProperty
    val oldType by NodeProperty<T>()

    @NodeRelationship(NEW_TYPE, Direction.OUTGOING)
    @GraphQLDescription("The new type.")
    @FilterProperty
    val newType by NodeProperty<T>()

}