package gropius

import gropius.sync.github.Incoming
import io.github.graphglue.data.repositories.EnableGraphglueRepositories
import kotlinx.coroutines.runBlocking
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import org.springframework.stereotype.Component

@Configuration
class SyncApplication(
    val incoming: Incoming
) {
    @EventListener(ApplicationReadyEvent::class)
    fun doSomethingAfterStartup() {
        runBlocking {
            incoming.sync()
        }
    }
}

@SpringBootApplication
@EnableGraphglueRepositories
@EnableReactiveMongoRepositories
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
