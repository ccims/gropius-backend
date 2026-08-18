package gropius.repository.template

import gropius.model.template.IMSProjectTemplate
import gropius.repository.GropiusRepository
import org.springframework.stereotype.Repository

/**
 * Repository for [IMSProjectTemplate]
 */
@Repository
interface IMSProjectTemplateRepository : GropiusRepository<IMSProjectTemplate, String>
