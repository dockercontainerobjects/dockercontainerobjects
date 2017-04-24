package org.dockercontainerobjects

import static extension java.util.Optional.empty
import static extension java.util.stream.Collectors.toList
import static extension org.dockercontainerobjects.docker.DockerClientExtensions.createContainer
import static extension org.dockercontainerobjects.docker.DockerClientExtensions.inetAddressOfType
import static extension org.dockercontainerobjects.docker.DockerClientExtensions.inspectContainer
import static extension org.dockercontainerobjects.docker.DockerClientExtensions.inspectImage
import static extension org.dockercontainerobjects.docker.DockerClientExtensions.pullImage
import static extension org.dockercontainerobjects.docker.DockerClientExtensions.startContainer
import static extension org.dockercontainerobjects.docker.DockerClientExtensions.stopContainer
import static extension org.dockercontainerobjects.docker.DockerClientExtensions.removeContainer
import static extension org.dockercontainerobjects.docker.DockerClientExtensions.removeImage
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
import org.dockercontainerobjects.annotations.BuildImage
import org.dockercontainerobjects.annotations.ContainerAddress
import org.dockercontainerobjects.annotations.ContainerId
import org.dockercontainerobjects.annotations.RegistryImage
import org.slf4j.LoggerFactory
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.exception.NotFoundException
import com.github.dockerjava.api.model.NetworkSettings
import com.github.dockerjava.api.exception.DockerException
import org.dockercontainerobjects.annotations.Environment

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
        val registryImageAnnotation = containerType.getAnnotation(RegistryImage).unsure
        val registryImageMethods = containerType.findMethods(expectingNoParameters && annotatedWith(RegistryImage))
                .stream
                .collect(Collectors.toList)
        val buildImageAnnotation = containerType.getAnnotation(BuildImage).unsure
        val buildImageMethods = containerType.findMethods(expectingNoParameters && annotatedWith(BuildImage))
                .stream
                .collect(Collectors.toList)
        var options = 0
        if (registryImageAnnotation.present) options++
        options += registryImageMethods.size
        if (buildImageAnnotation.present) options++
        options += buildImageMethods.size
        if (options != 1)
            throw new IllegalArgumentException(
                "Container class '%s' has more than one way to define the image to use" <<< containerType.simpleName);
        var String imageId = null
        var boolean forcePull = false
        var boolean autoRemove = false

        if (registryImageAnnotation.present) {
            val registryImageAnnotationValue = registryImageAnnotation.get
            imageId = registryImageAnnotationValue.value
            if (imageId === null || imageId.empty)
                throw new IllegalArgumentException(
                    "Annotation '%s' on class '%s' must define a value to be used to define the image to use" <<<
                        #[RegistryImage.simpleName, containerType.simpleName])
            forcePull = registryImageAnnotationValue.forcePull
            autoRemove = registryImageAnnotationValue.autoRemove
        } else if (!registryImageMethods.empty) {
            val registryImageMethod = registryImageMethods.get(0)
            if (!registryImageMethod.isOfReturnType(String))
                throw new IllegalArgumentException(
                    "Method '%s' on class '%s' must return '%s' to be used to define the image to use" <<<
                        #[registryImageMethod, containerType.simpleName, String.simpleName]);
            val registryImageAnnotationValue = registryImageMethod.getAnnotation(RegistryImage)
            imageId = registryImageAnnotationValue.value
            if (imageId !== null && !imageId.empty)
                throw new IllegalArgumentException(
                    "Annotation '%s' on method '%s' on class '%s' cannot define a value to be used to define the image to use" <<<
                        #[RegistryImage.simpleName, registryImageMethod, containerType.simpleName])
            imageId = registryImageMethod.call(containerInstance) as String
            if (imageId === null || imageId.empty)
                throw new IllegalArgumentException(
                    "Method '%s' on class '%s' must return a non-null value to be used to define the image to use" <<<
                        #[registryImageMethod, containerType.simpleName])
            forcePull = registryImageAnnotationValue.forcePull
            autoRemove = registryImageAnnotationValue.autoRemove
        }

        val imagePresent =
            try {
                imageId = dockerClient.inspectImage(imageId).id
                true
            } catch (NotFoundException ex) {
                false
            }
        if (imagePresent) autoRemove = false
        if (forcePull || !imagePresent) {
            imageId = dockerClient.pullImage(imageId).id
        }

        new ImageRegistrationInfo(imageId, false, autoRemove)
    }

    private def createContainer(Object containerInstance, ImageRegistrationInfo imageInfo) {
        val containerType = containerInstance.class
        // check for environment
        val environment = containerType.getAnnotation(Environment)
                .unsure
                .map([ value.stream.map [ if (key.empty) value else key+"="+value ].collect(toList) ])

        containerInstance.invokeContainerLifecycleListeners(BeforeCreating)
        val containerId = dockerClient.createContainer(imageInfo.id, environment).id
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
        // try to remove image if requested
        if (imageInfo.autoRemove)
            try {
                dockerClient.removeImage(imageInfo.id)
            } catch (NotFoundException e) {
                // if the image is already removed, log and ignore
                l.warn("Image '%s' requested to be auto-removed, but was not found" <<< #[imageInfo.id], e)
            } catch (DockerException e) {
                // TODO check if there is a more specific exception for this case
                // if the image is still in use, log and ignore
                l.warn("Image '%s' requested to be auto-removed, but seems to be still in use" <<< #[imageInfo.id], e)
            }
    }

    private static def void invokeContainerLifecycleListeners(Object containerInstance,
            Class<? extends Annotation> annotationType) {
        l.debug [ "Invoking life-cycle event '%s'" <<< annotationType.simpleName ]
        containerInstance.invokeInstanceMethods(
                onInstance && expectingNoParameters && annotatedWith(annotationType), empty)
    }
}
