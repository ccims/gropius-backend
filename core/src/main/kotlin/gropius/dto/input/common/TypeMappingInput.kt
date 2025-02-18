package gropius.dto.input.common

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.scalars.ID

@GraphQLDescription("Input to map an old type to a new type")
class TypeMappingInput(
    @GraphQLDescription("The old type, null representing no type")
    val oldType: ID,
    @GraphQLDescription("The new type, null representing no type")
    val newType: ID?
) : Input()

/**
 * Checks that the mapping is unique, meaning no two entries exist for the same `oldType`
 *
 * @throws IllegalStateException if two entries with the same `oldType` exist
 */
fun List<TypeMappingInput>.ensureUniqueMapping() {
    val duplicates = this.groupingBy { it.oldType }.eachCount().filter { it.value > 1 }.keys
    if (duplicates.isNotEmpty()) {
        throw IllegalArgumentException("Duplicate oldTypes found: $duplicates")
    }
}