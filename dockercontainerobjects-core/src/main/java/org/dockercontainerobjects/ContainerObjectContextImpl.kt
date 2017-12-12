package org.dockercontainerobjects

import org.dockercontainerobjects.docker.ContainerLocator
import org.dockercontainerobjects.docker.ImageLocator
import org.dockercontainerobjects.docker.NetworkSettings

internal class ContainerObjectContextImpl<T: Any>(
        override val environment: ContainerObjectsEnvironment, override val type: Class<T>):
        ContainerObjectContext<T> {

    override var stage: ContainerObjectLifecycleStage = ContainerObjectLifecycleStage.INSTANCE_CREATED
        internal set(value) {
            field = value
            ExtensionManager.updateContainerObjectFields(this)
        }
    override var image: ImageLocator? = null
        internal set
    override var container: ContainerLocator? = null
        internal set
    override var autoRemoveImage: Boolean? = null
        internal set
    override var networkSettings: NetworkSettings? = null
        internal set
    override var instance: T? = null
        internal set
}
