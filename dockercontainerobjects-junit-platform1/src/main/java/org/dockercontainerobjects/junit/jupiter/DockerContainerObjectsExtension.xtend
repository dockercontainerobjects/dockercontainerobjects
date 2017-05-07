package org.dockercontainerobjects.junit.jupiter

import org.junit.jupiter.api.^extension.AfterAllCallback
import org.junit.jupiter.api.^extension.AfterEachCallback
import org.junit.jupiter.api.^extension.BeforeAllCallback
import org.junit.jupiter.api.^extension.BeforeEachCallback
import org.junit.jupiter.api.^extension.ContainerExtensionContext
import org.junit.jupiter.api.^extension.TestExtensionContext
import org.dockercontainerobjects.ContainerObjectsEnvironmentFactory
import org.dockercontainerobjects.ContainerObjectsClassEnhancer

/**
 * JUnitPlatform Extension to instantiate Docker containers
 */
class DockerContainerObjectsExtension implements
        BeforeAllCallback, BeforeEachCallback, AfterEachCallback, AfterAllCallback {

    private var extension ContainerObjectsClassEnhancer enhancer

    override beforeAll(ContainerExtensionContext context) throws Exception {
        enhancer = ContainerObjectsEnvironmentFactory.instance.newEnvironment.enhancer
        context.testClass.ifPresent[ setupClass ]
    }

    override beforeEach(TestExtensionContext context) throws Exception {
        context.testInstance.setupInstance
    }

    override afterEach(TestExtensionContext context) throws Exception {
        context.testInstance.teardownInstance
    }

    override afterAll(ContainerExtensionContext context) throws Exception {
        context.testClass.ifPresent[ teardownClass ]
        enhancer.close
    }
}
