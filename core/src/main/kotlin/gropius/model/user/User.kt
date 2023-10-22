package gropius.model.user

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import gropius.model.common.AuditedNode
import gropius.model.common.ExtensibleNode
import gropius.model.issue.Issue
import gropius.model.issue.timeline.Assignment
import gropius.model.user.permission.NodePermission
import gropius.service.user.AvatarGenerationService
import io.github.graphglue.model.*
import org.springframework.beans.factory.annotation.Autowired
import java.net.URI

@DomainNode(searchQueryName = "searchUsers")
@GraphQLDescription(
    """A user known to the Gropius System.
    This might be a user that registered directly, or a user the systems know via a sync adapter.
    A user can create AuditedNodes, participate in Issues and be assigned to Issues.
    READ is always granted.
    """
)
@Authorization(NodePermission.READ, allowAll = true)
abstract class User(
    @property:GraphQLDescription("The name which should be displayed for the user.")
    @FilterProperty
    @OrderProperty
    @SearchProperty
    var displayName: String,
    @property:GraphQLDescription("The email address of the user.")
    @FilterProperty
    @OrderProperty
    var email: String?,
    @GraphQLIgnore
    var avatar: URI?,
    @property:GraphQLIgnore
    @FilterProperty
    @OrderProperty
    @SearchProperty
    var username: String?,
) : ExtensibleNode() {

    @GraphQLDescription(
        """The identifier of the user.
        This is only unique for GropiusUsers, for IMSUsers, no constrains v  are guaranteed.
        """
    )
    abstract fun username(): String?

    @GraphQLDescription("The avatar of the user.")
    fun avatar(
        @GraphQLIgnore
        @Autowired
        avatarGenerationService: AvatarGenerationService
    ): URI {
        return avatar ?: URI(avatarGenerationService.generateAvatar(rawId ?: ""))
    }

    @NodeRelationship(AuditedNode.CREATED_BY, Direction.INCOMING)
    @GraphQLDescription("AuditedNodes the user created.")
    @FilterProperty
    val createdNodes by NodeSetProperty<AuditedNode>()

    @NodeRelationship(Issue.PARTICIPANT, Direction.INCOMING)
    @GraphQLDescription("Issues the user participated in.")
    @FilterProperty
    val participatedIssues by NodeSetProperty<Issue>()

    @NodeRelationship(Assignment.USER, Direction.INCOMING)
    @GraphQLDescription("Assignments the user is part of, this includes assignments which aren't active.")
    @FilterProperty
    val assignments by NodeSetProperty<Assignment>()
}