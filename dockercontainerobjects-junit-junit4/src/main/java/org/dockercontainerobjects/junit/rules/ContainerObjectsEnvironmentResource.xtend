package org.dockercontainerobjects.junit.rules

import org.dockercontainerobjects.ContainerObjectsEnvironment
import org.dockercontainerobjects.ContainerObjectsEnvironmentFactory
import org.eclipse.xtend.lib.annotations.Accessors
import org.junit.rules.ExternalResource

class ContainerObjectsEnvironmentResource extends ExternalResource {

    @Accessors(PUBLIC_GETTER)
    var ContainerObjectsEnvironment environment

    override before() {
        environment = ContainerObjectsEnvironmentFactory.instance.newDefaultEnvironment
    }

    override after() {
        environment.close
    }

    def getManager() {
        environment.manager
    }
}
