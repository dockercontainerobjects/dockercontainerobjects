package org.dockercontainerobjects

import org.dockercontainerobjects.docker.Docker
import org.dockercontainerobjects.util.debug
import org.dockercontainerobjects.util.loggerFor
import java.io.IOException
import java.net.Proxy
import java.net.URL
import java.net.URLConnection
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

class ContainerObjectsEnvironment(
        val docker: Docker,
        val dockerNetworkProxy: Proxy,
        executorService: ScheduledExecutorService?): AutoCloseable {

    val manager: ContainerObjectsManager = ContainerObjectsManagerImpl(this)

    val enhancer: ContainerObjectsClassEnhancer = ContainerObjectsClassEnhancerImpl(this)

    private var executorInternallyManaged: Boolean
    private var executorInternal: ScheduledExecutorService?

    init {
        executorInternal = executorService
        executorInternallyManaged = false
    }

    val executor: ScheduledExecutorService
            get() {
                if (executorInternal == null) {
                    executorInternallyManaged = true
                    executorInternal = Executors.newScheduledThreadPool(
                            Runtime.getRuntime().availableProcessors(), EnvironmentThreadFactory)
                }
                return executorInternal!!
            }

    private val containers: MutableMap<Any, ContainerObjectContext<*>> = ConcurrentHashMap()

    init {
        ExtensionManager.setupEnvironment(this)
    }

    constructor(docker: Docker, dockerNetworkProxy: Proxy): this(docker, dockerNetworkProxy, null)

    @Throws(IOException::class)
    override fun close() {
        ExtensionManager.teardownEnvironment(this)
        docker.close()
        if (executorInternallyManaged)
            executorInternal!!.shutdown()
    }

    @Throws(IOException::class)
    fun openOnDockerNetwork(url: URL): URLConnection = url.openConnection(dockerNetworkProxy)

    internal fun registerContainerObject(ctx: ContainerObjectContext<*>) {
        if (ctx.environment !== this)
            throw IllegalArgumentException("Container object belongs to a diferent environment")
        val instance = ctx.instance ?: IllegalArgumentException("Container object is not initialized")
        containers.put(instance, ctx)
    }

    internal fun unregisterContainerObject(ctx: ContainerObjectContext<*>) {
        if (ctx.environment !== this)
            throw IllegalArgumentException("Container object belongs to a diferent environment")
        val instance = ctx.instance ?: throw IllegalArgumentException("Container object is not initialized")
        val removed = containers.remove(instance, ctx)
        if (!removed)
            throw IllegalStateException("Container object was not unregistered. It wasn't found on this environment")
    }

    internal fun getContainerObjectRegistration(containerInstance: Any): ContainerObjectContext<*> {
        return containers[containerInstance] ?: throw IllegalArgumentException("Provided container instance is not registered in this environment")
    }

    object EnvironmentThreadFactory: ThreadFactory {

        override fun newThread(r: Runnable) = EnvironmentThread(r)
    }

    class EnvironmentThread(r: Runnable): Thread(r) {

        val threadId = threadCounter.incrementAndGet()

        override fun run() {
            l.debug { "environment thread $threadId starting" }
            try {
                super.run()
            } finally {
                l.debug { "environment thread $threadId ending" }
            }
        }
    }

    companion object {
        private val l = loggerFor<ContainerObjectsEnvironment>()

        @JvmField internal val threadCounter = AtomicInteger()
    }
}
