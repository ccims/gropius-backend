package gropius.schema.mutation

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import graphql.schema.DataFetchingEnvironment
import gropius.authorization.gropiusAuthorizationContext
import gropius.dto.input.common.DeleteNodeInput
import gropius.dto.input.misc.CreateLegalInformationInput
import gropius.dto.input.misc.UpdateLegalInformationInput
import gropius.dto.payload.DeleteNodePayload
import gropius.graphql.AutoPayloadType
import gropius.model.misc.LegalInformation
import gropius.service.misc.LegalInformationService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * Contains all misc mutations
 */
@Component
@Transactional(propagation = Propagation.REQUIRES_NEW)
class MiscMutations(
    private val legalInformationService: LegalInformationService
) : Mutation {

    @GraphQLDescription("Creates a new LegalInformation, requires admin")
    @AutoPayloadType("The created LegalInformation")
    suspend fun createLegalInformation(
        @GraphQLDescription("Defines the created LegalInformation")
        input: CreateLegalInformationInput, dfe: DataFetchingEnvironment
    ): LegalInformation {
        return legalInformationService.createLegalInformation(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription("Updates the specified LegalInformation, requires admin")
    @AutoPayloadType("The updated LegalInformation")
    suspend fun updateLegalInformation(
        @GraphQLDescription("Defines which LegalInformation to update and how to update it")
        input: UpdateLegalInformationInput, dfe: DataFetchingEnvironment
    ): LegalInformation {
        return legalInformationService.updateLegalInformation(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription("Deletes the specified LegalInformation, requires admin")
    suspend fun deleteLegalInformation(
        @GraphQLDescription("Defines which LegalInformation to delete")
        input: DeleteNodeInput, dfe: DataFetchingEnvironment
    ): DeleteNodePayload {
        legalInformationService.deleteLegalInformation(dfe.gropiusAuthorizationContext, input)
        return DeleteNodePayload(input.id)
    }

}