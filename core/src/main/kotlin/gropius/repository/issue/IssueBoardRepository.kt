package gropius.repository.issue

import gropius.model.issue.IssueBoard
import gropius.repository.GropiusRepository
import org.springframework.stereotype.Repository


    /**
     * Repository for [IssueBoard]
     */
    @Repository
    interface IssueBoardRepository : GropiusRepository<IssueBoard, String>
