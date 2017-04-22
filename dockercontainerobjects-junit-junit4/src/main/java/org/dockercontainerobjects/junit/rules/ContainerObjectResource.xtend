package org.dockercontainerobjects.junit.rules

import java.util.function.Supplier
import org.dockercontainerobjects.ContainerObjectsManager
import org.eclipse.xtend.lib.annotations.Accessors
import org.junit.rules.ExternalResource

class ContainerObjectResource<T> extends ExternalResource {

    @Accessors(PUBLIC_GETTER)
    val Supplier<ContainerObjectsManager> managerSupplier

    @Accessors(PUBLIC_GETTER)
    val Class<T> containerType

    @Accessors(PUBLIC_GETTER)
    var T containerInstance

    new(Supplier<ContainerObjectsManager> managerSupplier, Class<T> containerType) {
        this.managerSupplier = managerSupplier
        this.containerType = containerType
    }

    new(ContainerObjectsEnvironmentResource environment, Class<T> containerType) {
        this([ environment.manager ], containerType)
    }

    override before() {
        containerInstance = managerSupplier.get.create(containerType)
    }

    override after() {
        managerSupplier.get.destroy(containerInstance)
    }
}
