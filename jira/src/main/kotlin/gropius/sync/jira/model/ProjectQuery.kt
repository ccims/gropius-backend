package gropius.sync.jira.model

import gropius.model.architecture.IMSProject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.springframework.data.mongodb.core.mapping.Document

/**
 * A field in the changelog returned by Jira
 * @param field The name of the field
 * @param fieldtype The type of the field
 * @param fieldId The id of the field
 * @param from The old value of the field
 * @param to The new value of the field
 * @param fromString The old value of the field if a string
 * @param toString The new value of the field if a string
 */
@Serializable
@Document
data class ChangelogFieldEntry(
    val field: String,
    val fieldtype: String,
    val fieldId: String? = null,
    val from: String?,
    val to: String?,
    val fromString: String?,
    val toString: String?
)

/**
 * A single changelog entry
 * @param id The id of the entry
 * @param author The author of the entry
 * @param created The creation date of the entry
 * @param items The items of the entry
 */
@Serializable
@Document
data class ChangeLogEntry(
    val id: String, val author: JsonObject, val created: String, val items: List<ChangelogFieldEntry>
)

/**
 * A container for the changelog
 * @param histories The changelog entries
 */
@Serializable
@Document
data class ChangeLogContainer(val histories: MutableList<ChangeLogEntry>)

/**
 * A container for the changelog
 * @param histories The changelog entries
 */
@Serializable
@Document
data class ValueChangeLogContainer(val values: List<ChangeLogEntry>, val startAt: Int, val total: Int)

/**
 * Kotlin representation of the Issue JSON
 * @param expand The expand string
 * @param id The id of the issue
 * @param self The self link of the issue
 * @param key The key of the issue
 * @param editmeta The edit meta data of the issue
 * @param changelog The changelog of the issue
 * @param fields The fields of the issue
 */
@Serializable
data class IssueBean(
    val expand: String,
    val id: String,
    val self: String,
    val key: String,
    val editmeta: JsonObject,
    val changelog: ChangeLogContainer,
    val fields: Map<String, JsonElement>
) {
    fun data(
        imsProject: IMSProject, names: JsonObject, schema: JsonObject
    ) = IssueData(
        imsProject.rawId!!, expand, id, self, key, editmeta, changelog, fields.toMutableMap(), names, schema
    );
}

/**
 * Kotlin representation of the Issue JSON
 * @param expand The expand string
 * @param id The id of the issue
 * @param self The self link of the issue
 * @param key The key of the issue
 * @param editmeta The edit meta data of the issue
 * @param changelog The changelog of the issue
 * @param fields The fields of the issue
 * @param names The names of the issue
 * @param schema The schema of the issue
 */
@Serializable
data class IssueQuery(
    val expand: String,
    val id: String,
    val self: String,
    val key: String,
    val editmeta: JsonObject,
    val changelog: ChangeLogContainer,
    val fields: Map<String, JsonElement>,
    val names: JsonObject,
    val schema: JsonObject
) {
    fun data(imsProject: IMSProject) = IssueData(
        imsProject.rawId!!, expand, id, self, key, editmeta, changelog, fields.toMutableMap(), names, schema
    );
}

/**
 * Kotlin representation of the Issue JSON
 * @param id The id of the issue
 * @param self The self link of the issue
 * @param author The author of the comment
 * @param body The body of the comment
 * @param updateAuthor The update author of the comment
 * @param created The creation date of the comment
 * @param updated The update date of the comment
 * @param jsdPublic Whether the comment is public
 */
@Serializable
data class JiraComment(
    val id: String,
    val self: String,
    val author: JsonObject,
    val body: String,
    val updateAuthor: JsonObject,
    val created: String,
    val updated: String,
    val jsdPublic: Boolean
) {}

/**
 * Kotlin representation of the CommentQuery JSON
 * @param comments The comments of the issue
 * @param startAt The start index of the query
 * @param total The total number of comments
 */
@Serializable
data class CommentQuery(
    var comments: List<JiraComment>, val startAt: Int, val total: Int
) {}

/**
 * Kotlin representation of the ProjectQuery JSON
 * @param issues The issues of the project
 * @param startAt The start index of the query
 * @param total The total number of issues
 * @param names The names of the project
 * @param schema The schema of the project
 */
@Serializable
data class ProjectQuery(
    var issues: List<IssueBean>,
    val startAt: Int,
    val total: Int,
    val names: JsonObject? = null,
    val schema: JsonObject? = null
) {
    fun issues(imsProject: IMSProject): List<IssueData> =
        issues.map { it.data(imsProject, names ?: JsonObject(mapOf()), schema ?: JsonObject(mapOf())) }
}