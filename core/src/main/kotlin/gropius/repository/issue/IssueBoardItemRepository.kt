package gropius.repository.issue


import gropius.model.issue.IssueBoardItem
import gropius.repository.GropiusRepository
import org.springframework.stereotype.Repository


/**
 * Repository for [IssueBoardItem]
 */
@Repository
interface IssueBoardItemRepository : GropiusRepository<IssueBoardItem, String>