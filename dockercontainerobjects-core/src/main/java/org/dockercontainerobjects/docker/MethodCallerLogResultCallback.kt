package org.dockercontainerobjects.docker

import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.api.model.StreamType.STDERR
import com.github.dockerjava.api.model.StreamType.STDOUT
import com.github.dockerjava.core.async.ResultCallbackTemplate
import org.dockercontainerobjects.ContainerObjectContext
import org.dockercontainerobjects.ContainerObjectLifecyclePhase.CONTAINER_OBJECT_DESTRUCTION
import org.dockercontainerobjects.LogEntryContext
import org.dockercontainerobjects.util.call
import java.lang.reflect.Method

sealed class MethodCallerLogResultCallback<T: Any>(val context: ContainerObjectContext<*>, val method: Method):
        ResultCallbackTemplate<MethodCallerLogResultCallback<T>, Frame>() {

    override fun onNext(frame: Frame) {
        if (context.phase != CONTAINER_OBJECT_DESTRUCTION) {
            val argument = onGenerateCallbackArgument(frame)
            method.call(context.instance, argument)
            onPostProcessCallbackArgument(frame, argument)
        } else
            close()
    }

    protected abstract fun onGenerateCallbackArgument(frame: Frame): T
    protected open fun onPostProcessCallbackArgument(frame: Frame, argument: T) {}

    class ByteArrayArgumentMethodCallback(context: ContainerObjectContext<*>, method: Method):
            MethodCallerLogResultCallback<ByteArray>(context, method) {

        override fun onGenerateCallbackArgument(frame: Frame): ByteArray = frame.payload
    }

    class StringArgumentMethodCallback(context: ContainerObjectContext<*>, method: Method):
            MethodCallerLogResultCallback<String>(context, method) {

        override fun onGenerateCallbackArgument(frame: Frame) = String(frame.payload)
    }

    class LogEntryContextArgumentMethodCallback(context: ContainerObjectContext<*>, method: Method):
            MethodCallerLogResultCallback<FrameLogEntryContext>(context, method) {

        override fun onGenerateCallbackArgument(frame: Frame) = FrameLogEntryContext(frame)

        override fun onPostProcessCallbackArgument(frame: Frame, argument: FrameLogEntryContext) {
            if (argument.stopRequested)
                close()
        }
    }

    class FrameLogEntryContext(val frame: Frame): LogEntryContext {

        var stopRequested: Boolean = false
            private set

        override fun stop() {
            stopRequested = true
        }

        override val entryBytes: ByteArray get() = frame.payload
        override val isStdOut: Boolean get() = frame.streamType == STDOUT
        override val isStdErr: Boolean get() = frame.streamType == STDERR
    }
}
