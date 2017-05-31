package org.dockercontainerobjects.junit.rules

import org.dockercontainerobjects.ContainerObjectsEnvironment
import org.dockercontainerobjects.ContainerObjectsManager
import org.junit.rules.ExternalResource
import java.util.function.Supplier

class ContainerObjectResource<T: Any>
        (val managerSupplier: Supplier<ContainerObjectsManager>, val containerType: Class<T>):
        ExternalResource() {

    lateinit var containerInstance: T
        private set

    constructor(environment: ContainerObjectsEnvironment, containerType: Class<T>):
            this(Supplier { environment.manager }, containerType)

    constructor(environment: ContainerObjectsEnvironmentResource, containerType: Class<T>):
            this(Supplier { -> environment.manager }, containerType)

    override fun before() {
        containerInstance = managerSupplier.get().create(containerType)
    }

    override fun after() {
        managerSupplier.get().destroy(containerInstance)
    }
}
