package org.dockercontainerobjects

import java.io.IOException
import java.net.Proxy
import java.net.URL
import java.util.Map
import java.util.concurrent.ConcurrentHashMap
import com.github.dockerjava.api.DockerClient
import org.eclipse.xtend.lib.annotations.Accessors
import org.eclipse.xtend.lib.annotations.Data

class ContainerObjectsEnvironment implements AutoCloseable {

    @Accessors(PUBLIC_GETTER)
    val DockerClient dockerClient

    @Accessors(PUBLIC_GETTER)
    val ContainerObjectsManager manager

    @Accessors(PUBLIC_GETTER)
    val ContainerObjectsClassEnhancer enhancer

    @Accessors(PUBLIC_GETTER)
    val Proxy dockerNetworkProxy

    private val Map<Object, RegistrationInfo> containers = new ConcurrentHashMap

    protected new(DockerClient dockerClient, Proxy dockerNetworkProxy) {
        this.dockerClient = dockerClient
        this.dockerNetworkProxy = dockerNetworkProxy
        manager = new ContainerObjectsManagerImpl(this)
        enhancer = new ContainerObjectsClassEnhancerImpl(this)
    }

    override close() throws IOException {
        dockerClient.close
    }

    def openOnDockerNetwork(URL url) throws IOException {
        url.openConnection(dockerNetworkProxy)
    }

    protected def registerContainer(RegistrationInfo info) {
        containers.put(info.container.instance, info)
    }

    protected def unregisterContainer(Object containerInstance) {
        containers.remove(containerInstance)
    }

    protected def getRegistrationInfo(Object containerInstance) {
        return containers.get(containerInstance)
    }

    @Data protected static class ImageRegistrationInfo {
        String id
        boolean dynamic
        boolean autoRemove
    }

    @Data protected static class ContainerRegistrationInfo {
        String id
        Object instance
    }

    @Data protected static class RegistrationInfo {
        ImageRegistrationInfo image
        ContainerRegistrationInfo container
    }
}
