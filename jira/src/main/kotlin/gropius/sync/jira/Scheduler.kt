package gropius.sync.jira

import gropius.sync.SyncConfigurationProperties
import kotlinx.coroutines.runBlocking
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.SchedulingConfigurer
import org.springframework.scheduling.config.ScheduledTaskRegistrar
import java.time.Instant
import kotlin.system.exitProcess

@Configuration
@EnableScheduling
class Scheduler(
    private val githubSync: JiraSync, private val syncConfigurationProperties: SyncConfigurationProperties
) : SchedulingConfigurer {
    override fun configureTasks(taskRegistrar: ScheduledTaskRegistrar) {
        var timeToNextExecution = 0L
        taskRegistrar.addTriggerTask({
            try {
                runBlocking {
                    githubSync.sync()
                    timeToNextExecution = syncConfigurationProperties.schedulerFallbackTime
                }
            } catch (e: Exception) {
                e.printStackTrace()
                exitProcess(1)//Debug
                timeToNextExecution = syncConfigurationProperties.schedulerFallbackTime
            }
        }, {
            val lastCompletionTime = it.lastCompletion() ?: Instant.now()
            lastCompletionTime.plusMillis(timeToNextExecution)
        })
    }
}