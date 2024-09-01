package gropius.repository.architecture

import gropius.model.architecture.layout.RelationPartnerLayout
import gropius.repository.GropiusRepository
import org.springframework.stereotype.Repository

/**
 * Repository for [RelationPartnerLayout]
 */
@Repository
interface RelationPartnerLayoutRepository : GropiusRepository<RelationPartnerLayout, String>