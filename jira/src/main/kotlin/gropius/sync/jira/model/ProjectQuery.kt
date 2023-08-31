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
data class ChangleLogEntry(
    val id: String, val author: JsonObject, val created: String, val items: List<ChangelogFieldEntry>
)

@Serializable
@Document
data class ChangleLogContainer(val histories: List<ChangleLogEntry>)

@Serializable
data class IssueBean(
    val expand: String,
    val id: String,
    val self: String,
    val key: String,
    val editmeta: JsonObject,
    val changelog: ChangleLogContainer,
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
    val changelog: ChangleLogContainer,
    val fields: Map<String, JsonElement>,
    val names: JsonObject,
    val schema: JsonObject
) {
    fun data(imsProject: IMSProject) = IssueData(
        imsProject.rawId!!, expand, id, self, key, editmeta, changelog, fields.toMutableMap(), names, schema
    );
}

@Serializable
data class ProjectQuery(
    var issues: List<IssueBean>, val names: JsonObject, val schema: JsonObject
) {
    fun issues(imsProject: IMSProject): List<IssueData> = issues.map { it.data(imsProject, names, schema) }
}