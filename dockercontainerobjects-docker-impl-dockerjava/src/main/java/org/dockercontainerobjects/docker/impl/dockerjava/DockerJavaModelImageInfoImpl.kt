package org.dockercontainerobjects.docker.impl.dockerjava

import com.github.dockerjava.api.model.Image
import org.dockercontainerobjects.docker.ImageId
import org.dockercontainerobjects.docker.ImageName
import org.dockercontainerobjects.docker.support.AbstractImageInfo
import java.time.Instant

class DockerJavaModelImageInfoImpl(private val model: Image): AbstractImageInfo() {

    override val id: ImageId
        get() = ImageId(model.id ?: throw IllegalStateException())

    override val created: Instant
        get() = model.created.let {
            if (it != null) Instant.ofEpochSecond(it) else Instant.EPOCH
        }

    override val size: Long
        get() = model.size ?: 0

    override val tags by lazy {
        model.repoTags.let {
            if (it != null) it.map { ImageName(it) } else emptyList()
        }
    }
}
