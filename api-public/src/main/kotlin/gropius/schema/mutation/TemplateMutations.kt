package gropius.schema.mutation

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.expediagroup.graphql.server.operations.Mutation
import graphql.schema.DataFetchingEnvironment
import gropius.authorization.gropiusAuthorizationContext
import gropius.dto.input.template.*
import gropius.graphql.AutoPayloadType
import gropius.model.template.*
import gropius.service.template.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * Contains all template-related mutations
 */
@Component
@Transactional(propagation = Propagation.REQUIRES_NEW)
class TemplateMutations : Mutation {

    @GraphQLDescription("Updates the deprecation state of the template, requires CAN_CREATE_TEMPLATES")
    @AutoPayloadType("The updated Template")
    suspend fun updateTemplateDeprecationStatus(
        @GraphQLDescription("Defines the new deprecation status and the Template to update")
        input: UpdateTemplateDeprecationStatusInput,
        dfe: DataFetchingEnvironment,
        @GraphQLIgnore
        @Autowired
        templateService: TemplateService
    ): Template<*, *> {
        return templateService.updateTemplateDeprecationStatus(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription("Creates a new ArtefactTemplate, requires CAN_CREATE_TEMPLATES")
    @AutoPayloadType("The created ArtefactTemplate")
    suspend fun createArtefactTemplate(
        @GraphQLDescription("Defines the created ArtefactTemplate")
        input: CreateArtefactTemplateInput,
        dfe: DataFetchingEnvironment,
        @GraphQLIgnore
        @Autowired
        templateService: ArtefactTemplateService
    ): ArtefactTemplate {
        return templateService.createArtefactTemplate(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription("Creates a new ComponentTemplate, requires CAN_CREATE_TEMPLATES")
    @AutoPayloadType("The created ComponentTemplate")
    suspend fun createComponentTemplate(
        @GraphQLDescription("Defines the created ComponentTemplate")
        input: CreateComponentTemplateInput,
        dfe: DataFetchingEnvironment,
        @GraphQLIgnore
        @Autowired
        templateService: ComponentTemplateService
    ): ComponentTemplate {
        return templateService.createComponentTemplate(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription("Creates a new InterfaceSpecificationTemplate, requires CAN_CREATE_TEMPLATES")
    @AutoPayloadType("The created InterfaceSpecificationTemplate")
    suspend fun createInterfaceSpecificationTemplate(
        @GraphQLDescription("Defines the created InterfaceSpecificationTemplate")
        input: CreateInterfaceSpecificationTemplateInput,
        dfe: DataFetchingEnvironment,
        @GraphQLIgnore
        @Autowired
        templateService: InterfaceSpecificationTemplateService
    ): InterfaceSpecificationTemplate {
        return templateService.createInterfaceSpecificationTemplate(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription("Creates a new IssueTemplate, requires CAN_CREATE_TEMPLATES")
    @AutoPayloadType("The created IssueTemplate")
    suspend fun createIssueTemplate(
        @GraphQLDescription("Defines the created IssueTemplate")
        input: CreateIssueTemplateInput,
        dfe: DataFetchingEnvironment,
        @GraphQLIgnore
        @Autowired
        templateService: IssueTemplateService
    ): IssueTemplate {
        return templateService.createIssueTemplate(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription("Creates a new RelationTemplate, requires CAN_CREATE_TEMPLATES")
    @AutoPayloadType("The created RelationTemplate")
    suspend fun createRelationTemplate(
        @GraphQLDescription("Defines the created RelationTemplate")
        input: CreateRelationTemplateInput,
        dfe: DataFetchingEnvironment,
        @GraphQLIgnore
        @Autowired
        templateService: RelationTemplateService
    ): RelationTemplate {
        return templateService.createRelationTemplate(dfe.gropiusAuthorizationContext, input)
    }

}