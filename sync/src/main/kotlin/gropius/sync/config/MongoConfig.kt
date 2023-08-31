package gropius.sync.config

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.data.mongodb.core.convert.MongoCustomConversions
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

/**
 * Custom configuration for the mongodb driver implementing custom conversion for offsetdatetime
 */
@Configuration
class MongoConfig {

    /**
     * Bean providing custom converter
     * @return resulting converter list
     */
    @Bean
    fun customConversions(): MongoCustomConversions {
        return MongoCustomConversions(
            listOf(
                OffsetDateTimeReadConverter(),
                OffsetDateTimeWriteConverter(),
                JsonElementReadConverter(),
                JsonElementWriteConverter(),
                JsonObjectReadConverter(),
                JsonObjectWriteConverter()
            )
        )
    }

    /**
     * MongoDB write type converter
     */
    internal class OffsetDateTimeWriteConverter : Converter<OffsetDateTime, Date?> {
        override fun convert(source: OffsetDateTime): Date? {
            return Date.from(source.toInstant().atZone(ZoneOffset.UTC).toInstant())
        }
    }

    /**
     * MongoDB read type converter
     */
    internal class OffsetDateTimeReadConverter : Converter<Date, OffsetDateTime?> {
        override fun convert(source: Date): OffsetDateTime? {
            return source.toInstant()?.atOffset(ZoneOffset.UTC)
        }
    }

    /**
     * MongoDB write type converter
     */
    internal class JsonElementWriteConverter : Converter<JsonElement, String> {
        override fun convert(source: JsonElement): String {
            return source.toString()
        }
    }

    /**
     * MongoDB read type converter
     */
    internal class JsonElementReadConverter : Converter<String, JsonElement> {
        override fun convert(source: String): JsonElement {
            return Json.parseToJsonElement(source)
        }
    }

    /**
     * MongoDB write type converter
     */
    internal class JsonObjectWriteConverter : Converter<JsonObject, String> {
        override fun convert(source: JsonObject): String {
            return source.toString()
        }
    }

    /**
     * MongoDB read type converter
     */
    internal class JsonObjectReadConverter : Converter<String, JsonObject> {
        override fun convert(source: String): JsonObject {
            return Json.parseToJsonElement(source).jsonObject
        }
    }
}