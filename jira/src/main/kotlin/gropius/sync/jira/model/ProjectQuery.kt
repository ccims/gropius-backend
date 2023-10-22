package gropius.sync.jira.model

import gropius.model.architecture.IMSProject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.springframework.data.mongodb.core.mapping.Document

@Serializable
@Document
data class ChangelogFieldEntry(
    val field: String,
    val fieldtype: String,
    val fieldId: String,
    val from: JsonElement,
    val to: JsonElement,
    val fromString: String?,
    val toString: String?
)

@Serializable
@Document
data class ChangeLogEntry(
    val id: String, val author: JsonObject, val created: String, val items: List<ChangelogFieldEntry>
)

@Serializable
@Document
data class ChangeLogContainer(val histories: List<ChangeLogEntry>)

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

@Serializable
data class CommentQuery(
    var comments: List<JiraComment>, val startAt: Int, val total: Int
) {}

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