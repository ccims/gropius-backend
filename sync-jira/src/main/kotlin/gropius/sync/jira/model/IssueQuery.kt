package gropius.sync.jira.model

import kotlinx.serialization.json.JsonElement

/**
 * Kotlin representation of the IssueTypeRequest JSON
 * @param name The name of the issue type
 */
@Serializable
data class IssueTypeRequest(
    val name: String
) {}

/**
 * Kotlin representation of the ProjectRequest JSON
 * @param key The key of the project
 */
@Serializable
data class ProjectRequest(
    val key: String
) {}

/**
 * Kotlin representation of the IssueQueryRequest JSON
 * @param summary The title of the issue
 * @param description The description of the issue
 * @param issuetype The issue type of the issue
 * @param project The project of the issue
 * @param components The components of the issue
 */
@Serializable
data class IssueQueryRequestFields(
    val summary: String,
    val description: String,
    val issuetype: IssueTypeRequest,
    val project: ProjectRequest,
    val components: List<JsonElement>
)

/**
 * Kotlin representation of the IssueQueryRequest JSON
 * @param fields The fields of the issue
 */
@Serializable
data class IssueQueryRequest(val fields: IssueQueryRequestFields) {}