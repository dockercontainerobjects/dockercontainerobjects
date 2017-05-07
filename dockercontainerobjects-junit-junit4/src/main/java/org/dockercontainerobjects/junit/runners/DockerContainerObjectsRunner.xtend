package org.dockercontainerobjects.junit.runners

import org.dockercontainerobjects.ContainerObjectsClassEnhancer
import org.dockercontainerobjects.ContainerObjectsEnvironmentFactory
import org.dockercontainerobjects.junit.rules.DockerContainerObjectsTestClassEnhancer
import org.dockercontainerobjects.junit.rules.DockerContainerObjectsTestInstanceEnhancer
import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.runners.model.InitializationError

class DockerContainerObjectsRunner extends BlockJUnit4ClassRunner {

    val ContainerObjectsClassEnhancer enhancer

    new(Class<?> type) throws InitializationError {
        super(type)
        enhancer = ContainerObjectsEnvironmentFactory.instance.newEnvironment.enhancer
    }

    override classRules() {
        val rules = super.classRules
        rules += new DockerContainerObjectsTestClassEnhancer(enhancer, true, testClass.javaClass)
        rules
    }

    override getTestRules(Object target) {
        val rules = super.getTestRules(target)
        rules += new DockerContainerObjectsTestInstanceEnhancer(enhancer, false, target)
        rules
    }
}
