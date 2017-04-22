package org.dockercontainerobjects.junit.rules

import java.io.IOException
import org.dockercontainerobjects.ContainerObjectsClassEnhancer
import org.dockercontainerobjects.ContainerObjectsEnvironmentFactory
import org.eclipse.xtend.lib.annotations.Accessors
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class DockerContainerObjectsTestInstanceEnhancer implements TestRule {

    @Accessors(PUBLIC_GETTER)
    val extension ContainerObjectsClassEnhancer enhancer

    val boolean autoClose

    @Accessors(PUBLIC_GETTER)
    val Object testInstance

    new(ContainerObjectsClassEnhancer enhancer, boolean autoClose, Object testInstance) {
        this.enhancer = enhancer
        this.autoClose = autoClose
        this.testInstance = testInstance
    }

    new(Class<?> testClass) {
        this(ContainerObjectsEnvironmentFactory.instance.newDefaultEnvironment.enhancer, true, testClass)
    }

    override apply(Statement base, Description description) {
        [
            beforeEach
            try {
                base.evaluate
            } finally {
                afterEach
            }
        ]
    }

    private def void beforeEach() {
        testInstance.setupInstance
    }

    private def void afterEach() throws IOException {
        try {
            testInstance.teardownInstance
        } finally {
            if (autoClose) enhancer.close
        }
    }
}
