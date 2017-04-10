package org.dockercontainerobjects

import static extension java.util.Optional.empty
import static extension org.dockercontainerobjects.docker.DockerClientExtensions.createContainer
import static extension org.dockercontainerobjects.docker.DockerClientExtensions.inetAddressOfType
import static extension org.dockercontainerobjects.docker.DockerClientExtensions.inspectContainer
import static extension org.dockercontainerobjects.docker.DockerClientExtensions.startContainer
import static extension org.dockercontainerobjects.docker.DockerClientExtensions.stopContainer
import static extension org.dockercontainerobjects.docker.DockerClientExtensions.removeContainer
import static extension org.dockercontainerobjects.util.AccessibleObjects.annotatedWith
import static extension org.dockercontainerobjects.util.AccessibleObjects.instantiate
import static extension org.dockercontainerobjects.util.Fields.ofType
import static extension org.dockercontainerobjects.util.Fields.updateFields
import static extension org.dockercontainerobjects.util.Functions.constant
import static extension org.dockercontainerobjects.util.Functions.nil
import static extension org.dockercontainerobjects.util.Loggers.debug
import static extension org.dockercontainerobjects.util.Members.onInstance
import static extension org.dockercontainerobjects.util.Methods.call
import static extension org.dockercontainerobjects.util.Methods.expectingNoParameters
import static extension org.dockercontainerobjects.util.Methods.invokeInstanceMethods
import static extension org.dockercontainerobjects.util.Methods.isExpectingNoParameters
import static extension org.dockercontainerobjects.util.Methods.isOfReturnType
import static extension org.dockercontainerobjects.util.Methods.findMethods
import static extension org.dockercontainerobjects.util.Optionals.unsure
import static extension org.dockercontainerobjects.util.Predicates.operator_and
import static extension org.dockercontainerobjects.util.Strings.operator_tripleLessThan

import java.lang.^annotation.Annotation
import java.io.IOException
import java.net.InetAddress
import java.util.stream.Collectors
import javax.inject.Inject
import org.dockercontainerobjects.ContainerObjectsEnvironment.ImageRegistrationInfo
import org.dockercontainerobjects.ContainerObjectsEnvironment.ContainerRegistrationInfo
import org.dockercontainerobjects.ContainerObjectsEnvironment.RegistrationInfo
import org.dockercontainerobjects.annotations.AfterCreated
import org.dockercontainerobjects.annotations.AfterStarted
import org.dockercontainerobjects.annotations.AfterStopped
import org.dockercontainerobjects.annotations.AfterRemoved
import org.dockercontainerobjects.annotations.BeforeCreating
import org.dockercontainerobjects.annotations.BeforeStarting
import org.dockercontainerobjects.annotations.BeforeStopping
import org.dockercontainerobjects.annotations.BeforeRemoving
import org.dockercontainerobjects.annotations.ContainerAddress
import org.dockercontainerobjects.annotations.ContainerId
import org.dockercontainerobjects.annotations.RegistryImage
import org.slf4j.LoggerFactory
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.NetworkSettings

class ContainerObjectsManagerImpl implements ContainerObjectsManager {

    private static val l = LoggerFactory.getLogger(ContainerObjectsManagerImpl)

    private extension val ContainerObjectsEnvironment env

    new(ContainerObjectsEnvironment env) {
        this.env = env
    }

    override close() throws IOException {
        env.close
    }

    override <T> create(Class<T> containerType) {
        // create container instance
        val containerInstance = containerType.instantiate

        // initialize containers in instance fields
        enhancer.setupInstance(containerInstance)

        // injecting non-container attributes
        containerInstance.updateFields(onInstance && ofType(DockerClient) && annotatedWith(Inject), constant(env.dockerClient))
        containerInstance.updateFields(onInstance && ofType(ContainerObjectsManager) && annotatedWith(Inject), constant(this))

        // find or prepare the container image
        val imageInfo = containerInstance.prepareImage

        // create container
        val containerInfo = containerInstance.createContainer(imageInfo)

        // register container
        val info = new RegistrationInfo(imageInfo, containerInfo)
        registerContainer(info)

        // start container
        containerInstance.startContainer(containerInfo)

        containerInstance
    }

    override destroy(Object containerInstance) {
        val info = containerInstance.registrationInfo

        // stop container
        containerInstance.stopContainer(info.container)

        // remove container
        containerInstance.removeContainer(info.container)

        // optional image disposal
        containerInstance.teardownImage(info.image)

        // destroy container instance
        unregisterContainer(containerInstance)

        // cleanup non-container attributes
        containerInstance.updateFields(onInstance && ofType(DockerClient) && annotatedWith(Inject), nil)
        containerInstance.updateFields(onInstance && ofType(ContainerObjectsManager) && annotatedWith(Inject), nil)

        // destroy containers in instance fields
        enhancer.teardownInstance(containerInstance)
    }

    override getContainerId(Object containerInstance) {
        val info = containerInstance.registrationInfo
        if (info !== null) info.container.id
    }

    override getContainerStatus(Object containerInstance) {
        val containerId = containerInstance.containerId
        if (containerId === null) return ContainerStatus.UNKNOWN
        val state = dockerClient.inspectContainer(containerId).state
        switch (state.status) {
            case "running": ContainerStatus.STARTED
            case "created": ContainerStatus.CREATED
            case "exited": ContainerStatus.STOPPED
            default: ContainerStatus.UNKNOWN
        }
    }

    override getContainerNetworkSettings(Object containerInstance) {
        dockerClient.inspectContainer(containerInstance.containerId).networkSettings
    }

    override <ADDR extends InetAddress> getContainerAddress(Object containerInstance, Class<ADDR> addrType) {
        containerInstance.containerNetworkSettings.inetAddressOfType(addrType)
    }

    private def prepareImage(Object containerInstance) {
        val containerType = containerInstance.class
        // check for image from annotation or method
        // TODO check for build from annotation or method
        val imageAnnotation = containerType.getAnnotation(RegistryImage).unsure
        val imageMethods = containerType.findMethods(onInstance && expectingNoParameters && annotatedWith(RegistryImage))
            .stream.collect(Collectors.toList)
        if ((!imageAnnotation.present && imageMethods.empty) ||
                (imageAnnotation.present && !imageMethods.empty) ||
                (imageMethods.size > 1))
            throw new IllegalStateException(
                    "Container class '%s' must be annotated with '%s' annotation " +
                            "or must have one method annotated with '%s' annotation" <<<
                            #[containerType, RegistryImage.simpleName, RegistryImage.simpleName])
        val imageMethod = imageMethods.stream.findAny
        val imageId = imageMethod.map [
            if (!isOfReturnType(String) || !isExpectingNoParameters)
                throw new IllegalStateException(
                        "Method '%s' in container class '%s' must have no arguments
                        and return '%s'" <<< #[it, containerType, String.simpleName])
            call(containerInstance) as String
        ].orElseGet [
            imageAnnotation.get.value
        ]

        new ImageRegistrationInfo(imageId, false)
    }

    private def createContainer(Object containerInstance, ImageRegistrationInfo imageInfo) {
        containerInstance.invokeContainerLifecycleListeners(BeforeCreating)
        val containerId = dockerClient.createContainer(imageInfo.id).id
        containerInstance.updateFields(
            onInstance && ofType(String) && annotatedWith(Inject, ContainerId), constant(containerId))
        containerInstance.invokeContainerLifecycleListeners(AfterCreated)

        new ContainerRegistrationInfo(containerId, containerInstance)
    }

    private def startContainer(Object containerInstance, ContainerRegistrationInfo containerInfo) {
        containerInstance.invokeContainerLifecycleListeners(BeforeStarting)
        val response = dockerClient.startContainer(containerInfo.id)
        containerInstance.updateFields(
            onInstance && ofType(NetworkSettings) && annotatedWith(Inject), constant(response.networkSettings))
        containerInstance.updateFields(onInstance && annotatedWith(Inject, ContainerAddress)) [ type|
            response.networkSettings.inetAddressOfType(type)
        ]
        containerInstance.invokeContainerLifecycleListeners(AfterStarted)
    }

    private def stopContainer(Object containerInstance, ContainerRegistrationInfo containerInfo) {
        containerInstance.invokeContainerLifecycleListeners(BeforeStopping)
        dockerClient.stopContainer(containerInfo.id)
        containerInstance.invokeContainerLifecycleListeners(AfterStopped)
        containerInstance.updateFields(onInstance && ofType(NetworkSettings) && annotatedWith(Inject), nil)
        containerInstance.updateFields(onInstance && annotatedWith(Inject, ContainerAddress), nil)
    }

    private def removeContainer(Object containerInstance, ContainerRegistrationInfo containerInfo) {
        containerInstance.invokeContainerLifecycleListeners(BeforeRemoving)
        dockerClient.removeContainer(containerInfo.id)
        containerInstance.invokeContainerLifecycleListeners(AfterRemoved)
        containerInstance.updateFields(onInstance && ofType(String) && annotatedWith(Inject, ContainerId), nil)
    }

    private def teardownImage(Object containerInstance, ImageRegistrationInfo imageInfo) {
    }

    private static def void invokeContainerLifecycleListeners(Object containerInstance,
            Class<? extends Annotation> annotationType) {
        l.debug [ "Invoking life-cycle event '%s'" <<< annotationType.simpleName ]
        containerInstance.invokeInstanceMethods(
                onInstance && expectingNoParameters && annotatedWith(annotationType), empty)
    }
}
