package org.dockercontainerobjects.docker.impl.dockerjava

import com.github.dockerjava.api.command.InspectImageResponse
import org.dockercontainerobjects.docker.ImageId
import org.dockercontainerobjects.docker.ImageName
import org.dockercontainerobjects.docker.support.AbstractImageDetailedInfo
import java.time.Instant

class DockerJavaInspectImageDetailedInfoImpl(private val response: InspectImageResponse)
    : AbstractImageDetailedInfo() {

    companion object {
        const val SEPARATOR = '='
    }

    override val id
        get() = ImageId(response.id ?: throw IllegalStateException())

    override val created
        get() = response.created?.let { Instant.parse(it) } ?: Instant.EPOCH

    override val size: Long
        get() = response.size ?: 0

    override val author
        get() = response.author.orEmpty()

    override val os
        get() = response.os.orEmpty()

    override val architecture
        get() = response.arch.orEmpty()

    override val tags by lazy {
        response.repoTags?.map { ImageName(it) }.orEmpty()
    }

    override val environment: Map<String, String> by lazy {
        response.config?.env?.map {
            it.substringBefore(SEPARATOR, it) to it.substringAfter(SEPARATOR, "")
        }?.toMap().orEmpty()
    }

    override val labels: Map<String, String> by lazy {
        response.config?.labels?.toMap().orEmpty()
    }
}
