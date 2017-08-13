package org.dockercontainerobjects.support

import org.dockercontainerobjects.ContainerObjectsEnvironment
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeoutException
import javax.inject.Inject

abstract class AbstractLateInitContainerObject: LateInitContainerObject {

    private val readyCounter = CountDownLatch(1)

    open protected val maxTimeoutMillis get() = 1000*60*5 // 5 minutes

    @Inject
    protected lateinit var environment: ContainerObjectsEnvironment

    protected fun markAsReady() {
        readyCounter.countDown()
    }

    override final val isReady get() = (readyCounter.count == 0L)

    @Throws(TimeoutException::class, InterruptedException::class)
    override final fun waitForReady(timeoutMillis: Int) {
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

    override final fun whenReady(): CompletableFuture<Void> =
            CompletableFuture.runAsync(Runnable { waitForReady() }, environment.executor)
}
