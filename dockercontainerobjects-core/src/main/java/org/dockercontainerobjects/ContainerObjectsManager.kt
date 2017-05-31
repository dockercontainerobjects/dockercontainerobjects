package org.dockercontainerobjects

import com.github.dockerjava.api.model.NetworkSettings
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

    fun getContainerId(containerInstance: Any): String?
    fun getContainerStatus(containerInstance: Any): ContainerStatus
    fun isContainerRunning(containerInstance: Any): Boolean =
        getContainerStatus(containerInstance) == ContainerStatus.STARTED
    fun getContainerNetworkSettings(containerInstance: Any): NetworkSettings?
    fun <ADDR: InetAddress> getContainerAddress(containerInstance: Any, addrType: Class<ADDR>): ADDR?
    fun getContainerAddress(containerInstance: Any): InetAddress? =
        getContainerAddress(containerInstance, InetAddress::class.java)
}
