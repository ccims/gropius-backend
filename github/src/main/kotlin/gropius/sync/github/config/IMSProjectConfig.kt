package gropius.sync.github.config

import com.lectra.koson.obj
import gropius.model.architecture.IMSProject
import gropius.sync.JsonHelper
import gropius.sync.github.model.RepoDescription

/**
 * Config read out from a single IMSProject and an IMSConfig node
 * @param imsProject the Gropius IMSProject to use as input
 * @param imsConfig the config of the parent IMS
 * @param botUser bot user name
 * @param readUser read user name
 * @param repo repository url
 */
data class IMSProjectConfig(
    val botUser: String?,
    val repo: RepoDescription,
    val enableOutgoing: Boolean,
    val enableOutgoingLabels: Boolean,
    val enableOutgoingComments: Boolean,
    val enableOutgoingAssignments: Boolean,
    val enableOutgoingTitleChanges: Boolean,
    val enableOutgoingState: Boolean,
    val defaultType: String?,
    val defaultTemplate: String?
) {
    /**
     * @param imsProject the Gropius IMSProject to use as input
     * @param helper Reference for the spring instance of JsonHelper
     * @param imsConfig the config of the parent IMS
     */
    constructor(
        helper: JsonHelper, imsProject: IMSProject
    ) : this(
        botUser = helper.parseString(imsProject.templatedFields["bot-user"]),
        repo = helper.objectMapper.readValue<RepoDescription>(
            imsProject.templatedFields["repo"]!!, RepoDescription::class.java
        ),
        enableOutgoing = helper.parseBoolean(imsProject.templatedFields["enable-outgoing"]),
        enableOutgoingLabels = helper.parseBoolean(imsProject.templatedFields["enable-outgoing-labels"]),
        enableOutgoingComments = helper.parseBoolean(imsProject.templatedFields["enable-outgoing-comments"]),
        enableOutgoingAssignments = helper.parseBoolean(imsProject.templatedFields["enable-outgoing-assignments"]),
        enableOutgoingTitleChanges = helper.parseBoolean(imsProject.templatedFields["enable-outgoing-title-changes"]),
        enableOutgoingState = helper.parseBoolean(imsProject.templatedFields["enable-outgoing-state"]),
        defaultType = helper.parseString(imsProject.templatedFields["default-type"]),
        defaultTemplate = helper.parseString(imsProject.templatedFields["default-template"])
    )

    companion object {
        /**
         * Name of requested IMSProjectTemplate
         */
        const val IMS_PROJECT_TEMPLATE_NAME = "Github"

        /**
         * Fields of the requested IMSProjectTemplate
         */
        val IMS_PROJECT_TEMPLATE_FIELDS = mapOf("repo" to obj {
            "properties" to obj {
                "owner" to obj {
                    "type" to "string"
                }
                "repo" to obj {
                    "type" to "string"
                }
            }
            "metadata" to obj {
                "gropius-type" to "github-owner"
            }
        }.toString(), "default-template" to obj {
            "nullable" to true
            "type" to "string"
            "metadata" to obj {
                "gropius-node" to "IssueTemplate"
            }
        }.toString(), "default-type" to obj {
            "nullable" to true
            "type" to "string"
            "metadata" to obj {
                "gropius-node" to "IssueType"
            }
        }.toString(), "enable-outgoing" to obj {
            "nullable" to true
            "type" to "boolean"
        }.toString(), "enable-outgoing-labels" to obj {
            "nullable" to true
            "type" to "boolean"
        }.toString(), "enable-outgoing-comments" to obj {
            "nullable" to true
            "type" to "boolean"
        }.toString(), "enable-outgoing-assignments" to obj {
            "nullable" to true
            "type" to "boolean"
        }.toString(), "enable-outgoing-title-changes" to obj {
            "nullable" to true
            "type" to "boolean"
        }.toString(), "enable-outgoing-state" to obj {
            "nullable" to true
            "type" to "boolean"
        }.toString()) + IMSConfigManager.COMMON_TEMPLATE_FIELDS
    }
}
