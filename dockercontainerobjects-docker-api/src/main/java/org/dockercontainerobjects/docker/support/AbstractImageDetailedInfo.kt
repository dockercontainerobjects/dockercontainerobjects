package org.dockercontainerobjects.docker.support

import org.dockercontainerobjects.docker.ImageDetailedInfo

@Suppress("EqualsOrHashCode")
abstract class AbstractImageDetailedInfo: AbstractImageInfo(), ImageDetailedInfo {

    override fun equals(other: Any?) =
            other != null && other is ImageDetailedInfo && equals(other)
}
