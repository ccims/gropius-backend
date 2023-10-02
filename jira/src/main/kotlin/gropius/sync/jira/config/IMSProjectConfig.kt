package gropius.sync.jira.config

import com.lectra.koson.arr
import com.lectra.koson.obj
import gropius.model.architecture.IMSProject
import gropius.sync.JsonHelper

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
    val repo: String,
    val enableOutgoing: Boolean,
    val enableOutgoingLabels: Boolean,
    val enableOutgoingComments: Boolean
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
        repo = helper.parseString(imsProject.templatedFields["repo"])!!,
        enableOutgoing = helper.parseBoolean(imsProject.templatedFields["enable-outgoing"]),
        enableOutgoingLabels = helper.parseBoolean(imsProject.templatedFields["enable-outgoing-labels"]),
        enableOutgoingComments = helper.parseBoolean(imsProject.templatedFields["enable-outgoing-comments"])
    )

    companion object {
        /**
         * Name of requested IMSProjectTemplate
         */
        const val IMS_PROJECT_TEMPLATE_NAME = "Jira"

        /**
         * Fields of the requested IMSProjectTemplate
         */
        val IMS_PROJECT_TEMPLATE_FIELDS = mapOf("repo" to obj {
            "\$schema" to IMSConfigManager.SCHEMA
            "type" to "string"
        }.toString(), "enable-outgoing" to obj {
            "\$schema" to IMSConfigManager.SCHEMA
            "type" to arr["null", "boolean"]
        }.toString(), "enable-outgoing-labels" to obj {
            "\$schema" to IMSConfigManager.SCHEMA
            "type" to arr["null", "boolean"]
        }.toString(), "enable-outgoing-comments" to obj {
            "\$schema" to IMSConfigManager.SCHEMA
            "type" to arr["null", "boolean"]
        }.toString()) + IMSConfigManager.COMMON_TEMPLATE_FIELDS
    }
}
