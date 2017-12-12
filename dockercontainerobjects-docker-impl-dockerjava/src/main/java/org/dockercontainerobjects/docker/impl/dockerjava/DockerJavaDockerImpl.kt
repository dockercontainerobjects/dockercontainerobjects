package org.dockercontainerobjects.docker.impl.dockerjava

import com.github.dockerjava.api.DockerClient
import org.dockercontainerobjects.docker.support.AbstractDockerImpl

class DockerJavaDockerImpl(val client: DockerClient) : AbstractDockerImpl() {

    override val images = DockerJavaDockerImagesImpl(this)
    override val containers = DockerJavaDockerContainersImpl(this)

    override fun close() {
        client.close()
    }
}
