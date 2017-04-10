package org.dockercontainerobjects

import org.eclipse.xtend.lib.annotations.Accessors
import org.eclipse.xtend.lib.annotations.AccessorType
import com.github.dockerjava.api.DockerClient

class ContainerObjectsEnvironmentFactory {

    @Accessors(AccessorType.PUBLIC_GETTER)
    static val instance = new ContainerObjectsEnvironmentFactory

    def newEnvironment(DockerClient dockerClient) {
        new ContainerObjectsEnvironment(dockerClient)
    }

    def newDefaultEnvironment() {
        new ContainerObjectsEnvironment()
    }
}
