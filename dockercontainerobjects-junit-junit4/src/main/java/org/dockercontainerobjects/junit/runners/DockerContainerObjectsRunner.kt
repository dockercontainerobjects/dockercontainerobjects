package org.dockercontainerobjects.junit.runners

import org.dockercontainerobjects.ContainerObjectsEnvironmentFactory
import org.dockercontainerobjects.junit.rules.DockerContainerObjectsTestClassEnhancer
import org.dockercontainerobjects.junit.rules.DockerContainerObjectsTestInstanceEnhancer
import org.junit.rules.TestRule
import org.junit.runners.BlockJUnit4ClassRunner

class DockerContainerObjectsRunner(testClass: Class<*>) : BlockJUnit4ClassRunner(testClass) {

    val enhancer = ContainerObjectsEnvironmentFactory.newEnvironment().enhancer

    override fun classRules(): MutableList<TestRule> {
        val rules = super.classRules()
        rules += DockerContainerObjectsTestClassEnhancer(enhancer, true, testClass.javaClass)
        return rules
    }

    override fun getTestRules(target: Any): MutableList<TestRule> {
        val rules = super.getTestRules(target)
        rules += DockerContainerObjectsTestInstanceEnhancer(enhancer, false, target)
        return rules
    }
}

