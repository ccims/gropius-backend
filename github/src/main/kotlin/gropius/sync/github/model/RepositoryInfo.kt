package gropius.sync.github.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.OffsetDateTime

/**
 * Mapping of a single repository from neo4j to github
 */
@Document
data class RepositoryInfo(
    /**
     * user/orga on github
     */
    @Indexed
    val user: String,
    /**
     * repo on github
     */
    @Indexed
    val repo: String,
    /**
     * Time of the last item of the last issue query
     */
    var lastAccess: OffsetDateTime
) {
    /**
     * MongoDB ID
     */
    @Id
    var id: ObjectId? = null
}