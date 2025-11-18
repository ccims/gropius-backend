package gropius.repository.template

import gropius.model.template.IMSIssueTemplate
import gropius.repository.GropiusRepository
import org.springframework.stereotype.Repository

/**
 * Repository for [IMSIssueTemplate]
 */
@Repository
interface IMSIssueTemplateRepository : GropiusRepository<IMSIssueTemplate, String>
