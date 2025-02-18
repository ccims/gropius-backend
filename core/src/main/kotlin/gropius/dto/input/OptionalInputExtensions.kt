package gropius.dto.input

import com.expediagroup.graphql.generator.execution.OptionalInput
import com.expediagroup.graphql.generator.scalars.ID
import gropius.dto.input.common.TypeMappingInput
import gropius.repository.GropiusRepository
import gropius.repository.findAllById
import io.github.graphglue.model.Node
import kotlin.collections.get
import kotlin.reflect.KProperty0

/**
 * Executes [block] with the value if `this is OptionalInput.Defined`
 *
 * @param block executed if input is defined
 */
@Suppress("UNCHECKED_CAST")
inline fun <T> OptionalInput<T>.ifPresent(block: (T) -> Unit) {
    if (this is OptionalInput.Defined) {
        block(this.value as T)
    }
}

/**
 * `true` if the input is present
 */
val OptionalInput<*>.isPresent: Boolean get() = this is OptionalInput.Defined

/**
 * If present, returns its value, otherwise [value]
 *
 * @param value result in case not present
 * @return its value if present, otherwise [value]
 */
fun <T> OptionalInput<T>.orElse(value: T): T {
    ifPresent {
        return it
    }
    return value
}

/**
 * Helper function to ensure that two optional lists are disjoint
 * Typically used in an add/remove update context
 * Uses the properties to get the name of the properties to generate the failure message
 *
 * @param otherProperty the other property
 * @throws IllegalArgumentException if both lists are present and not disjoint
 */
infix fun KProperty0<OptionalInput<List<*>>>.ensureDisjoint(otherProperty: KProperty0<OptionalInput<List<*>>>) {
    this.get().ifPresent { thisIds ->
        otherProperty.get().ifPresent {
            val commonIds = thisIds intersect it.toSet()
            if (commonIds.isNotEmpty()) {
                throw IllegalArgumentException("`${this.name}` and `${otherProperty.name}` must be disjoint: $commonIds")
            }
        }
    }
}

/**
 * Transforms a list of [gropius.dto.input.common.TypeMappingInput] to a mapping using the provided [typeRepository]
 *
 * @param T the type of the returned types
 * @param typeRepository used to map [ID] to [T]
 * @param validator used to validate the types
 * @return the generated mapping
 */
suspend fun <T : Node> OptionalInput<List<TypeMappingInput>>.toMapping(
    typeRepository: GropiusRepository<T, String>,
    validator: suspend (T) -> Unit
): Map<T, T?> {
    ifPresent { inputs ->
        val allTypeIds = inputs.flatMap { listOf(it.newType, it.oldType) }.filterNotNull().toSet()
        val allTypesById = typeRepository.findAllById(allTypeIds).associateBy { it.graphQLId }
        return inputs.associate { allTypesById[it.oldType]!! to allTypesById[it.newType] }
    }
    return emptyMap()
}