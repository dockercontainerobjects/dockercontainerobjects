package org.dockercontainerobjects

interface ContainerObjectsClassEnhancer: AutoCloseable {

    fun setupClass(type: Class<*>)
    fun setupInstance(instance: Any)

    fun teardownClass(type: Class<*>)
    fun teardownInstance(instance: Any)
}
