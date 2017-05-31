package org.dockercontainerobjects.junit.rules

import org.dockercontainerobjects.ContainerObjectsEnvironment
import org.dockercontainerobjects.ContainerObjectsEnvironmentFactory
import org.junit.rules.ExternalResource

class ContainerObjectsEnvironmentResource: ExternalResource() {

    lateinit var environment: ContainerObjectsEnvironment
        private set

    val manager get() = environment.manager

    override fun before() {
        environment = ContainerObjectsEnvironmentFactory.newEnvironment()
    }

    override fun after() {
        environment.close()
    }
}
