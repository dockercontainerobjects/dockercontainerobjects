package org.dockercontainerobjects.junit.rules

import org.dockercontainerobjects.ContainerObjectsClassEnhancer
import org.dockercontainerobjects.ContainerObjectsEnvironmentFactory
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class DockerContainerObjectsTestClassEnhancer
        (val enhancer: ContainerObjectsClassEnhancer, val autoClose: Boolean, val testClass: Class<*>):
        TestRule {

    constructor(testClass: Class<*>):
            this(ContainerObjectsEnvironmentFactory.newEnvironment().enhancer, true, testClass)

    override fun apply(base: Statement, description: Description?): Statement {
        return object: Statement() {
            override fun evaluate() {
                beforeAll()
                try {
                    base.evaluate()
                } finally {
                    afterAll()
                }
            }
        }
    }

    private fun beforeAll() {
        enhancer.setupClass(testClass)
    }

    private fun afterAll() {
        try {
            enhancer.teardownClass(testClass)
        } finally {
            if (autoClose) enhancer.close()
        }
    }
}
