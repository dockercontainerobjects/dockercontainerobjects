package org.dockercontainerobjects.docker

import java.net.Inet4Address
import java.net.InetAddress

interface DockerContainers {

    @Throws(ContainerNotFoundException::class)
    fun getId(name: ContainerName): ContainerId

    @Throws(ContainerNotFoundException::class)
    fun info(locator: ContainerLocator): ContainerInfo

    fun list(
            nameFilter: String? = null,
            idFilter: String? = null,
            fromImage: ImageLocator? = null,
            status: ContainerStatus? = null,
            labels: Map<String, String>? = null,
            includeAll: Boolean = false): List<ContainerInfo>

    @Throws(ContainerNotFoundException::class)
    fun status(locator: ContainerLocator): ContainerStatus

    @Throws(ContainerNotFoundException::class)
    fun address(locator: ContainerLocator): InetAddress

    @Throws(ContainerNotFoundException::class)
    fun inspect(locator: ContainerLocator): ContainerDetailedInfo

    @Throws(ImageNotFoundException::class)
    fun create(spec: ContainerSpec): ContainerId

    @Throws(ContainerNotFoundException::class)
    fun start(locator: ContainerLocator)
    @Throws(ContainerNotFoundException::class)
    fun stop(locator: ContainerLocator): Int
    @Throws(ContainerNotFoundException::class)
    fun restart(locator: ContainerLocator)

    @Throws(ContainerNotFoundException::class)
    fun pause(locator: ContainerLocator)
    @Throws(ContainerNotFoundException::class)
    fun unpause(locator: ContainerLocator)

    @Throws(ContainerNotFoundException::class)
    fun remove(locator: ContainerLocator, force: Boolean = false, removeVolumes: Boolean = false)

    @Throws(ContainerNotFoundException::class)
    fun logs(locator: ContainerLocator, spec: ContainerLogSpec)
}
