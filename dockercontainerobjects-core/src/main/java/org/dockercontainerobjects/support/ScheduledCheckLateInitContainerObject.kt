package org.dockercontainerobjects.support

import org.dockercontainerobjects.ContainerObjectsEnvironment
import org.dockercontainerobjects.annotations.AfterContainerStarted
import org.dockercontainerobjects.util.debug
import org.dockercontainerobjects.util.loggerFor
import java.time.Instant
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject

abstract class ScheduledCheckLateInitContainerObject: AbstractLateInitContainerObject() {

    companion object {
        private val l = loggerFor<ScheduledCheckLateInitContainerObject>()
    }

    private lateinit var started: Instant
    private lateinit var checkfuture: ScheduledFuture<*>
    open protected val checkFrequencyMillis get() = 250 // 0.25 seconds

    @Inject
    protected lateinit var environment: ContainerObjectsEnvironment

    protected abstract fun isServerReady(): Boolean

    @AfterContainerStarted
    private fun onContainerStarted() {
        started = Instant.now()
        l.debug { "server starting at $started" }
        scheduleCheck()
    }

    private fun scheduleCheck() {
        l.debug { "scheduling a check every $checkFrequencyMillis ms" }
        checkfuture = environment.executor.scheduleWithFixedDelay(this::checkServerReady, 0, checkFrequencyMillis.toLong(), MILLISECONDS)
    }

    private fun cancelCheck(successful: Boolean) {
        l.debug { "canceling scheduled check. ready: $successful" }
        checkfuture.cancel(false)
        if (successful) markAsReady()
    }

    private fun isCheckExpired() =
            started.plusMillis(maxTimeoutMillis.toLong()).isBefore(Instant.now())

    private fun checkServerReady() {
        // server ready!, stop
        if (isServerReady())
            cancelCheck(true)
        // time expired, stop
        else if (isCheckExpired())
            cancelCheck(false)
        // if no decision, will try again
    }
}
