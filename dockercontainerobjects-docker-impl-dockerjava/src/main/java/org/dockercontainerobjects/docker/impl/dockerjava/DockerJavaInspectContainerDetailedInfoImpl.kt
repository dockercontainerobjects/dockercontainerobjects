package org.dockercontainerobjects.docker.impl.dockerjava

import com.github.dockerjava.api.command.InspectContainerResponse
import org.dockercontainerobjects.docker.ContainerId
import org.dockercontainerobjects.docker.ContainerName
import org.dockercontainerobjects.docker.ContainerStatus
import org.dockercontainerobjects.docker.NetworkSettings
import org.dockercontainerobjects.docker.support.AbstractContainerDetailedInfo
import java.time.Instant

class DockerJavaInspectContainerDetailedInfoImpl(private val response: InspectContainerResponse)
    : AbstractContainerDetailedInfo() {

    companion object {

        const val SEPARATOR = '='
    }

    override val id
        get() = ContainerId(response.id ?: throw IllegalStateException())

    override val created
        get() = response.created?.let { Instant.parse(it) } ?: Instant.EPOCH

    override val names by lazy {
        response.name?.let {
            listOf(ContainerName(if (it.startsWith("/")) it.substring(1) else it))
        }.orEmpty()
    }

    override val status
        get() = ContainerStatus.valueOf(
                response.state.status?.toUpperCase() ?: throw IllegalStateException()
        )

    override val environment by lazy {
        response.config?.env?.map {
            it.substringBefore(SEPARATOR, it) to it.substringAfter(SEPARATOR, "")
        }?.toMap().orEmpty()
    }

    override val labels: Map<String, String> by lazy {
        response.config?.labels?.toMap().orEmpty()
    }

    override val network =
            DockerJavaNetworkSettings(response.networkSettings ?: throw IllegalStateException())
}
