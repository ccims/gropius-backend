package gropius.repository.template

import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository
import org.springframework.stereotype.Repository
import gropius.model.template.InterfaceSpecificationTemplate

/**
 * Repository for [InterfaceSpecificationTemplate]
 */
@Repository
interface InterfaceSpecificationTemplateRepository : ReactiveNeo4jRepository<InterfaceSpecificationTemplate, String>