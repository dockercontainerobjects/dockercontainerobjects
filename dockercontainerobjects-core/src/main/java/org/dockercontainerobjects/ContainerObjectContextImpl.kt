package org.dockercontainerobjects

import com.github.dockerjava.api.model.NetworkSettings

internal class ContainerObjectContextImpl<T: Any>(
        override val environment: ContainerObjectsEnvironment, override val type: Class<T>):
        ContainerObjectContext<T> {

    override var stage: ContainerObjectLifecycleStage = ContainerObjectLifecycleStage.INSTANCE_CREATED
        internal set(value) {
            field = value
            ExtensionManager.updateContainerObjectFields(this)
        }
    override var imageId: String? = null
        internal set
    override var containerId: String? = null
        internal set
    override var autoRemoveImage: Boolean? = null
        internal set
    override var networkSettings: NetworkSettings? = null
        internal set
    override var instance: T? = null
        internal set
}
