package org.dockercontainerobjects.docker

import java.time.Instant

interface ImageInfo {

    val id: ImageId
    val created: Instant
    val size: Long
    val tags: List<ImageName>

    fun equals(other: ImageInfo) =
            (id == other.id && created == other.created && size == other.size)

    val representation: String get() = "name: $id, created: $created, size: $size, tags: $tags"
}
