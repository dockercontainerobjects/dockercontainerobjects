package org.dockercontainerobjects.docker

interface DockerImages {

    @Throws(ImageNotFoundException::class)
    fun getId(name: ImageName): ImageId

    fun isAvailable(locator: ImageLocator): Boolean

    @Throws(ImageNotFoundException::class)
    fun info(locator: ImageLocator): ImageInfo

    fun list(nameFilter: String? = null, labels: Map<String, String>? = null): List<ImageInfo>

    @Throws(ImageNotFoundException::class)
    fun inspect(locator: ImageLocator): ImageDetailedInfo

    @Throws(ImageNotFoundException::class)
    fun pull(name: ImageName)

    fun build(spec: ImageSpec): ImageId

    @Throws(ImageNotFoundException::class)
    fun remove(locator: ImageLocator, force: Boolean = false)
}
