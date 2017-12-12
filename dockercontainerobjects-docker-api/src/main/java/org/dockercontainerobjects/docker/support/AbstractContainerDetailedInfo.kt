package org.dockercontainerobjects.docker.support

import org.dockercontainerobjects.docker.ContainerDetailedInfo

abstract class AbstractContainerDetailedInfo : AbstractContainerInfo(), ContainerDetailedInfo {

    override fun equals(other: Any?) =
            other != null && other is ContainerDetailedInfo && equals(other)
}
