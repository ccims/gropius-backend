package gropius.sync.github.config

import com.lectra.koson.arr
import com.lectra.koson.obj
import gropius.model.architecture.IMS
import gropius.model.template.IMSTemplate
import gropius.sync.JsonHelper
import java.net.URI

/**
 * Config read out from a single IMS node
 * @param ims the Gropius ims to use as input
 * @param botUser the bot user string extracted from the template
 * @param readUser the read user string extracted from the template
 * @param graphQLUrl the read url extracted from the template
 * @param imsTemplate the template of the current IMS
 */
data class IMSConfig(
    val botUser: String, val readUser: String, val graphQLUrl: URI, val imsTemplate: IMSTemplate
) {
    /**
     * @param ims the Gropius ims to use as input
     * @param helper Reference for the spring instance of JsonHelper
     * @param imsTemplate the template of the current IMS
     */
    constructor(
        helper: JsonHelper, ims: IMS, imsTemplate: IMSTemplate
    ) : this(
        botUser = helper.parseString(ims.templatedFields["bot-user"]) ?: "github-bot",
        readUser = helper.parseString(ims.templatedFields["read-user"])!!,
        graphQLUrl = URI(helper.parseString(ims.templatedFields["graphql-url"])!!),
        imsTemplate = imsTemplate
    )

    companion object {
        /**
         * Name of the requested IMSTemplate
         */
        const val IMS_TEMPLATE_NAME = "Github"

        /**
         * Fields of the requested IMSTemplate
         */
        val IMS_TEMPLATE_FIELDS = mapOf("read-user" to obj {
            "\$schema" to IMSConfigManager.SCHEMA
            "type" to arr["null", obj {
                "type" to "string"
                "gropius-node" to "IMSUser"
                "gropius-type" to "github-user"
            }]
        }.toString(), "graphql-url" to obj {
            "\$schema" to IMSConfigManager.SCHEMA
            "type" to "string"
            "format" to "uri"
        }.toString()) + IMSConfigManager.COMMON_TEMPLATE_FIELDS
    }
}
