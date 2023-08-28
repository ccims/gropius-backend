package gropius.sync.config

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
                OffsetDateTimeWriteConverter()
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
}