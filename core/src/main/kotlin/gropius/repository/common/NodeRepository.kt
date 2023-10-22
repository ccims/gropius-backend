package gropius.repository.common

import gropius.repository.GropiusRepository
import io.github.graphglue.model.Node
import org.springframework.stereotype.Repository

/**
 * Repository for [Node]
 */
@Repository
interface NodeRepository : GropiusRepository<Node, String>