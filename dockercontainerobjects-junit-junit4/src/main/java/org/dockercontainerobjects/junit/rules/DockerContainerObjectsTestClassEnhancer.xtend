package org.dockercontainerobjects.junit.rules

import java.io.IOException
import org.dockercontainerobjects.ContainerObjectsClassEnhancer
import org.dockercontainerobjects.ContainerObjectsEnvironmentFactory
import org.eclipse.xtend.lib.annotations.Accessors
import org.junit.rules.TestRule
import org.junit.runners.model.Statement
import org.junit.runner.Description

class DockerContainerObjectsTestClassEnhancer implements TestRule {

    @Accessors(PUBLIC_GETTER)
    val extension ContainerObjectsClassEnhancer enhancer

    val boolean autoClose

    @Accessors(PUBLIC_GETTER)
    val Class<?> testClass

    new(ContainerObjectsClassEnhancer enhancer, boolean autoClose, Class<?> testClass) {
        this.enhancer = enhancer
        this.autoClose = autoClose
        this.testClass = testClass
    }

    new(Class<?> testClass) {
        this(ContainerObjectsEnvironmentFactory.instance.newDefaultEnvironment.enhancer, true, testClass)
    }

    override apply(Statement base, Description description) {
        [
            beforeAll
            try {
                base.evaluate
            } finally {
                afterAll
            }
        ]
    }

    private def void beforeAll() {
        testClass.setupClass
    }

    private def void afterAll() throws IOException {
        try {
            testClass.teardownClass
        } finally {
            if (autoClose) enhancer.close
        }
    }
}
