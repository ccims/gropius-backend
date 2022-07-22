package gropius.repository.template

import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository
import org.springframework.stereotype.Repository
import gropius.model.template.IssueTemplate

/**
 * Repository for [IssueTemplate]
 */
@Repository
interface IssueTemplateRepository : ReactiveNeo4jRepository<IssueTemplate, String>