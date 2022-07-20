package gropius.sync.github.model

import gropius.model.issue.Issue
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.neo4j.core.ReactiveNeo4jOperations
import org.springframework.data.neo4j.core.findById
import java.time.OffsetDateTime

/**
 * Mapping of a single issue from neo4j to github
 */
@Document
data class IssueInfo(
    /**
     * ID on github
     */
    @Indexed(unique = true)
    var githubId: String,
    /**
     * ID in gropius database
     */
    @Indexed(unique = true)
    var neo4jId: String,
    /**
     * True if changed after last access and has to be queried
     */
    val dirty: Boolean,
    /**
     * Time of the last accessed timeline item
     */
    var lastAccess: OffsetDateTime?
) {
    /**
     * MongoDB ID
     */
    @Id
    var id: ObjectId? = null
    suspend fun load(neoOperations: ReactiveNeo4jOperations): Issue {
        return neoOperations.findById<Issue>(neo4jId)!!
    }
}