package org.dockercontainerobjects.docker.support

import org.dockercontainerobjects.docker.ContainerInfo

abstract class AbstractContainerInfo : ContainerInfo {

    override fun equals(other: Any?) =
            other != null && other is ContainerInfo && equals(other)

    override fun hashCode() = id.hashCode()

    override fun toString() = "($representation)"
}
