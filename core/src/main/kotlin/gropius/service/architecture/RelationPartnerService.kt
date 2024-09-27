package gropius.service.architecture

import gropius.model.architecture.RelationPartner
import gropius.repository.GropiusRepository
import gropius.service.common.NodeService

/**
 * Base class for services for subclasses of [RelationPartner]
 *
 * @param repository the associated repository used for CRUD functionality
 * @param T the type of Node this service is used for
 * @param R Repository type associated with [T]
 */
abstract class RelationPartnerService<T : RelationPartner, R : GropiusRepository<T, String>>(
    repository: R
) : NodeService<T, R>(repository)