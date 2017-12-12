package org.dockercontainerobjects

import org.dockercontainerobjects.docker.Addresses
import org.dockercontainerobjects.docker.ContainerLocator
import org.dockercontainerobjects.docker.NetworkSettings
import java.net.InetAddress

interface ContainerObjectsManager: AutoCloseable {

    companion object {

        const val IMAGE_TAG_DYNAMIC_PLACEHOLDER = "*"

        const val SCHEME_CLASSPATH = "classpath"
        const val SCHEME_FILE = "file"
        const val SCHEME_HTTP = "http"
        const val SCHEME_HTTPS = "https"

        const val DOCKERFILE_DEFAULT_NAME = "Dockerfile"
    }

    enum class ContainerStatus {
        CREATED, STARTED, STOPPED, UNKNOWN
    }

    fun <T: Any> create(containerType: Class<T>): T
    fun <T: Any> destroy(containerInstance: T)
    fun <T: Any> restart(containerInstance: T)

    fun getContainerId(containerInstance: Any): ContainerLocator?
    fun getContainerStatus(containerInstance: Any): ContainerStatus
    fun isContainerRunning(containerInstance: Any): Boolean =
            getContainerStatus(containerInstance) == ContainerStatus.STARTED
    fun getContainerNetworkSettings(containerInstance: Any): NetworkSettings?
    fun getContainerAddresses(containerInstance: Any): Addresses?
    fun getContainerAddress(containerInstance: Any): InetAddress? =
            getContainerAddresses(containerInstance)?.preferred
}
