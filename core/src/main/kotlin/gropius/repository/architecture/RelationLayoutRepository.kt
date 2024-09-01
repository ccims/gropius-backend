package gropius.repository.architecture

import gropius.model.architecture.layout.RelationLayout
import gropius.repository.GropiusRepository
import org.springframework.stereotype.Repository

/**
 * Repository for [RelationLayout]
 */
@Repository
interface RelationLayoutRepository : GropiusRepository<RelationLayout, String>