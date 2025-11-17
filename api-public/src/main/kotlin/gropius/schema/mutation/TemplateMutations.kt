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

    @GraphQLDescription("Updates an ArtefactTemplate, requires CAN_CREATE_TEMPLATES")
    @AutoPayloadType("The updated ArtefactTemplate")
    suspend fun updateArtefactTemplate(
        @GraphQLDescription("Defines which ArtefactTemplate to update and how to update it")
        input: UpdateArtefactTemplateInput,
        dfe: DataFetchingEnvironment,
        @GraphQLIgnore
        @Autowired
        templateService: ArtefactTemplateService
    ): ArtefactTemplate {
        return templateService.updateArtefactTemplate(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription("Updates a ComponentTemplate, requires CAN_CREATE_TEMPLATES")
    @AutoPayloadType("The updated ComponentTemplate")
    suspend fun updateComponentTemplate(
        @GraphQLDescription("Defines which ComponentTemplate to update and how to update it")
        input: UpdateComponentTemplateInput,
        dfe: DataFetchingEnvironment,
        @GraphQLIgnore
        @Autowired
        templateService: ComponentTemplateService
    ): ComponentTemplate {
        return templateService.updateComponentTemplate(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription("Updates an InterfaceSpecificationTemplate, requires CAN_CREATE_TEMPLATES")
    @AutoPayloadType("The updated InterfaceSpecificationTemplate")
    suspend fun updateInterfaceSpecificationTemplate(
        @GraphQLDescription("Defines which InterfaceSpecificationTemplate to update and how to update it")
        input: UpdateInterfaceSpecificationTemplateInput,
        dfe: DataFetchingEnvironment,
        @GraphQLIgnore
        @Autowired
        templateService: InterfaceSpecificationTemplateService
    ): InterfaceSpecificationTemplate {
        return templateService.updateInterfaceSpecificationTemplate(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription("Updates an IssueTemplate, requires CAN_CREATE_TEMPLATES")
    @AutoPayloadType("The updated IssueTemplate")
    suspend fun updateIssueTemplate(
        @GraphQLDescription("Defines which IssueTemplate to update and how to update it")
        input: UpdateIssueTemplateInput,
        dfe: DataFetchingEnvironment,
        @GraphQLIgnore
        @Autowired
        templateService: IssueTemplateService
    ): IssueTemplate {
        return templateService.updateIssueTemplate(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription("Updates a RelationTemplate, requires CAN_CREATE_TEMPLATES")
    @AutoPayloadType("The updated RelationTemplate")
    suspend fun updateRelationTemplate(
        @GraphQLDescription("Defines which RelationTemplate to update and how to update it")
        input: UpdateRelationTemplateInput,
        dfe: DataFetchingEnvironment,
        @GraphQLIgnore
        @Autowired
        templateService: RelationTemplateService
    ): RelationTemplate {
        return templateService.updateRelationTemplate(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription("Updates an IMSTemplate, requires CAN_CREATE_TEMPLATES")
    @AutoPayloadType("The updated IMSTemplate")
    suspend fun updateIMSTemplate(
        @GraphQLDescription("Defines which IMSTemplate to update and how to update it")
        input: UpdateIMSTemplateInput,
        dfe: DataFetchingEnvironment,
        @GraphQLIgnore
        @Autowired
        templateService: IMSTemplateService
    ): IMSTemplate {
        return templateService.updateIMSTemplate(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription("Updates a ComponentVersionTemplate, requires CAN_CREATE_TEMPLATES")
    @AutoPayloadType("The updated ComponentVersionTemplate")
    suspend fun updateComponentVersionTemplate(
        @GraphQLDescription("Defines which ComponentVersionTemplate to update and how to update it")
        input: UpdateComponentVersionTemplateInput,
        dfe: DataFetchingEnvironment,
        @GraphQLIgnore
        @Autowired
        templateService: ComponentVersionTemplateService
    ): ComponentVersionTemplate {
        return templateService.updateComponentVersionTemplate(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription("Updates an InterfaceSpecificationVersionTemplate, requires CAN_CREATE_TEMPLATES")
    @AutoPayloadType("The updated InterfaceSpecificationVersionTemplate")
    suspend fun updateInterfaceSpecificationVersionTemplate(
        @GraphQLDescription("Defines which InterfaceSpecificationVersionTemplate to update and how to update it")
        input: UpdateInterfaceSpecificationVersionTemplateInput,
        dfe: DataFetchingEnvironment,
        @GraphQLIgnore
        @Autowired
        templateService: InterfaceSpecificationVersionTemplateService
    ): InterfaceSpecificationVersionTemplate {
        return templateService.updateInterfaceSpecificationVersionTemplate(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription("Updates an InterfacePartTemplate, requires CAN_CREATE_TEMPLATES")
    @AutoPayloadType("The updated InterfacePartTemplate")
    suspend fun updateInterfacePartTemplate(
        @GraphQLDescription("Defines which InterfacePartTemplate to update and how to update it")
        input: UpdateInterfacePartTemplateInput,
        dfe: DataFetchingEnvironment,
        @GraphQLIgnore
        @Autowired
        templateService: InterfacePartTemplateService
    ): InterfacePartTemplate {
        return templateService.updateInterfacePartTemplate(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription("Updates an IMSProjectTemplate, requires CAN_CREATE_TEMPLATES")
    @AutoPayloadType("The updated IMSProjectTemplate")
    suspend fun updateIMSProjectTemplate(
        @GraphQLDescription("Defines which IMSProjectTemplate to update and how to update it")
        input: UpdateIMSProjectTemplateInput,
        dfe: DataFetchingEnvironment,
        @GraphQLIgnore
        @Autowired
        templateService: IMSProjectTemplateService
    ): IMSProjectTemplate {
        return templateService.updateIMSProjectTemplate(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription("Updates an IMSIssueTemplate, requires CAN_CREATE_TEMPLATES")
    @AutoPayloadType("The updated IMSIssueTemplate")
    suspend fun updateIMSIssueTemplate(
        @GraphQLDescription("Defines which IMSIssueTemplate to update and how to update it")
        input: UpdateIMSIssueTemplateInput,
        dfe: DataFetchingEnvironment,
        @GraphQLIgnore
        @Autowired
        templateService: IMSIssueTemplateService
    ): IMSIssueTemplate {
        return templateService.updateIMSIssueTemplate(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription("Updates an IMSUserTemplate, requires CAN_CREATE_TEMPLATES")
    @AutoPayloadType("The updated IMSUserTemplate")
    suspend fun updateIMSUserTemplate(
        @GraphQLDescription("Defines which IMSUserTemplate to update and how to update it")
        input: UpdateIMSUserTemplateInput,
        dfe: DataFetchingEnvironment,
        @GraphQLIgnore
        @Autowired
        templateService: IMSUserTemplateService
    ): IMSUserTemplate {
        return templateService.updateIMSUserTemplate(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription("Updates an IssueType, requires CAN_CREATE_TEMPLATES")
    @AutoPayloadType("The updated IssueType")
    suspend fun updateIssueType(
        @GraphQLDescription("Defines which IssueType to update and how to update it")
        input: UpdateIssueTypeInput,
        dfe: DataFetchingEnvironment,
        @GraphQLIgnore
        @Autowired
        issueTypeService: IssueTypeService
    ): IssueType {
        return issueTypeService.updateIssueType(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription("Updates an IssueState, requires CAN_CREATE_TEMPLATES")
    @AutoPayloadType("The updated IssueState")
    suspend fun updateIssueState(
        @GraphQLDescription("Defines which IssueState to update and how to update it")
        input: UpdateIssueStateInput,
        dfe: DataFetchingEnvironment,
        @GraphQLIgnore
        @Autowired
        issueStateService: IssueStateService
    ): IssueState {
        return issueStateService.updateIssueState(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription("Updates an IssuePriority, requires CAN_CREATE_TEMPLATES")
    @AutoPayloadType("The updated IssuePriority")
    suspend fun updateIssuePriority(
        @GraphQLDescription("Defines which IssuePriority to update and how to update it")
        input: UpdateIssuePriorityInput,
        dfe: DataFetchingEnvironment,
        @GraphQLIgnore
        @Autowired
        issuePriorityService: IssuePriorityService
    ): IssuePriority {
        return issuePriorityService.updateIssuePriority(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription("Updates an AssignmentType, requires CAN_CREATE_TEMPLATES")
    @AutoPayloadType("The updated AssignmentType")
    suspend fun updateAssignmentType(
        @GraphQLDescription("Defines which AssignmentType to update and how to update it")
        input: UpdateAssignmentTypeInput,
        dfe: DataFetchingEnvironment,
        @GraphQLIgnore
        @Autowired
        assignmentTypeService: AssignmentTypeService
    ): AssignmentType {
        return assignmentTypeService.updateAssignmentType(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription("Updates an IssueRelationType, requires CAN_CREATE_TEMPLATES")
    @AutoPayloadType("The updated IssueRelationType")
    suspend fun updateIssueRelationType(
        @GraphQLDescription("Defines which IssueRelationType to update and how to update it")
        input: UpdateIssueRelationTypeInput,
        dfe: DataFetchingEnvironment,
        @GraphQLIgnore
        @Autowired
        issueRelationTypeService: IssueRelationTypeService
    ): IssueRelationType {
        return issueRelationTypeService.updateIssueRelationType(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription("Updates an IntraComponentDependencySpecificationType, requires CAN_CREATE_TEMPLATES")
    @AutoPayloadType("The updated IntraComponentDependencySpecificationType")
    suspend fun updateIntraComponentDependencySpecificationType(
        @GraphQLDescription("Defines which IntraComponentDependencySpecificationType to update and how to update it")
        input: UpdateIntraComponentDependencySpecificationTypeInput,
        dfe: DataFetchingEnvironment,
        @GraphQLIgnore
        @Autowired
        intraComponentDependencySpecificationTypeService: IntraComponentDependencySpecificationTypeService
    ): IntraComponentDependencySpecificationType {
        return intraComponentDependencySpecificationTypeService.updateIntraComponentDependencySpecificationType(dfe.gropiusAuthorizationContext, input)
    }

}