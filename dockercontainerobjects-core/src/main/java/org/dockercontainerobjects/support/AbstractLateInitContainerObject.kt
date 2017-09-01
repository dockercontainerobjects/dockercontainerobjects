package org.dockercontainerobjects.support

import org.dockercontainerobjects.util.debug
import org.dockercontainerobjects.util.loggerFor
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeoutException

abstract class AbstractLateInitContainerObject: LateInitContainerObject {

    private val readyCounter = CountDownLatch(1)

    open protected val maxTimeoutMillis get() = 1000*60*5 // 5 minutes

    protected fun markAsReady() {
        l.debug("container is being marked as ready")
        readyCounter.countDown()
    }

    override final val isReady get() = (readyCounter.count == 0L)

    @Throws(TimeoutException::class, InterruptedException::class)
    override final fun waitForReady(timeoutMillis: Int) {
        l.debug { "waiting until container ready, up to $timeoutMillis ms" }
        if (!readyCounter.await(timeoutMillis.toLong(), MILLISECONDS))
            throw TimeoutException()
    }

    override final fun waitForReady() {
        try {
            waitForReady(maxTimeoutMillis)
        } catch (ex: Exception) {
            throw RuntimeException(ex)
        }
    }

    protected open fun onBeforeReady() {}

    companion object {
        private val l = loggerFor<AbstractLateInitContainerObject>()
    }
}
