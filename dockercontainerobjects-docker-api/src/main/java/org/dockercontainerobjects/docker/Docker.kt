package org.dockercontainerobjects.docker

interface Docker: AutoCloseable {

    val images: DockerImages
    val containers: DockerContainers
}
