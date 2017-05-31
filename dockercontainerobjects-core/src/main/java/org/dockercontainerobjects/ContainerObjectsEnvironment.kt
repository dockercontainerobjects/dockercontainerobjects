package org.dockercontainerobjects

import com.github.dockerjava.api.DockerClient
import java.io.IOException
import java.net.Proxy
import java.net.URL
import java.net.URLConnection
import java.util.concurrent.ConcurrentHashMap

class ContainerObjectsEnvironment(val dockerClient: DockerClient, val dockerNetworkProxy: Proxy): AutoCloseable {

    val manager: ContainerObjectsManager = ContainerObjectsManagerImpl(this)

    val enhancer: ContainerObjectsClassEnhancer = ContainerObjectsClassEnhancerImpl(this)

    private val containers: MutableMap<Any, ContainerObjectContext<*>> = ConcurrentHashMap()

    init {
        ExtensionManager.setupEnvironment(this)
    }

    @Throws(IOException::class)
    override fun close() {
        ExtensionManager.teardownEnvironment(this)
        dockerClient.close()
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
}
