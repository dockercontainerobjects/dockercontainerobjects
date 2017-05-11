package org.dockercontainerobjects

import com.github.dockerjava.api.model.NetworkSettings

interface ContainerObjectContext<T> {

    def ContainerObjectsEnvironment getEnvironment()
    def Class<T> getType()
    def ContainerObjectLifecycleStage getStage()
    def ContainerObjectLifecyclePhase getPhase() { stage.phase }
    def String getImageId()
    def String getContainerId()
    def NetworkSettings getNetworkSettings()
    def T getInstance()
}
