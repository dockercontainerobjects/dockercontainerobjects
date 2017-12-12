package org.dockercontainerobjects.docker

interface ImageDetailedInfo : ImageInfo {

    val author: String
    val os: String
    val architecture: String
    val labels: Map<String, String>
    val environment: Map<String, String>

    fun equals(other: ImageDetailedInfo) =
            equals(other as ImageInfo) &&
                    author == other.author && os == other.os && architecture == other.architecture

    override val representation: String get() = "${super.representation}, author: $author, os: $os, architecture: $architecture"
}
