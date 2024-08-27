package gropius.sync.jira.config

import com.lectra.koson.arr
import com.lectra.koson.obj
import gropius.model.architecture.IMS
import gropius.model.template.IMSTemplate
import gropius.sync.JsonHelper
import java.net.URI

/**
 * Config read out from a single IMS node
 * @param botUser the bot user string extracted from the template
 * @param readUser the read user string extracted from the template
 * @param rootUrl the read url extracted from the template
 * @param imsTemplate the template of the current IMS
 * @param defaultType the type of newly created issues
 * @param defaultTemplate the template of newly created issues
 */
data class IMSConfig(
    val botUser: String,
    val readUser: String?,
    val rootUrl: URI,
    val imsTemplate: IMSTemplate,
    val defaultType: String?,
    val defaultTemplate: String?,
    val isCloud: Boolean?
) {
    /**
     * @param ims the Gropius ims to use as input
     * @param helper Reference for the spring instance of JsonHelper
     * @param imsTemplate the template of the current IMS
     */
    constructor(
        helper: JsonHelper, ims: IMS, imsTemplate: IMSTemplate
    ) : this(
        botUser = helper.parseString(ims.templatedFields["bot-user"]) ?: "jira-bot",
        readUser = helper.parseString(ims.templatedFields["read-user"]),
        rootUrl = URI(helper.parseString(ims.templatedFields["root-url"])!!),
        imsTemplate = imsTemplate,
        defaultType = helper.parseString(ims.templatedFields["default-type"]),
        defaultTemplate = helper.parseString(ims.templatedFields["default-template"]),
        isCloud = helper.parseString(ims.templatedFields["jira-edition"])?.let { it == "CLOUD" },
    )

    companion object {
        /**
         * Name of the requested IMSTemplate
         */
        const val IMS_TEMPLATE_NAME = "Jira"

        /**
         * Fields of the requested IMSTemplate
         */
        val IMS_TEMPLATE_FIELDS = mapOf("read-user" to obj {
            "nullable" to true
            "type" to "string"
            "metadata" to obj {
                "gropius-node" to "IMSUser"
                "gropius-type" to "jira-user"
            }
        }.toString(), "root-url" to obj {
            "type" to "string"
            "metadata" to obj {
                "format" to "uri"
            }
        }.toString(), "jira-edition" to obj {
            "enum" to arr["CLOUD", "DATA_CENTER"]
        }.toString(), "default-type" to obj {
            "nullable" to true
            "type" to "string"
            "metadata" to obj {
                "gropius-node" to "IssueType"
            }
        }.toString(), "default-template" to obj {
            "nullable" to true
            "type" to "string"
            "metadata" to obj {
                "gropius-node" to "IssueTemplate"
            }
        }.toString()) + IMSConfigManager.COMMON_TEMPLATE_FIELDS
    }
}
