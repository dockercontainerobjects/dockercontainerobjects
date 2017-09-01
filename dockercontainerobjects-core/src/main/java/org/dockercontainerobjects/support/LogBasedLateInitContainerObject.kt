package org.dockercontainerobjects.support

import org.dockercontainerobjects.ContainerObjectsEnvironment
import org.dockercontainerobjects.LogEntryContext
import org.dockercontainerobjects.annotations.OnLogEntry
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
    private fun onLogEntry(ctx: LogEntryContext) {
        l.debug { "inspecting log entry: ${ctx.entryText.trim()}" }
        if (serverReadyLogEntry.matcher(ctx.entryText).find()) {
            ctx.stop()
            CompletableFuture
                    .runAsync(Runnable { onBeforeReady() }, environment.executor)
                    .thenRunAsync(Runnable { markAsReady() }, environment.executor)
        }
    }
}
