package gropius.sync.jira.config

import com.lectra.koson.arr
import com.lectra.koson.obj
import gropius.model.template.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.reactor.awaitSingle
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
         * JSON schema version to include
         */
        const val SCHEMA = "https://json-schema.org/draft/2020-12/schema"

        /**
         * Template fields for IMSTemplate and IMSProjectTemplate
         */
        val COMMON_TEMPLATE_FIELDS = mapOf("bot-user" to obj {
            "\$schema" to SCHEMA
            "type" to arr["null", "string"]
        }.toString(), "last-notification" to obj {
            "\$schema" to SCHEMA
            "type" to arr["null", obj {
                "type" to "object"
                "properties" to obj {
                    "title" to obj {
                        "type" to "string"
                    }
                    "content" to obj {
                        "type" to "string"
                    }
                }
                "required" to arr["title", "content"]
                "gropius-type" to "notification"
            }]
        }.toString())

        /**
         * Name of the ensured IMSIssueTemplate
         */
        private const val IMS_ISSUE_TEMPLATE_NAME = "Jira Issue"

        /**
         * Fields of the required IMSIssueTemplate
         */
        private val IMS_ISSUE_TEMPLATE_FIELDS = mapOf("last-notification" to obj {
            "\$schema" to SCHEMA
            "type" to arr["null", obj {
                "type" to "object"
                "properties" to obj {
                    "title" to obj {
                        "type" to "string"
                    }
                    "content" to obj {
                        "type" to "string"
                    }
                }
                "required" to arr["title", "content"]
                "gropius-type" to "notification"
            }]
        }.toString(), "url" to obj {
            "\$schema" to SCHEMA
            "type" to "string"
            "format" to "uri"
        }.toString(), "id" to obj {
            "\$schema" to SCHEMA
            "type" to "number"
        }.toString(), "number" to obj {
            "\$schema" to SCHEMA
            "type" to "number"
        }.toString()
        )

        /**
         * Name of the ensured IMSIssueTemplate
         */
        private const val IMS_USER_TEMPLATE_NAME = "Jira User"

        /**
         * Fields of the required IMSIssueTemplate
         */
        private val IMS_USER_TEMPLATE_FIELDS = mapOf(
            "github_id" to obj {
                "\$schema" to SCHEMA
                "type" to arr["null", "string"]
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
        if (acceptableTemplates.isEmpty()) {
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
