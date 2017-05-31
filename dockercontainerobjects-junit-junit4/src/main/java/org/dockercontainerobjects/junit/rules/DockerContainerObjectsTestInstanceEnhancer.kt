package org.dockercontainerobjects.junit.rules

import org.dockercontainerobjects.ContainerObjectsClassEnhancer
import org.dockercontainerobjects.ContainerObjectsEnvironmentFactory
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class DockerContainerObjectsTestInstanceEnhancer
        (val enhancer: ContainerObjectsClassEnhancer, val autoClose: Boolean, val testInstance: Any):
        TestRule {

    constructor(testInstance: Any):
            this(ContainerObjectsEnvironmentFactory.newEnvironment().enhancer, true, testInstance)

    override fun apply(base: Statement, description: Description?): Statement {
        return object:Statement() {
            override fun evaluate() {
                beforeEach()
                try {
                    base.evaluate()
                } finally {
                    afterEach()
                }
            }
        }
    }

    private fun beforeEach() {
        enhancer.setupInstance(testInstance)
    }

    private fun afterEach() {
        try {
            enhancer.teardownInstance(testInstance)
        } finally {
            if (autoClose) enhancer.close()
        }
    }
}
