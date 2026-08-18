package gropius.repository.template

import gropius.model.template.ComponentVersionTemplate
import gropius.repository.GropiusRepository
import org.springframework.stereotype.Repository

/**
 * Repository for [ComponentVersionTemplate]
 */
@Repository
interface ComponentVersionTemplateRepository : GropiusRepository<ComponentVersionTemplate, String>
