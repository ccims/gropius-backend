package gropius.sync.jira.config

import com.lectra.koson.arr
import com.lectra.koson.obj
import gropius.model.architecture.IMSProject
import gropius.sync.JsonHelper
import gropius.sync.jira.model.RepoDescription

/**
 * Config read out from a single IMSProject and an IMSConfig node
 * @param imsProject the Gropius IMSProject to use as input
 * @param imsConfig the config of the parent IMS
 * @param botUser bot user name
 * @param readUser read user name
 * @param repo repository url
 */
data class IMSProjectConfig(
    val botUser: String?, val repo: RepoDescription
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
        )
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
            "\$schema" to IMSConfigManager.SCHEMA
            "type" to "object"
            "properties" to obj {
                "owner" to obj {
                    "type" to "string"
                }
                "repo" to obj {
                    "type" to "string"
                }
            }
            "required" to arr["owner", "repo"]
            "gropius-type" to "github-owner"
        }.toString()) + IMSConfigManager.COMMON_TEMPLATE_FIELDS
    }
}
