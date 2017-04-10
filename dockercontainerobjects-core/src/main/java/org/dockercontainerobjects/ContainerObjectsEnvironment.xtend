package org.dockercontainerobjects

import java.io.IOException
import java.util.Map
import java.util.concurrent.ConcurrentHashMap
import org.eclipse.xtend.lib.annotations.Accessors
import org.eclipse.xtend.lib.annotations.Data
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.DockerClientBuilder

class ContainerObjectsEnvironment implements AutoCloseable {

    @Accessors(PUBLIC_GETTER)
    val DockerClient dockerClient

    @Accessors(PUBLIC_GETTER)
    val ContainerObjectsManager manager

    @Accessors(PUBLIC_GETTER)
    val ContainerObjectsClassEnhancer enhancer

    private val Map<Object, RegistrationInfo> containers = new ConcurrentHashMap

    new(DockerClient dockerClient) {
        this.dockerClient = dockerClient
        manager = new ContainerObjectsManagerImpl(this)
        enhancer = new ContainerObjectsClassEnhancerImpl(this)
    }

    new() {
        this(DockerClientBuilder.instance.build)
    }

    override close() throws IOException {
        dockerClient.close
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
