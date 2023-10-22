package gropius.model.user

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import gropius.model.architecture.IMS
import gropius.model.template.BaseTemplate
import gropius.model.template.IMSUserTemplate
import gropius.model.template.TemplatedNode
import io.github.graphglue.model.Direction
import io.github.graphglue.model.DomainNode
import io.github.graphglue.model.FilterProperty
import io.github.graphglue.model.NodeRelationship
import org.springframework.data.neo4j.core.schema.CompositeProperty
import java.net.URI

@DomainNode
@GraphQLDescription(
    """A user an IMS.
    This user might be linked to a GropiusUser.
    Note that this link can change at any time.
    The username might not be unique.
    It is possible that this user never heard of Gropius, and is only known to the system due to sync adapters.
    """
)
class IMSUser(
    displayName: String,
    email: String?,
    avatar: URI?,
    username: String?,
    @property:GraphQLIgnore
    @CompositeProperty
    override val templatedFields: MutableMap<String, String>
) : User(displayName, email, avatar, username), TemplatedNode {

    companion object {
        const val GROPIUS_USER = "GROPIUS_USER"
    }

    @GraphQLDescription("The username of the IMSUser. Synced from the IMS this user is part of. Might not be unique.")
    override fun username(): String? = username

    @NodeRelationship(BaseTemplate.USED_IN, Direction.INCOMING)
    @GraphQLDescription("The Template of this IMSUser")
    @FilterProperty
    override val template by NodeProperty<IMSUserTemplate>()

    @NodeRelationship(GROPIUS_USER, Direction.OUTGOING)
    @GraphQLDescription("The GropiusUser this IMSUser is linked to. An IMSUser might be linked to no GropiusUser.")
    @FilterProperty
    val gropiusUser by NodeProperty<GropiusUser?>()

    @NodeRelationship(IMS.USER, Direction.INCOMING)
    @GraphQLDescription("The IMS this user is part of.")
    @FilterProperty
    val ims by NodeProperty<IMS>()
}