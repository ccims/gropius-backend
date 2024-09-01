package gropius.sync.github.config

import com.lectra.koson.obj
import gropius.model.template.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.neo4j.core.ReactiveNeo4jOperations
import org.springframework.data.neo4j.core.findAll
import org.springframework.stereotype.Component

/**
 * Find and ensure the IMSTemplate in the database
 * @param neoOperations Reference for the spring instance of ReactiveNeo4jOperations
 */
@Component
class IMSConfigManager(
    @Qualifier("graphglueNeo4jOperations")
    private val neoOperations: ReactiveNeo4jOperations,
) {

    companion object {
        /**
         * Template fields for IMSTemplate and IMSProjectTemplate
         */
        val COMMON_TEMPLATE_FIELDS = mapOf("bot-user" to obj {
            "nullable" to true
            "type" to "string"
        }.toString(), "last-notification" to obj {
            "nullable" to true
            "properties" to obj {
                "title" to obj {
                    "type" to "string"
                }
                "content" to obj {
                    "type" to "string"
                }
            }
            "metadata" to obj {
                "gropius-type" to "notification"
            }
        }.toString())

        /**
         * Name of the ensured IMSIssueTemplate
         */
        private const val IMS_ISSUE_TEMPLATE_NAME = "Github Issue"

        /**
         * Fields of the required IMSIssueTemplate
         */
        private val IMS_ISSUE_TEMPLATE_FIELDS = mapOf("last-notification" to obj {
            "nullable" to true
            "properties" to obj {
                "title" to obj {
                    "type" to "string"
                }
                "content" to obj {
                    "type" to "string"
                }
            }
            "metadata" to obj {
                "gropius-type" to "notification"
            }
        }.toString(), "url" to obj {
            "type" to "string"
            "metadata" to obj {
                "format" to "uri"
            }
        }.toString(), "id" to obj {
            "type" to "int32"
        }.toString(), "number" to obj {
            "type" to "int32"
        }.toString()
        )

        /**
         * Name of the ensured IMSIssueTemplate
         */
        private const val IMS_USER_TEMPLATE_NAME = "Github User"

        /**
         * Fields of the required IMSIssueTemplate
         */
        private val IMS_USER_TEMPLATE_FIELDS = mapOf(
            "github_id" to obj {
                "nullable" to true
                "type" to "int32"
            }.toString()
        )
    }

    /**
     * Check if a given Template can be used as if it was the reference template.
     * @param possibleTemplate a random template
     * @param referenceName the title to check
     * @param referenceType check if all values specified by possibleTemplate are valid using the referenceType schemas
     * @return true if valid
     */
    private fun isContentCompatible(
        possibleTemplate: BaseTemplate<*, *>, referenceName: String, referenceType: Map<String, String>
    ): Boolean {
        //TODO: Replace with matching logic of template update operation
        if (possibleTemplate.name != referenceName) {
            return false;
        }
        for ((destinationKey, destinationType) in referenceType) {
            val destinationTypedef = Json.parseToJsonElement(destinationType).jsonObject
            if (possibleTemplate.templateFieldSpecifications.containsKey(destinationKey)) {
                val possibleType = possibleTemplate.templateFieldSpecifications[destinationKey]
                if (destinationType == possibleType) {
                    continue
                } else {
                    //TODO: More graceful comparison
                    return false
                }
            } else if (destinationTypedef["nullable"]?.jsonPrimitive?.boolean == true) {
                continue
            } else {
                return false
            }
        }
        return true
    }

    /**
     * Check if a given Template is identical.
     * @param possibleTemplate a random template
     * @param referenceName the title to check
     * @param referenceType check if all values specified by possibleTemplate are valid using the referenceType schemas
     * @return true if valid
     */
    private fun isContentIdentical(
        possibleTemplate: BaseTemplate<*, *>, referenceName: String, referenceType: Map<String, String>
    ): Boolean {
        return (possibleTemplate.name == referenceName) && (possibleTemplate.templateFieldSpecifications == referenceType)
    }

    /**
     * Find a matching set of IMSTemplate, IMSProjectTemplate and IMSIssueTemplate
     * @return the set of templates
     */
    suspend fun findTemplates(): Set<IMSTemplate> {
        val acceptableTemplates = neoOperations.findAll<IMSTemplate>().filter {
            (!it.isDeprecated) && isContentCompatible(
                it, IMSConfig.IMS_TEMPLATE_NAME, IMSConfig.IMS_TEMPLATE_FIELDS
            ) && isContentCompatible(
                it.imsProjectTemplate().value,
                IMSProjectConfig.IMS_PROJECT_TEMPLATE_NAME,
                IMSProjectConfig.IMS_PROJECT_TEMPLATE_FIELDS
            ) && isContentCompatible(
                it.imsIssueTemplate().value, IMS_ISSUE_TEMPLATE_NAME, IMS_ISSUE_TEMPLATE_FIELDS
            ) && isContentCompatible(
                it.imsUserTemplate().value, IMS_USER_TEMPLATE_NAME, IMS_USER_TEMPLATE_FIELDS
            )
        }.toSet().toMutableSet()
        val identicalTemplates = neoOperations.findAll<IMSTemplate>().filter {
            (!it.isDeprecated) && isContentIdentical(
                it, IMSConfig.IMS_TEMPLATE_NAME, IMSConfig.IMS_TEMPLATE_FIELDS
            ) && isContentIdentical(
                it.imsProjectTemplate().value,
                IMSProjectConfig.IMS_PROJECT_TEMPLATE_NAME,
                IMSProjectConfig.IMS_PROJECT_TEMPLATE_FIELDS
            ) && isContentIdentical(
                it.imsIssueTemplate().value, IMS_ISSUE_TEMPLATE_NAME, IMS_ISSUE_TEMPLATE_FIELDS
            ) && isContentIdentical(
                it.imsUserTemplate().value, IMS_USER_TEMPLATE_NAME, IMS_USER_TEMPLATE_FIELDS
            )
        }.toSet().toMutableSet()
        if (identicalTemplates.isEmpty()) {
            val imsTemplate =
                IMSTemplate(IMSConfig.IMS_TEMPLATE_NAME, "", IMSConfig.IMS_TEMPLATE_FIELDS.toMutableMap(), false)
            imsTemplate.imsProjectTemplate().value = IMSProjectTemplate(
                IMSProjectConfig.IMS_PROJECT_TEMPLATE_NAME,
                "",
                IMSProjectConfig.IMS_PROJECT_TEMPLATE_FIELDS.toMutableMap()
            )
            imsTemplate.imsIssueTemplate().value = IMSIssueTemplate(
                IMS_ISSUE_TEMPLATE_NAME, "", IMS_ISSUE_TEMPLATE_FIELDS.toMutableMap()
            )
            imsTemplate.imsUserTemplate().value = IMSUserTemplate(
                IMS_USER_TEMPLATE_NAME, "", IMS_USER_TEMPLATE_FIELDS.toMutableMap()
            )
            imsTemplate.imsUserTemplate().value.partOf().value = imsTemplate
            acceptableTemplates += (neoOperations.save(imsTemplate).awaitSingle())
        }
        return acceptableTemplates
    }

}
