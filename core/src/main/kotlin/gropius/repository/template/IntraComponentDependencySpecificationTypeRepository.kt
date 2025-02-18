package gropius.repository.template

import gropius.model.template.IntraComponentDependencySpecificationType
import gropius.repository.GropiusRepository
import org.springframework.stereotype.Repository

/**
 * Repository for [IntraComponentDependencySpecificationType]
 */
@Repository
interface IntraComponentDependencySpecificationTypeRepository : GropiusRepository<IntraComponentDependencySpecificationType, String>