package org.dockercontainerobjects

interface LogEntryContext {

    fun stop()
    val entryBytes: ByteArray
    val entryText: String get() = String(entryBytes)
    val isStdOut: Boolean
    val isStdErr: Boolean
}
