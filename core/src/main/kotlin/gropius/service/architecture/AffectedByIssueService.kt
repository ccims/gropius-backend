package gropius.service.architecture

import gropius.model.architecture.AffectedByIssue
import gropius.repository.GropiusRepository
import gropius.service.common.NamedNodeService

/**
 * Base class for services for subclasses of [AffectedByIssue]
 *
 * @param repository the associated repository used for CRUD functionality
 * @param T the type of Node this service is used for
 * @param R Repository type associated with [T]
 */
abstract class AffectedByIssueService<T : AffectedByIssue, R : GropiusRepository<T, String>>(repository: R) :
    NamedNodeService<T, R>(repository)