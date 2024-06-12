package gropius.sync.github

import gropius.GithubConfigurationProperties
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
    private val githubSync: GithubSync,
    private val syncConfigurationProperties: SyncConfigurationProperties,
    private val githubConfigurationProperties: GithubConfigurationProperties
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
                if (githubConfigurationProperties.dieOnError) {
                    exitProcess(1)//Debug
                }
                timeToNextExecution = syncConfigurationProperties.schedulerFallbackTime
            }
        }, {
            val lastCompletionTime = it.lastCompletion() ?: Instant.now()
            lastCompletionTime.plusMillis(timeToNextExecution)
        })
    }
}