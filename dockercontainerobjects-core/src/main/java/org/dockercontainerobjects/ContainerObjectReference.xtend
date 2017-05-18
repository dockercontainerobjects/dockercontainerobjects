package org.dockercontainerobjects

class ContainerObjectReference<T> implements AutoCloseable {

    val ContainerObjectsManager manager
    val T containerInstance

    new(ContainerObjectsManager manager, T containerInstance) {
        this.manager = manager;
        this.containerInstance = containerInstance;
    }

    new(ContainerObjectsManager manager, Class<T> containerType) {
        this(manager, manager.create(containerType));
    }

    static def <T> newReference(ContainerObjectsEnvironment env, Class<T> containerType) {
        new ContainerObjectReference(env.manager, containerType)
    }

    def T getInstance() {
        containerInstance
    }

    def getId() {
        manager.getContainerId(containerInstance)
    }

    def getAddress() {
        manager.getContainerAddress(containerInstance)
    }

    override close() {
        manager.destroy(containerInstance)
    }
}