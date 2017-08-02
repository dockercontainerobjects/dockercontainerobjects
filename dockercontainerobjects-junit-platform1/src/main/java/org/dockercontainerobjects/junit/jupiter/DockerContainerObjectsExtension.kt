package org.dockercontainerobjects.junit.jupiter

import org.dockercontainerobjects.ContainerObjectsClassEnhancer
import org.dockercontainerobjects.ContainerObjectsEnvironmentFactory
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class DockerContainerObjectsExtension: BeforeAllCallback, BeforeEachCallback, AfterEachCallback, AfterAllCallback {

    lateinit var enhancer: ContainerObjectsClassEnhancer
        private set

    override fun beforeAll(context: ExtensionContext) {
        enhancer = ContainerObjectsEnvironmentFactory.newEnvironment().enhancer
        enhancer.setupClass(context.requiredTestClass)
    }

    override fun beforeEach(context: ExtensionContext) {
        enhancer.setupInstance(context.requiredTestInstance)
    }

    override fun afterEach(context: ExtensionContext) {
        enhancer.teardownInstance(context.requiredTestInstance)
    }

    override fun afterAll(context: ExtensionContext) {
        enhancer.teardownClass(context.requiredTestClass)
        enhancer.close()
    }
}

