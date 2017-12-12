package org.dockercontainerobjects.docker

import java.time.Instant

interface ContainerInfo {

    val id: ContainerId
    val created: Instant
    val names: List<ContainerName>

    fun equals(other: ContainerInfo) =
            (id == other.id && created == other.created)

    val representation: String get() = "name: $id, created: $created, names: $names"
}
