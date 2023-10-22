package gropius.schema.mutation

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.expediagroup.graphql.server.operations.Mutation
import graphql.schema.DataFetchingEnvironment
import gropius.authorization.gropiusAuthorizationContext
import gropius.dto.input.common.DeleteNodeInput
import gropius.dto.input.user.UpdateGropiusUserInput
import gropius.dto.input.user.permission.*
import gropius.dto.payload.DeleteNodePayload
import gropius.graphql.AutoPayloadType
import gropius.model.user.GropiusUser
import gropius.model.user.permission.ComponentPermission
import gropius.model.user.permission.GlobalPermission
import gropius.model.user.permission.IMSPermission
import gropius.model.user.permission.ProjectPermission
import gropius.service.user.GropiusUserService
import gropius.service.user.permission.ComponentPermissionService
import gropius.service.user.permission.GlobalPermissionService
import gropius.service.user.permission.IMSPermissionService
import gropius.service.user.permission.ProjectPermissionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Contains all User-related mutations
 *
 * @param componentPermissionService used for ComponentPermission-related mutations
 * @param projectPermissionService used for ProjectPermission-related mutations
 * @param imsPermissionService used for all IMSPermission-related mutations
 * @param globalPermissionService used for all GlobalPermission-related mutations
 */
@Component
class UserMutations(
    private val componentPermissionService: ComponentPermissionService,
    private val projectPermissionService: ProjectPermissionService,
    private val imsPermissionService: IMSPermissionService,
    private val globalPermissionService: GlobalPermissionService
) : Mutation {

    @GraphQLDescription(
        """Updates a GropiusUser. Only the same user and admin users can update a GropiusUser.
        Only admin users can update isAdmin
        """
    )
    @AutoPayloadType("The updated GropiusUser")
    suspend fun updateGropiusUser(
        @GraphQLDescription("Defines which GropiusUser to update and how to update it")
        input: UpdateGropiusUserInput,
        dfe: DataFetchingEnvironment,
        @GraphQLIgnore
        @Autowired
        gropiusUserService: GropiusUserService
    ): GropiusUser {
        return gropiusUserService.updateGropiusUser(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Creates a new ComponentPermission, requires ADMIN on all Components which should be added to the created
        permission.
        """
    )
    @AutoPayloadType("The created ComponentPermission")
    suspend fun createComponentPermission(
        @GraphQLDescription("Defines the created ComponentPermission")
        input: CreateComponentPermissionInput, dfe: DataFetchingEnvironment
    ): ComponentPermission {
        return componentPermissionService.createComponentPermission(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Updates a ComponentPermission, requires ADMIN on all Components the permission currently affects.
        Ensures that after the update, all affected Components still have a permission which grants ADMIN to at least
        one user.
        """
    )
    @AutoPayloadType("The updated ComponentPermission")
    suspend fun updateComponentPermission(
        @GraphQLDescription("Defines which ComponentPermission to update and how to update it")
        input: UpdateComponentPermissionInput, dfe: DataFetchingEnvironment
    ): ComponentPermission {
        return componentPermissionService.updateNodePermission(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Deletes a ComponentPermission, requires ADMIN on all Components the permission currently affects.
        Ensures that after the permission is deleted, all previously affected Components still have a permission which 
        grants ADMIN to at least one user.
        """
    )
    suspend fun deleteComponentPermission(
        @GraphQLDescription("Defines which ComponentPermission to delete")
        input: DeleteNodeInput, dfe: DataFetchingEnvironment
    ): DeleteNodePayload {
        componentPermissionService.deleteNodePermission(dfe.gropiusAuthorizationContext, input)
        return DeleteNodePayload(input.id)
    }

    @GraphQLDescription(
        """Creates a new ProjectPermission, requires ADMIN on all Projects which should be added to the created
        permission.
        """
    )
    @AutoPayloadType("The created ProjectPermission")
    suspend fun createProjectPermission(
        @GraphQLDescription("Defines the created ProjectPermission")
        input: CreateProjectPermissionInput, dfe: DataFetchingEnvironment
    ): ProjectPermission {
        return projectPermissionService.createProjectPermission(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Updates a ProjectPermission, requires ADMIN on all Projects the permission currently affects.
        Ensures that after the update, all affected Projects still have a permission which grants ADMIN to at least
        one user.
        """
    )
    @AutoPayloadType("The updated ProjectPermission")
    suspend fun updateProjectPermission(
        @GraphQLDescription("Defines which ProjectPermission to update and how to update it")
        input: UpdateProjectPermissionInput, dfe: DataFetchingEnvironment
    ): ProjectPermission {
        return projectPermissionService.updateNodePermission(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Deletes a ProjectPermission, requires ADMIN on all Projects the permission currently affects.
        Ensures that after the permission is deleted, all previously affected Projects still have a permission which 
        grants ADMIN to at least one user.
        """
    )
    suspend fun deleteProjectPermission(
        @GraphQLDescription("Defines which ProjectPermission to delete")
        input: DeleteNodeInput, dfe: DataFetchingEnvironment
    ): DeleteNodePayload {
        projectPermissionService.deleteNodePermission(dfe.gropiusAuthorizationContext, input)
        return DeleteNodePayload(input.id)
    }

    @GraphQLDescription(
        """Creates a new IMSPermission, requires ADMIN on all IMSs which should be added to the created
        permission.
        """
    )
    @AutoPayloadType("The created IMSPermission")
    suspend fun createIMSPermission(
        @GraphQLDescription("Defines the created IMSPermission")
        input: CreateIMSPermissionInput, dfe: DataFetchingEnvironment
    ): IMSPermission {
        return imsPermissionService.createIMSPermission(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Updates a IMSPermission, requires ADMIN on all IMSs the permission currently affects.
        Ensures that after the update, all affected IMSs still have a permission which grants ADMIN to at least
        one user.
        """
    )
    @AutoPayloadType("The updated IMSPermission")
    suspend fun updateIMSPermission(
        @GraphQLDescription("Defines which IMSPermission to update and how to update it")
        input: UpdateIMSPermissionInput, dfe: DataFetchingEnvironment
    ): IMSPermission {
        return imsPermissionService.updateNodePermission(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription(
        """Deletes a IMSPermission, requires ADMIN on all IMSs the permission currently affects.
        Ensures that after the permission is deleted, all previously affected IMSs still have a permission which 
        grants ADMIN to at least one user.
        """
    )
    suspend fun deleteIMSPermission(
        @GraphQLDescription("Defines which IMSPermission to delete")
        input: DeleteNodeInput, dfe: DataFetchingEnvironment
    ): DeleteNodePayload {
        imsPermissionService.deleteNodePermission(dfe.gropiusAuthorizationContext, input)
        return DeleteNodePayload(input.id)
    }

    @GraphQLDescription("Creates a new GlobalPermission, requires that the user is an admin")
    @AutoPayloadType("The created GlobalPermission")
    suspend fun createGlobalPermission(
        @GraphQLDescription("Defines the created GlobalPermission")
        input: CreateGlobalPermissionInput, dfe: DataFetchingEnvironment
    ): GlobalPermission {
        return globalPermissionService.createGlobalPermission(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription("Updates a GlobalPermission, requires that the user is an admin")
    @AutoPayloadType("The updated GlobalPermission")
    suspend fun updateGlobalPermission(
        @GraphQLDescription("Defines which GlobalPermission to update and how to update it")
        input: UpdateGlobalPermissionInput, dfe: DataFetchingEnvironment
    ): GlobalPermission {
        return globalPermissionService.updateGlobalPermission(dfe.gropiusAuthorizationContext, input)
    }

    @GraphQLDescription("Deletes a GlobalPermission, requires that the user is an admin")
    suspend fun deleteGlobalPermission(
        @GraphQLDescription("Defines which GlobalPermission to delete")
        input: DeleteNodeInput, dfe: DataFetchingEnvironment
    ): DeleteNodePayload {
        globalPermissionService.deleteGlobalPermission(dfe.gropiusAuthorizationContext, input)
        return DeleteNodePayload(input.id)
    }

}