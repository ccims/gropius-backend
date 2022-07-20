package gropius.sync.github.model

import gropius.sync.github.generated.fragment.TimelineItemData
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

/**
 * Cache for unprocessed timeline items
 */
@Document
data class TimelineItemDataCache(
    /**
     * ID on github
     */
    @Indexed(unique = true)
    var githubId: String,
    /**
     * Github ID of the associated issue
     */
    val issue: String,
    /**
     * Raw Github API data
     */
    val data: TimelineItemData,
    /**
     * Number of attempts to sync into gropius database
     */
    var attempts: Int?
) {
    /**
     * MongoDB ID
     */
    @Id
    var id: ObjectId? = null
}