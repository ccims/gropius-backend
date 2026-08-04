package gropius.model.issue.timeline

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.expediagroup.graphql.generator.annotations.GraphQLType
import tools.jackson.databind.ObjectMapper
import io.github.graphglue.model.DomainNode
import io.github.graphglue.model.FilterProperty
import org.springframework.beans.factory.annotation.Autowired
import java.time.OffsetDateTime

@DomainNode
@GraphQLDescription(
    """Event representing that a templated field was removed.
    READ is granted if READ is granted on `issue`.
    """
)
class RemovedTemplatedFieldEvent(
    createdAt: OffsetDateTime,
    lastModifiedAt: OffsetDateTime,
    @property:GraphQLDescription("The name of the templated field.")
    @FilterProperty
    val fieldName: String,
    @GraphQLIgnore
    val oldValue: String
) : PublicTimelineItem(createdAt, lastModifiedAt) {

    @GraphQLDescription("The removed old value of the templated field.")
    @GraphQLType("JSON")
    fun oldValue(
        @Autowired
        @GraphQLIgnore
        objectMapper: ObjectMapper
    ): Any? {
        return objectMapper.readTree(oldValue)
    }

}