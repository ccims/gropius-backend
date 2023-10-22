package gropius.graphql.filter

import gropius.graphql.DurationScalar
import io.github.graphglue.connection.filter.definition.scalars.ComparableFilterDefinition
import java.time.Duration

/**
 * Filter definition for a [Duration] property
 *
 * @param name the name of the field in the filter
 * @param neo4jName the name of the property in the database
 * @param nullable if true, the scalar is nullable, otherwise it is non-nullable
 */
class DurationFilterDefinition(name: String, neo4jName: String, nullable: Boolean) : ComparableFilterDefinition(
    name,
    "Filter which can be used to filter for Nodes with a specific Duration field",
    "DurationFilterInput",
    DurationScalar,
    neo4jName,
    nullable,
    emptyList()
)