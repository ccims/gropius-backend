package gropius.graphql.filter

import graphql.scalars.datetime.DateTimeScalar
import io.github.graphglue.connection.filter.definition.scalars.ComparableFilterDefinition
import java.time.OffsetDateTime

/**
 * Filter definition for a [OffsetDateTime] property
 *
 * @param name the name of the field in the filter
 * @param neo4jName the name of the property in the database
 * @param nullable if true, the scalar is nullable, otherwise it is non-nullable
 */
class DateTimeFilterDefinition(name: String, neo4jName: String, nullable: Boolean) : ComparableFilterDefinition(
    name,
    "Filter which can be used to filter for Nodes with a specific DateTime field",
    "DateTime",
    DateTimeScalar.INSTANCE,
    neo4jName,
    nullable,
    emptyList()
)