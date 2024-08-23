package gropius.graphql

import com.expediagroup.graphql.generator.execution.KotlinDataFetcherFactoryProvider
import com.expediagroup.graphql.generator.execution.SimpleKotlinDataFetcherFactoryProvider
import com.expediagroup.graphql.server.execution.GraphQLRequestHandler
import com.expediagroup.graphql.server.spring.execution.SpringGraphQLContextFactory
import com.expediagroup.graphql.server.spring.execution.SpringGraphQLRequestParser
import com.expediagroup.graphql.server.spring.execution.SpringGraphQLServer
import com.expediagroup.graphql.server.types.GraphQLResponse
import com.expediagroup.graphql.server.types.GraphQLServerError
import com.expediagroup.graphql.server.types.GraphQLServerResponse
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import graphql.Scalars
import graphql.scalars.regex.RegexScalar
import graphql.schema.*
import gropius.authorization.GropiusAuthorizationContext
import gropius.authorization.checkPermission
import gropius.authorization.gropiusAuthorizationContext
import gropius.graphql.filter.*
import gropius.model.architecture.*
import gropius.model.common.PERMISSION_FIELD_BEAN
import gropius.model.issue.Issue
import gropius.model.issue.timeline.*
import gropius.model.template.TEMPLATED_FIELDS_FILTER_BEAN
import gropius.model.template.TemplatedNode
import gropius.model.user.GropiusUser
import gropius.model.user.NODE_PERMISSION_FILTER_BEAN
import gropius.model.user.permission.ALL_PERMISSION_ENTRY_NAME
import gropius.util.JsonNodeMapper
import io.github.graphglue.authorization.Permission
import io.github.graphglue.connection.filter.TypeFilterDefinitionEntry
import io.github.graphglue.connection.filter.definition.scalars.StringFilterDefinition
import io.github.graphglue.definition.ExtensionFieldDefinition
import io.github.graphglue.definition.NodeDefinition
import io.github.graphglue.definition.NodeDefinitionCollection
import io.github.graphglue.graphql.extensions.authorizationContext
import org.neo4j.cypherdsl.core.Cypher
import org.neo4j.cypherdsl.core.Expression
import org.neo4j.driver.Driver
import org.neo4j.driver.Value
import org.springframework.beans.factory.BeanFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.server.ResponseStatusException
import java.net.URI
import java.time.Duration
import java.time.OffsetDateTime
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubclassOf

/**
 * Name of the GraphQL enum providing all types of timeline items
 */
const val TIMELINE_ITEM_TYPE_NAME = "TimelineItemType"

/**
 * Contains bean necessary for GraphQL configuration
 * Provides additional scalars
 */
@Configuration
class GraphQLConfiguration {

    /**
     * Necessary transaction manager
     *
     * @param driver used Neo4j driver
     * @param databaseNameProvider Neo4j database provider
     * @return the generated transaction manager
     */
    @Bean
    fun reactiveTransactionManager(
        driver: Driver, databaseNameProvider: ReactiveDatabaseSelectionProvider
    ): ReactiveNeo4jTransactionManager {
        return ReactiveNeo4jTransactionManager(driver, databaseNameProvider)
    }

    /**
     * JSON scalar which can be used by annotating a function / property with `@GraphQLType("JSON")`
     *
     * @return the provided JSON scalar
     */
    @Bean
    fun jsonScalar(objectMapper: ObjectMapper) =
        GraphQLScalarType.newScalar().name("JSON").description("A JSON scalar").coercing(JSONCoercing(objectMapper))
            .build()

    /**
     * Color scalar which can be used by annotating a function / property with `@GraphQLType("Color")`
     *
     * @return the provided Color scalar
     */
    @Bean
    fun colorScalar() = RegexScalar.Builder().name("Color")
        .description("A color string with the format #[0-9a-fA-F]{6} with the semantics #RRGGBB")
        .addPattern("#[0-9a-fA-F]{6}".toRegex().toPattern())

    /**
     * SchemaGeneratorHooks which support the [TypeGraphQLType] annotation
     * Also adds support for scalars:
     * - [OffsetDateTime] -> DateTime
     * - [URI] -> Url
     * - Duration -> Duration
     * Handles the automatic generation of payload types for mutations annotated with [AutoPayloadType]
     */
    @Bean
    fun schemaGeneratorHooks() = DefaultSchemaGeneratorHooks

    /**
     * Filter factory for [OffsetDateTime] properties
     *
     * @return the generated filter factory
     */
    @Bean
    fun dateTimeFilter() =
        TypeFilterDefinitionEntry(OffsetDateTime::class.createType(nullable = true)) { name, property, parentNodeDefinition, _ ->
            DateTimeFilterDefinition(
                name, parentNodeDefinition.getNeo4jNameOfProperty(property), property.returnType.isMarkedNullable
            )
        }

    /**
     * Filter factory for [Duration] properties
     *
     * @return the generated filter factory
     */
    @Bean
    fun durationFilter() =
        TypeFilterDefinitionEntry(Duration::class.createType(nullable = true)) { name, property, parentNodeDefinition, _ ->
            DurationFilterDefinition(
                name, parentNodeDefinition.getNeo4jNameOfProperty(property), property.returnType.isMarkedNullable
            )
        }

    /**
     * Filter factory for [URI] properties
     *
     * @return the generated filter factory
     */
    @Bean
    fun urlFilter() =
        TypeFilterDefinitionEntry(URI::class.createType(nullable = true)) { name, property, parentNodeDefinition, _ ->
            StringFilterDefinition(
                name, parentNodeDefinition.getNeo4jNameOfProperty(property), property.returnType.isMarkedNullable
            )
        }

    /**
     * Filter for templatedFields on [TemplatedNode]
     *
     * @param jsonNodeMapper used to serialize [JsonNode]s
     * @return the generated filter definition
     */
    @Bean(TEMPLATED_FIELDS_FILTER_BEAN)
    fun templatedFieldsFilter(jsonNodeMapper: JsonNodeMapper) = TemplatedFieldsFilterEntryDefinition(jsonNodeMapper)

    /**
     * Filter for [GropiusUser]s with a specific permission on a specific node
     *
     * @param nodeDefinitionCollection used to get the node definition
     * @return the generated filter definition
     */
    @Bean(NODE_PERMISSION_FILTER_BEAN)
    fun nodePermissionFilter(nodeDefinitionCollection: NodeDefinitionCollection) =
        NodePermissionFilterEntryDefinition(nodeDefinitionCollection)

    /**
     * Filter for [AffectedByIssue]s which are related to a specific [Trackable]
     *
     * @param nodeDefinitionCollection used to get the node definition
     * @return the generated filter definition
     */
    @Bean(RELATED_TO_FILTER_BEAN)
    fun relatedToFilter(nodeDefinitionCollection: NodeDefinitionCollection) =
        AffectedByIssueRelatedToFilterEntryDefinition(nodeDefinitionCollection)

    /**
     * Filter for [RelationPartner]s which are part of a specific [Project]'s graph.
     */
    @Bean(PART_OF_PROJECT_FILTER)
    fun partOfProjectFilter(nodeDefinitionCollection: NodeDefinitionCollection) =
        PartOfProjectFilterEntryDefinition(nodeDefinitionCollection)

    /**
     * GraphQL enum with all subtypes of [TimelineItem]
     *
     * @param nodeDefinitionCollection used to obtain subtypes
     * @return the generated GraphQL type
     */
    @Bean
    fun timelineItemTypeType(
        nodeDefinitionCollection: NodeDefinitionCollection
    ): GraphQLEnumType {
        val builder = GraphQLEnumType.newEnum().name(TIMELINE_ITEM_TYPE_NAME).description("All timeline items types")
        for (nodeDefinition in nodeDefinitionCollection) {
            if (nodeDefinition.nodeType.isSubclassOf(TimelineItem::class)) {
                builder.value(
                    nodeDefinition.primaryLabel.replace("(?<=[a-zA-Z])[A-Z]".toRegex(), "_$0").uppercase(),
                    nodeDefinition.primaryLabel,
                    "${nodeDefinition.name} timeline item"
                )
            }
        }
        return builder.build()
    }

    /**
     * Filter for [TimelineItem]s of specific types
     */
    @Bean(TIMELINE_ITEM_TYPE_FILTER_BEAN)
    fun timelineItemTypeFilter() = TimelineItemTypeFilterEntryDefinition()

    /**
     * Provides the permission field for all nodes
     *
     * @param beanFactory used to get the [NodeDefinitionCollection]
     * @return the generated field definition
     */
    @Bean(PERMISSION_FIELD_BEAN)
    fun permissionField(beanFactory: BeanFactory): ExtensionFieldDefinition {
        val field = GraphQLFieldDefinition.newFieldDefinition().name("hasPermission")
            .description("Checks if the current user has a specific permission on this Node").argument {
                it.name("permission").description("The permission to check for").type(
                    GraphQLTypeReference(
                        ALL_PERMISSION_ENTRY_NAME
                    )
                )
            }.type(GraphQLNonNull(Scalars.GraphQLBoolean)).build()

        val nodeDefinitionCollection by lazy {
            beanFactory.getBean(NodeDefinitionCollection::class.java)
        }

        return object : ExtensionFieldDefinition(field) {
            override fun generateFetcher(
                dfe: DataFetchingEnvironment,
                arguments: Map<String, Any?>,
                node: org.neo4j.cypherdsl.core.Node,
                nodeDefinition: NodeDefinition
            ): Expression {
                return if (dfe.checkPermission) {
                    val conditionGenerator = nodeDefinitionCollection.generateAuthorizationCondition(
                        nodeDefinition, Permission(arguments["permission"] as String, dfe.gropiusAuthorizationContext)
                    )
                    val condition = conditionGenerator.generateCondition(node)
                    condition
                } else {
                    Cypher.literalTrue()
                }
            }

            override fun transformResult(result: Value): Any {
                return result.asBoolean()
            }

        }
    }

    /**
     * Provides the body field for [IssueComment]s
     *
     * @param beanFactory used to get the [NodeDefinitionCollection]
     * @return the generated field definition
     */
    @Bean(BODY_FIELD_BEAN)
    fun bodyField(beanFactory: BeanFactory): ExtensionFieldDefinition {
        val field = GraphQLFieldDefinition.newFieldDefinition().name("body")
            .description(
                """The text of the Comment.
                Supports GFM (GitHub Flavored Markdown).
                Updates cause lastEditedAt and lastEditedBy to change.
                Empty String if IssueComment is deleted.
                """
            ).type(GraphQLNonNull(Scalars.GraphQLString)).build()

        val nodeDefinitionCollection by lazy {
            beanFactory.getBean(NodeDefinitionCollection::class.java)
        }

        return object : ExtensionFieldDefinition(field) {
            override fun generateFetcher(
                dfe: DataFetchingEnvironment,
                arguments: Map<String, Any?>,
                node: org.neo4j.cypherdsl.core.Node,
                nodeDefinition: NodeDefinition
            ): Expression {
                val issueNode = nodeDefinitionCollection.getNodeDefinition<Issue>().node().named("body_issue")
                return Cypher.caseExpression()
                    .`when`(node.hasLabels(nodeDefinitionCollection.getNodeDefinition<Body>().primaryLabel))
                    .then(
                        Cypher.valueAt(
                            Cypher.listBasedOn(node.relationshipFrom(issueNode, Issue.BODY))
                                .returning(issueNode.property(Issue::bodyBody.name)), 0
                        )
                    )
                    .`when`(node.property(IssueComment::isCommentDeleted.name))
                    .then(Cypher.literalOf<String>(""))
                    .elseDefault(node.property(IssueComment::body.name))
            }

            override fun transformResult(result: Value): Any {
                return result.asString()
            }

        }
    }

    /**
     * Provides the [ExtensionFieldDefinition] for checking if the current user allows to sync their data to a target
     */
    @Bean(SYNC_SELF_ALLOWED_FIELD_BEAN)
    fun syncSelfAllowedField(): ExtensionFieldDefinition = syncAllowedField(true)

    /**
     * Provides the [ExtensionFieldDefinition] for checking if the current user allows to sync other users' data to a target
     */
    @Bean(SYNC_OTHERS_ALLOWED_FIELD_BEAN)
    fun syncOthersAllowedField(): ExtensionFieldDefinition = syncAllowedField(false)

    /**
     * Provides the [ExtensionFieldDefinition] for checking if the current user allows to sync data to a target
     *
     * @param self if the field is the self or other variant
     * @return the generated field definition
     */
    private fun syncAllowedField(self: Boolean): ExtensionFieldDefinition {
        val field = GraphQLFieldDefinition.newFieldDefinition().name("isSync${if (self) "Self" else "Others"}Allowed")
            .description("Checks if the current user allows to sync ${if (self) "their" else "other users'"} data to this target")
            .type(GraphQLNonNull(Scalars.GraphQLBoolean)).build()

        return object : ExtensionFieldDefinition(field) {
            override fun generateFetcher(
                dfe: DataFetchingEnvironment,
                arguments: Map<String, Any?>,
                node: org.neo4j.cypherdsl.core.Node,
                nodeDefinition: NodeDefinition
            ): Expression {
                val authorizationContext = dfe.authorizationContext as? GropiusAuthorizationContext
                return if (authorizationContext != null) {
                    Cypher.exists(
                        node.relationshipFrom(
                            authorizationContext.userNode,
                            if (self) GropiusUser.CAN_SYNC_SELF else GropiusUser.CAN_SYNC_OTHERS
                        )
                    )
                } else {
                    Cypher.literalFalse()
                }
            }

            override fun transformResult(result: Value): Any {
                return result.asBoolean()
            }

        }
    }

    /**
     * Provides the [KotlinDataFetcherFactoryProvider] which generates a FunctionDataFetcher which handles
     * JSON input value injecting correctly.
     *
     * @param applicationContext used to obtain Spring beans
     * @return the generated [KotlinDataFetcherFactoryProvider], used by graphql-kotlin
     */
    @Bean
    fun kotlinDataFetcherFactory(applicationContext: ApplicationContext): KotlinDataFetcherFactoryProvider =
        object : SimpleKotlinDataFetcherFactoryProvider() {
            override fun functionDataFetcherFactory(target: Any?, kClass: KClass<*>, kFunction: KFunction<*>) =
                DataFetcherFactory {
                    GropiusFunctionDataFetcher(target, kFunction, applicationContext)
                }
        }

    /**
     * [SpringGraphQLServer] which catches [Exception]s and converts them into GraphQL errors
     *
     * @param requestParser required for [SpringGraphQLServer]
     * @param contextFactory required for [SpringGraphQLServer]
     * @param requestHandler required for [SpringGraphQLServer]
     * @return the generated [SpringGraphQLServer]
     */
    @Bean
    fun springGraphQLServer(
        requestParser: SpringGraphQLRequestParser,
        contextFactory: SpringGraphQLContextFactory,
        requestHandler: GraphQLRequestHandler
    ): SpringGraphQLServer = object : SpringGraphQLServer(requestParser, contextFactory, requestHandler) {
        override suspend fun execute(request: ServerRequest): GraphQLServerResponse? {
            return try {
                super.execute(request)
            } catch (e: ResponseStatusException) {
                GraphQLResponse<Any?>(
                    errors = listOf(
                        GraphQLServerError(
                            e.reason ?: "No error message provided",
                            extensions = mapOf("status" to e.statusCode.value())
                        )
                    )
                )
            }
        }
    }

}
