package org.dockercontainerobjects.support

import org.dockercontainerobjects.LogEntryContext
import org.dockercontainerobjects.annotations.OnLogEntry
import org.dockercontainerobjects.util.debug
import org.dockercontainerobjects.util.loggerFor
import java.util.regex.Pattern

abstract class LogBasedLateInitContainerObject: AbstractLateInitContainerObject() {

    companion object {
        private val l = loggerFor<LogBasedLateInitContainerObject>()
    }

    abstract protected val serverReadyLogEntry: Pattern

    @OnLogEntry
    private fun onLogEntry(ctx: LogEntryContext) {
        l.debug { "inspecting log entry: ${ctx.entryText.trim()}" }
        if (serverReadyLogEntry.matcher(ctx.entryText).find()) {
            markAsReady()
            ctx.stop()
        }
    }
}
