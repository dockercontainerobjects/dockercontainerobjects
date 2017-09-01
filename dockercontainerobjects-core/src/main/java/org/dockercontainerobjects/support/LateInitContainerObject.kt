package org.dockercontainerobjects.support

import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeoutException

interface LateInitContainerObject {

    val isReady: Boolean

    @Throws(TimeoutException::class, InterruptedException::class)
    fun waitForReady(timeoutMillis: Int)

    fun waitForReady()
}
