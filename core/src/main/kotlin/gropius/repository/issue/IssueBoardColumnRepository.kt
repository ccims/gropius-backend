package gropius.repository.issue

import gropius.model.issue.IssueBoardColumn
import gropius.repository.GropiusRepository
import org.springframework.stereotype.Repository


/**
 * Repository for [IssueBoardColumn]
 */
@Repository
interface IssueBoardColumnRepository : GropiusRepository<IssueBoardColumn, String>