package gropius.repository.template

import gropius.model.template.InterfacePartTemplate
import gropius.repository.GropiusRepository
import org.springframework.stereotype.Repository

/**
 * Repository for [InterfacePartTemplate]
 */
@Repository
interface InterfacePartTemplateRepository : GropiusRepository<InterfacePartTemplate, String>
