package org.dockercontainerobjects

import org.dockercontainerobjects.ContainerObjectLifecyclePhase.CONTAINER_OBJECT_CREATION
import org.dockercontainerobjects.ContainerObjectLifecyclePhase.CONTAINER_OBJECT_DESTRUCTION

enum class ContainerObjectLifecycleStage(val phase: ContainerObjectLifecyclePhase) {

    /** Container object instance just created, not interaction with Docker yet  */
    INSTANCE_CREATED(CONTAINER_OBJECT_CREATION),
    /** Docker image selected, created if necesary  */
    IMAGE_PREPARED(CONTAINER_OBJECT_CREATION),
    /** Docker container created  */
    CONTAINER_CREATED(CONTAINER_OBJECT_CREATION),
    /** Docker container started  */
    CONTAINER_STARTED(CONTAINER_OBJECT_CREATION),
    /** Docker container explicitly stopped  */
    CONTAINER_STOPPED(CONTAINER_OBJECT_DESTRUCTION),
    /** Docker container removed  */
    CONTAINER_REMOVED(CONTAINER_OBJECT_DESTRUCTION),
    /** Docker image released, removed if requested  */
    IMAGE_RELEASED(CONTAINER_OBJECT_DESTRUCTION),
    /** Container instance no longer tied to Docker, disconnected from its environment  */
    INSTANCE_DISCARDED(CONTAINER_OBJECT_DESTRUCTION)
}
