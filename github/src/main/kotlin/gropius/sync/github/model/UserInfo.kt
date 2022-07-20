package gropius.sync.github.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

/**
 * Mapping of a single user from neo4j to github
 */
@Document
data class UserInfo(
    /**
     * username on github
     */
    @Indexed(unique = true)
    val login: String,
    /**
     * ID in gropius database
     */
    @Indexed(unique = true)
    val neo4jId: String
) {
    /**
     * MongoDB ID
     */
    @Id
    var id: ObjectId? = null
}