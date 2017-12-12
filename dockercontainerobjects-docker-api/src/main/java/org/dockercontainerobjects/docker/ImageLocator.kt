package org.dockercontainerobjects.docker

sealed class ImageLocator

class ImageId(val id: String): ImageLocator() {

    companion object {
        const val SEPARATOR: Char = ':'
        const val ALGORITHM_SHA256 = "sha256"
    }

    /** the algorithm part of the name. may be empty. usually sha256 */
    val algorithm: String
        get() = id.substringBefore(SEPARATOR, "")

    /** the hash part of the name. will never be empty. may be a subset of the full name */
    val hash: String
        get() = id.substringAfter(SEPARATOR)

    val partial get() =
        when {
            algorithm.isBlank() -> true
            algorithm == ALGORITHM_SHA256 -> hash.length < 256
            else -> true // unknown algorithm. assume it's partial
        }

    fun matches(other: ImageId) =
            if (partial) other.hash.startsWith(hash) else equals(other)

    override fun toString() = id

    override fun hashCode() = id.hashCode()

    fun equals(other: ImageId) = (id == other.id)

    override fun equals(other: Any?) =
            other != null && other is ImageId && equals(other)
}

class ImageName(val name: String): ImageLocator() {

    val repository: String
        get() = name.substringBeforeLast(SEPARATOR)

    val tag: String
        get() = name.substringAfterLast(SEPARATOR, LATEST)

    override fun toString() = name

    override fun hashCode() = name.hashCode()

    fun equals(other: ImageName) = (name == other.name)

    override fun equals(other: Any?) =
            other != null && other is ImageName && equals(other)

    companion object {

        const val SEPARATOR: Char = ':'
        const val LATEST: String = "latest"
    }
}
