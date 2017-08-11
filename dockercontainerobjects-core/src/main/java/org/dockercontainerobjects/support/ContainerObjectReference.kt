package org.dockercontainerobjects.support

import org.dockercontainerobjects.ContainerObjectsEnvironment
import org.dockercontainerobjects.ContainerObjectsManager
import kotlin.reflect.KClass

class ContainerObjectReference<T: Any>(val manager: ContainerObjectsManager, val instance: T): AutoCloseable {

    constructor(manager: ContainerObjectsManager, containerType: Class<T>):
            this(manager, manager.create(containerType))

    constructor(manager: ContainerObjectsManager, containerType: KClass<T>):
            this(manager, containerType.java)

    companion object {

        @JvmStatic
        fun <T: Any> newReference(env: ContainerObjectsEnvironment, containerType: Class<T>) =
                ContainerObjectReference(env.manager, containerType)

        @JvmStatic
        fun <T: Any> newReference(env: ContainerObjectsEnvironment, containerType: KClass<T>) =
                ContainerObjectReference(env.manager, containerType)

        inline fun <reified T: Any> newReference(env: ContainerObjectsEnvironment) =
                newReference(env, T::class)
    }

    val id
        get() = manager.getContainerId(instance)

    val address
        get() = manager.getContainerAddress(instance)

    override fun close() {
        manager.destroy(instance)
    }

    fun restart() {
        manager.restart(instance)
    }
}
