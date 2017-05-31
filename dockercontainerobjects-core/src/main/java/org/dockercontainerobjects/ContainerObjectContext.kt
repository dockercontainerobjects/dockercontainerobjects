package org.dockercontainerobjects

import com.github.dockerjava.api.model.NetworkSettings

interface ContainerObjectContext<T: Any> {

    val environment: ContainerObjectsEnvironment
    val type: Class<T>
    val stage: ContainerObjectLifecycleStage
    val phase: ContainerObjectLifecyclePhase
        get() = stage.phase
    val imageId: String?
    val containerId: String?
    val autoRemoveImage: Boolean?
    val networkSettings: NetworkSettings?
    val instance: T?
}
