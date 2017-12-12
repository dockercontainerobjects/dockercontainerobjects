package org.dockercontainerobjects.docker

sealed class ContainerLocator

class ContainerId(val id: String): ContainerLocator() {

    override fun toString() = id

    override fun hashCode() = id.hashCode()

    fun equals(other: ContainerId) = (id == other.id)

    override fun equals(other: Any?) =
            other != null && other is ContainerId && equals(other)

    fun matches(other: ContainerId) = other.id.startsWith(id)
}

class ContainerName(val name: String): ContainerLocator() {

    override fun toString() = name

    override fun hashCode() = name.hashCode()

    fun equals(other: ContainerName) = (name == other.name)

    override fun equals(other: Any?) =
            other != null && other is ContainerName && equals(other)
}
