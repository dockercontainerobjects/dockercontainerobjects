package org.dockercontainerobjects

import java.io.IOException
import java.net.Proxy
import java.net.URL
import java.util.Map
import java.util.concurrent.ConcurrentHashMap
import com.github.dockerjava.api.DockerClient
import org.eclipse.xtend.lib.annotations.Accessors

class ContainerObjectsEnvironment implements AutoCloseable {

    @Accessors(PUBLIC_GETTER)
    val DockerClient dockerClient

    @Accessors(PUBLIC_GETTER)
    val ContainerObjectsManager manager

    @Accessors(PUBLIC_GETTER)
    val ContainerObjectsClassEnhancer enhancer

    @Accessors(PUBLIC_GETTER)
    val Proxy dockerNetworkProxy

    private val Map<Object, ContainerObjectContext<?>> containers = new ConcurrentHashMap

    protected new(DockerClient dockerClient, Proxy dockerNetworkProxy) {
        this.dockerClient = dockerClient
        this.dockerNetworkProxy = dockerNetworkProxy
        manager = new ContainerObjectsManagerImpl(this)
        enhancer = new ContainerObjectsClassEnhancerImpl(this)
        ExtensionManager.instance.setupEnvironment(this)
    }

    override close() throws IOException {
        ExtensionManager.instance.teardownEnvironment(this)
        dockerClient.close
    }

    def openOnDockerNetwork(URL url) throws IOException {
        url.openConnection(dockerNetworkProxy)
    }

    protected def registerContainerObject(ContainerObjectContext<?> ctx) {
        if (ctx.environment !== this)
            throw new IllegalArgumentException("Container object belongs to a diferent environment")
        if (ctx.instance === null)
            throw new IllegalArgumentException("Container object is not initialized")
        containers.put(ctx.instance, ctx)
    }

    protected def unregisterContainerObject(ContainerObjectContext<?> ctx) {
        if (ctx.environment !== this)
            throw new IllegalArgumentException("Container object belongs to a diferent environment")
        if (ctx.instance === null)
            throw new IllegalArgumentException("Container object is not initialized")
        val removed = containers.remove(ctx.instance, ctx)
        if (!removed)
            throw new IllegalStateException("Container object was not unregistered. It wasn't found on this environment")
    }

    protected def getContainerObjectRegistration(Object containerInstance) {
        val ctx = containers.get(containerInstance)
        if (ctx === null)
            throw new IllegalArgumentException("Provided container instance is not registered in this environment")
        return containers.get(containerInstance)
    }
}
