package org.dockercontainerobjects.docker.impl.dockerjava

import com.github.dockerjava.api.model.Container
import org.dockercontainerobjects.docker.ContainerId
import org.dockercontainerobjects.docker.ContainerName
import org.dockercontainerobjects.docker.support.AbstractContainerInfo
import java.time.Instant

class DockerJavaModelContainerInfoImpl(private val model: Container): AbstractContainerInfo() {

    override val id
        get() = ContainerId(model.id ?: throw IllegalStateException())

    override val created
        get() = model.created.let {
            if (it != null) Instant.ofEpochSecond(it) else Instant.EPOCH
        }

    override val names by lazy {
        model.names.let {
            if (it != null) it.map { ContainerName(it) } else emptyList()
        }
    }
}
