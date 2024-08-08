package gropius.sync

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component

/**
 * Spring component containing helper function for GitHub sync related json operations
 * @param objectMapper Reference for the spring instance of ObjectMapper
 */
@Component
class JsonHelper(
    val objectMapper: ObjectMapper
) {
    /**
     * Parse a json value into a string
     * For example a `"text"` becomes `text`, while `"text` results in null
     * @param input the json value stringified
     * @return null if null or not a valid json string
     */
    fun parseString(input: String?): String? {
        if (input == null) {
            return null
        }
        return objectMapper.readTree(input).textValue()
    }

    /**
     * Parse a json value into a string
     * Converts the value to a boolean
     */
    fun parseBoolean(input: String?): Boolean {
        if (input == null) {
            return false
        }
        return objectMapper.readTree(input).booleanValue()
    }

    /**
     * Parse a json value into a string
     * For example a `"text"` becomes `text`, while `"text` results in null
     * @param input the json value stringified
     * @return null if null or not a valid json string
     */
    fun parseNumber(input: String?): Double? {
        if (input == null) {
            return null
        }
        val d = objectMapper.readTree(input)
        return if (d.isNumber) d.doubleValue() else null
    }

    /**
     * Parse a json value into a label state map
     * @param input string
     * @return map of label to state, empty if null
     */
    fun parseLabelStateMap(input: String?): Map<String, String> {
        if (input == null) {
            return mapOf()
        }
        return objectMapper.readTree(input).toList().associate {
            it["label"].textValue() to it["state"].textValue()
        }
    }
}