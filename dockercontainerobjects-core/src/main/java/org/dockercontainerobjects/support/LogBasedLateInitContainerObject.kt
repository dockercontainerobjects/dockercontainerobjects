package org.dockercontainerobjects.support

import org.dockercontainerobjects.ContainerObjectsEnvironment
import org.dockercontainerobjects.annotations.OnLogEntry
import org.dockercontainerobjects.docker.ContainerLogEntryContext
import org.dockercontainerobjects.util.debug
import org.dockercontainerobjects.util.loggerFor
import java.util.concurrent.CompletableFuture
import java.util.regex.Pattern
import javax.inject.Inject

abstract class LogBasedLateInitContainerObject: AbstractLateInitContainerObject() {

    companion object {
        private val l = loggerFor<LogBasedLateInitContainerObject>()
    }

    @Inject
    protected lateinit var environment: ContainerObjectsEnvironment

    abstract protected val serverReadyLogEntry: Pattern

    @OnLogEntry
    private fun onLogEntry(ctx: ContainerLogEntryContext) {
        val text = ctx.text
        l.debug { "inspecting log entry: ${text.trim()}" }
        if (serverReadyLogEntry.matcher(text).find()) {
            ctx.stop()
            CompletableFuture
                    .runAsync(Runnable { onBeforeReady() }, environment.executor)
                    .thenRunAsync(Runnable { markAsReady() }, environment.executor)
        }
    }
}
