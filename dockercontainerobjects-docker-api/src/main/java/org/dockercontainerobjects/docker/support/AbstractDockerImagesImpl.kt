package org.dockercontainerobjects.docker.support

import org.dockercontainerobjects.docker.DockerImages
import org.dockercontainerobjects.docker.ImageId
import org.dockercontainerobjects.docker.ImageInfo
import org.dockercontainerobjects.docker.ImageLocator
import org.dockercontainerobjects.docker.ImageName
import org.dockercontainerobjects.docker.ImageNotFoundException

abstract class AbstractDockerImagesImpl<out C: AbstractDockerImpl>(protected val docker: C)
        : DockerImages {

    companion object {

        @JvmStatic
        fun infoFilter(locator: ImageLocator) =
                when(locator) {
                    is ImageId -> { info: ImageInfo -> locator.matches(info.id) }
                    is ImageName -> { info: ImageInfo -> info.tags.contains(locator) }
                }
    }

    override fun getId(name: ImageName) = info(name).id

    protected fun locate(locator: ImageLocator) =
            list(nameFilter = (locator as? ImageName)?.toString())
                    .filter(infoFilter(locator))

    override fun isAvailable(locator: ImageLocator) =
            locate(locator).isNotEmpty()

    override fun info(locator: ImageLocator) =
            try {
                locate(locator).first()
            } catch (e: NoSuchElementException) {
                throw ImageNotFoundException(e)
            }

    fun ImageLocator.toId() =
            when (this) {
                is ImageName -> getId(this)
                is ImageId -> this
            }
}
