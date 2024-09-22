package gropius.repository.architecture

import gropius.model.architecture.layout.View
import gropius.repository.GropiusRepository
import org.springframework.stereotype.Repository

/**
 * Repository for [View]
 */
@Repository
interface ViewRepository : GropiusRepository<View, String>