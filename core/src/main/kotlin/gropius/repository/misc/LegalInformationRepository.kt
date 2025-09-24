package gropius.repository.misc

import gropius.model.misc.LegalInformation
import gropius.repository.GropiusRepository
import org.springframework.stereotype.Repository

/**
 * Repository for [LegalInformation]
 */
@Repository
interface LegalInformationRepository : GropiusRepository<LegalInformation, String>