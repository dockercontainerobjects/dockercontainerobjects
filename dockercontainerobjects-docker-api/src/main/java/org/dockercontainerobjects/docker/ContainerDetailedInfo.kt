package org.dockercontainerobjects.docker

interface ContainerDetailedInfo : ContainerInfo {

    val status: ContainerStatus
    val environment: Map<String, String>
    val labels: Map<String, String>
    val network: NetworkSettings

    fun equals(other: ContainerDetailedInfo) =
            equals(other as ContainerInfo)

    override val representation: String get() = "${super.representation}, status: $status"
}
