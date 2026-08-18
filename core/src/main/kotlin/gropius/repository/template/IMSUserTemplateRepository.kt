package gropius.repository.template

import gropius.model.template.IMSUserTemplate
import gropius.repository.GropiusRepository
import org.springframework.stereotype.Repository

/**
 * Repository for [IMSUserTemplate]
 */
@Repository
interface IMSUserTemplateRepository : GropiusRepository<IMSUserTemplate, String>
