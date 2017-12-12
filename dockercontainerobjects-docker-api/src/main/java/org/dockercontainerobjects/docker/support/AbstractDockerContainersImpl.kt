package org.dockercontainerobjects.docker.support

import org.dockercontainerobjects.docker.ContainerId
import org.dockercontainerobjects.docker.ContainerInfo
import org.dockercontainerobjects.docker.ContainerLocator
import org.dockercontainerobjects.docker.ContainerName
import org.dockercontainerobjects.docker.ContainerNotFoundException
import org.dockercontainerobjects.docker.DockerContainers
import org.dockercontainerobjects.docker.ImageNotFoundException
import java.net.InetAddress

abstract class AbstractDockerContainersImpl<out C: AbstractDockerImpl>(protected val docker: C)
        : DockerContainers {

    companion object {

        @JvmStatic
        fun infoFilter(locator: ContainerLocator) =
                when(locator) {
                    is ContainerId -> { info: ContainerInfo -> locator.matches(info.id) }
                    is ContainerName -> { info: ContainerInfo -> info.names.contains(locator) }
                }
    }

    override fun getId(name: ContainerName) = info(name).id

    override fun status(locator: ContainerLocator) = inspect(locator).status

    override fun address(locator: ContainerLocator) = inspect(locator).network.addresses.preferred

    protected fun locate(locator: ContainerLocator) =
            list(
                    nameFilter = (locator as? ContainerName)?.toString(),
                    idFilter = (locator as? ContainerId)?.toString(),
                    includeAll = true)
                    .filter(infoFilter(locator))

    override fun info(locator: ContainerLocator) =
            try {
                locate(locator).first()
            } catch (e: NoSuchElementException) {
                throw ImageNotFoundException(e)
            }

    fun ContainerLocator.toId() =
            when (this) {
                is ContainerName -> getId(this)
                is ContainerId -> this
            }
}
