package gropius.sync.jira.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Object describing the identification for a GitHub repo
 * @param owner first part of url path part
 * @param repo second part of url path part
 */
data class RepoDescription(
    @JsonProperty("owner")
    val owner: String,
    @JsonProperty("repo")
    val repo: String
)