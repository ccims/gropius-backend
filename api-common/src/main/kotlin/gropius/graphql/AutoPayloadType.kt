package gropius.graphql

/**
 * Can be applied to GraphQL mutation functions
 * Results in automatically generated payload return types
 * The generated type has a single field, which is the return value of the method
 * annotated with this annotation
 *
 * @param description the description of the single field of the generated payload type
 * @param fieldName the name of the field in the generated payload type, defaults to lowercased return type name
 */
@MustBeDocumented
@Target(AnnotationTarget.FUNCTION)
annotation class AutoPayloadType(val description: String, val fieldName: String = "")
