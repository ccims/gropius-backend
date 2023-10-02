package gropius.sync.github.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

/**
 * Mapping of a single label from neo4j to GitHub
 * @param url API URL of IMS of the repo
 * @param githubId ID on GitHub
 * @param neo4jId ID in gropius database
 */
@Document
data class LabelInfo(
    @Indexed
    val imsProject: String,
    @Indexed
    val githubId: String,
    @Indexed(unique = true)
    val neo4jId: String
) {
    /**
     * MongoDB ID
     */
    @Id
    var id: ObjectId? = null
}
