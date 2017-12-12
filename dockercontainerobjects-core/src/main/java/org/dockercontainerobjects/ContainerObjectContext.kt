package org.dockercontainerobjects

import org.dockercontainerobjects.docker.ContainerLocator
import org.dockercontainerobjects.docker.ImageLocator
import org.dockercontainerobjects.docker.NetworkSettings

interface ContainerObjectContext<T: Any> {

    val environment: ContainerObjectsEnvironment
    val type: Class<T>
    val stage: ContainerObjectLifecycleStage
    val phase: ContainerObjectLifecyclePhase
        get() = stage.phase
    val image: ImageLocator?
    val container: ContainerLocator?
    val autoRemoveImage: Boolean?
    val networkSettings: NetworkSettings?
    val instance: T?
}
