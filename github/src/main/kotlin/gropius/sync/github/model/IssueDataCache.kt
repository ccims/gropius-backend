package gropius.sync.github.model

import gropius.sync.github.generated.fragment.IssueDataExtensive
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

/**
 * Cache for unprocessed issues
 */
@Document
data class IssueDataCache(
    /**
     * ID on github
     */
    @Indexed(unique = true)
    var githubId: String,
    /**
     * Owner of the parent repo
     */
    val user: String,
    /**
     * Parent repo
     */
    val repo: String,
    /**
     * Data from github api
     */
    val data: IssueDataExtensive,
    /**
     * Number of attempts tried to insert into db
     */
    var attempts: Int?
) {
    /**
     * MongoDB ID
     */
    @Id
    var id: ObjectId? = null
}