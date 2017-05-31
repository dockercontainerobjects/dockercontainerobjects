package org.dockercontainerobjects.junit.jupiter

import org.dockercontainerobjects.ContainerObjectsClassEnhancer
import org.dockercontainerobjects.ContainerObjectsEnvironmentFactory
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ContainerExtensionContext
import org.junit.jupiter.api.extension.TestExtensionContext

class DockerContainerObjectsExtension: BeforeAllCallback, BeforeEachCallback, AfterEachCallback, AfterAllCallback {

    lateinit var enhancer: ContainerObjectsClassEnhancer
        private set

    override fun beforeAll(context: ContainerExtensionContext) {
        enhancer = ContainerObjectsEnvironmentFactory.newEnvironment().enhancer
        context.testClass.ifPresent(enhancer::setupClass)
    }

    override fun beforeEach(context: TestExtensionContext) {
        enhancer.setupInstance(context.testInstance)
    }

    override fun afterEach(context: TestExtensionContext) {
        enhancer.teardownInstance(context.testInstance)
    }

    override fun afterAll(context: ContainerExtensionContext) {
        context.testClass.ifPresent(enhancer::teardownClass)
        enhancer.close()
    }
}

