package gropius.sync.jira.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class IssueTypeRequest(
    val name: String
) {}

@Serializable
data class ProjectRequest(
    val key: String
) {}

@Serializable
data class IssueQueryRequestFields(
    val summary: String,
    val description: String,
    val issuetype: IssueTypeRequest,
    val project: ProjectRequest,
    val components: List<JsonElement>
)

@Serializable
data class IssueQueryRequest(val fields: IssueQueryRequestFields) {}