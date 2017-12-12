package org.dockercontainerobjects.docker.support

import org.dockercontainerobjects.docker.ImageInfo

abstract class AbstractImageInfo: ImageInfo {

    override fun equals(other: Any?) =
            other != null && other is ImageInfo && equals(other)

    override fun hashCode() = id.hashCode()

    override fun toString() = "($representation)"
}
