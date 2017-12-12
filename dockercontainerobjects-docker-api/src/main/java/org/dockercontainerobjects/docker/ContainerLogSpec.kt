package org.dockercontainerobjects.docker

import java.time.Instant
import java.util.function.Consumer

class ContainerLogSpec(
        var since: Instant = Instant.EPOCH,
        var standartOutputIncluded: Boolean = true,
        var standarErrorIncluded: Boolean = true,
        var timestampsIncluded: Boolean = false
) {
    var logStartHandler:LogStartEventHandler = {}
    var logEntryHandler: LogEntryEventHandler =  {}
    var logDoneHandler: LogDoneEventHandler = {}

    fun since(since: Instant) =
            this.also { this.since = since }
    fun includeStandartOutput(standartOutputIncluded: Boolean) =
            this.also { this.standartOutputIncluded = standartOutputIncluded }
    fun includeStandarError(standarErrorIncluded: Boolean) =
            this.also { this.standarErrorIncluded = standarErrorIncluded }
    fun includeTimestamps(timestampsIncluded: Boolean) =
            this.also { this.timestampsIncluded = timestampsIncluded }

    fun onLogStart(handler: LogStartEventHandler) =
            this.also { logStartHandler = handler }
    fun onLogEntry(handler: LogEntryEventHandler) =
            this.also { logEntryHandler = handler }
    fun onLogDone(handler: LogDoneEventHandler) =
            this.also { logDoneHandler = handler }
}

typealias  LogStartEventHandler = () -> Unit
typealias LogEntryEventHandler = (ContainerLogEntryContext) -> Unit
typealias  LogDoneEventHandler = () -> Unit

interface ContainerLogEntryContext {

    fun stop()

    val bytes: ByteArray
    val text: String get() = String(bytes)

    val fromStandardOutput: Boolean
    val fromStandardError: Boolean
}
