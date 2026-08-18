package gropius.repository.template

import gropius.model.template.InterfaceSpecificationVersionTemplate
import gropius.repository.GropiusRepository
import org.springframework.stereotype.Repository

/**
 * Repository for [InterfaceSpecificationVersionTemplate]
 */
@Repository
interface InterfaceSpecificationVersionTemplateRepository : GropiusRepository<InterfaceSpecificationVersionTemplate, String>
