package org.dockercontainerobjects

import static extension java.util.Arrays.stream
import static extension java.util.Optional.empty
import static extension java.util.stream.Collectors.toList
import static extension org.dockercontainerobjects.ContainerObjectLifecycleStage.*
import static extension org.dockercontainerobjects.docker.DockerClientExtensions.buildImage
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
import static extension org.dockercontainerobjects.util.CompressExtensions.buildTARGZ
import static extension org.dockercontainerobjects.util.CompressExtensions.withEntry
import static extension org.dockercontainerobjects.util.Loggers.debug
import static extension org.dockercontainerobjects.util.Members.onInstance
import static extension org.dockercontainerobjects.util.Methods.call
import static extension org.dockercontainerobjects.util.Methods.expectingNoParameters
import static extension org.dockercontainerobjects.util.Methods.invokeInstanceMethods
import static extension org.dockercontainerobjects.util.Methods.isOfReturnType
import static extension org.dockercontainerobjects.util.Methods.findMethods
import static extension org.dockercontainerobjects.util.Optionals.unsure
import static extension org.dockercontainerobjects.util.Predicates.operator_and
import static extension org.dockercontainerobjects.util.Strings.toSnakeCase
import static extension org.dockercontainerobjects.util.Strings.operator_tripleLessThan

import java.lang.^annotation.Annotation
import java.io.File
import java.io.InputStream
import java.io.IOException
import java.net.InetAddress
import java.net.URI
import java.net.URL
import java.util.ArrayList
import java.util.HashMap
import java.util.List
import java.util.Map
import java.util.UUID
import java.util.stream.Collectors
import com.github.dockerjava.api.exception.DockerException
import com.github.dockerjava.api.exception.NotFoundException
import com.github.dockerjava.api.model.NetworkSettings
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.dockercontainerobjects.annotations.AfterContainerCreated
import org.dockercontainerobjects.annotations.AfterContainerRemoved
import org.dockercontainerobjects.annotations.AfterContainerStarted
import org.dockercontainerobjects.annotations.AfterContainerStopped
import org.dockercontainerobjects.annotations.AfterImageBuilt
import org.dockercontainerobjects.annotations.AfterImagePrepared
import org.dockercontainerobjects.annotations.AfterImageReleased
import org.dockercontainerobjects.annotations.AfterImageRemoved
import org.dockercontainerobjects.annotations.BeforeBuildingImage
import org.dockercontainerobjects.annotations.BeforeCreatingContainer
import org.dockercontainerobjects.annotations.BeforeReleasingImage
import org.dockercontainerobjects.annotations.BeforePreparingImage
import org.dockercontainerobjects.annotations.BeforeRemovingContainer
import org.dockercontainerobjects.annotations.BeforeRemovingImage
import org.dockercontainerobjects.annotations.BeforeStartingContainer
import org.dockercontainerobjects.annotations.BeforeStoppingContainer
import org.dockercontainerobjects.annotations.BuildImage
import org.dockercontainerobjects.annotations.BuildImageContent
import org.dockercontainerobjects.annotations.BuildImageContentEntry
import org.dockercontainerobjects.annotations.Environment
import org.dockercontainerobjects.annotations.EnvironmentEntry
import org.dockercontainerobjects.annotations.RegistryImage
import org.eclipse.xtend.lib.annotations.Accessors
import org.slf4j.LoggerFactory

class ContainerObjectsManagerImpl implements ContainerObjectsManager {

    private static val IMAGE_TAG_DEFAULT_TEMPLATE = "%s_%s:latest"

    public static val SCHEME_PATH_SEPARATOR = "://"

    public static val SCHEME_CLASSPATH_PREFIX = SCHEME_CLASSPATH+SCHEME_PATH_SEPARATOR
    public static val SCHEME_FILE_PREFIX = SCHEME_FILE+SCHEME_PATH_SEPARATOR
    public static val SCHEME_HTTP_PREFIX = SCHEME_HTTP+SCHEME_PATH_SEPARATOR
    public static val SCHEME_HTTPS_PREFIX = SCHEME_HTTPS+SCHEME_PATH_SEPARATOR

    private static val l = LoggerFactory.getLogger(ContainerObjectsManagerImpl)

    private extension val ContainerObjectsEnvironment env

    new(ContainerObjectsEnvironment env) {
        this.env = env
    }

    override close() throws IOException {
        env.close
    }

    override <T> create(Class<T> containerType) {
        val ctx = new ContainerObjectContextImpl<T>(env, containerType)
        // instance creation stage
        ctx.createInstance
        // image preparation stage
        ctx.prepareImage
        // container creation stage
        ctx.createContainer
        // container started stage
        ctx.startContainer
        // register container
        registerContainerObject(ctx)

        ctx.instance
    }

    override <T> destroy(T containerInstance) {
        val ctx = containerInstance.containerObjectRegistration as ContainerObjectContextImpl<T>
        unregisterContainerObject(ctx)

        // container stopping stage
        ctx.stopContainer
        // container removing stage
        ctx.removeContainer
        // image release stage
        ctx.teardownImage
        // instance discarded stage
        ctx.discardInstance
    }

    override getContainerId(Object containerInstance) {
        containerInstance.containerObjectRegistration.containerId
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
        containerInstance.containerObjectRegistration.networkSettings
    }

    override <ADDR extends InetAddress> getContainerAddress(Object containerInstance, Class<ADDR> addrType) {
        containerInstance.containerNetworkSettings.inetAddressOfType(addrType)
    }

    private static def <T> void processLifecycleStage(ContainerObjectContextImpl<T> ctx, ContainerObjectLifecycleStage stage) {
        ctx.stage = stage
        ExtensionManager.instance.updateContainerObjectFields(ctx)
    }

    private static def <T> void createInstance(ContainerObjectContextImpl<T> ctx) {
        // create container instance
        ctx.instance = ctx.type.instantiate

        // initialize containers in instance fields
        ctx.environment.enhancer.setupInstance(ctx.instance)
        ctx.processLifecycleStage(INSTANCE_CREATED)
    }

    private static def <T> void prepareImage(ContainerObjectContextImpl<T> ctx) {
        val containerType = ctx.type
        val containerInstance = ctx.instance
        containerInstance.invokeContainerLifecycleListeners(BeforePreparingImage)
        l.debug [ "preparing image for container class '%s'" <<< containerType.simpleName ]
        // check for image from annotation or method
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
                "Container class '%s' has more than one way to define the image to use" <<< containerType.simpleName)
        var String imageId = null
        var boolean forcePull = false
        var boolean autoRemove = false
        var boolean imageBuilt = false

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
                        #[registryImageMethod, containerType.simpleName, String.simpleName])
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
        } else {
            containerInstance.invokeContainerLifecycleListeners(BeforeBuildingImage)
            var Object imageRef = null
            var String imageTag = null
            if (buildImageAnnotation.present) {
                val buildImageAnnotationValue = buildImageAnnotation.get
                imageRef = buildImageAnnotationValue.value
                if (imageRef === null)
                    throw new IllegalArgumentException(
                        "Annotation '%s' on class '%s' must define a non-empty value pointing to a Dockerfile" <<<
                            #[BuildImage.simpleName, containerType.simpleName])
                imageTag = buildImageAnnotationValue.tag
            } else if (!buildImageMethods.empty) {
                val buildImageMethod = buildImageMethods.get(0)
                #[String, URI, URL, File, InputStream].stream.filter [ buildImageMethod.isOfReturnType(it) ].findAny.orElseThrow [
                    new IllegalArgumentException(
                        "Method '%s' on class '%s' must return a valid type pointing to a Dockerfile" <<<
                            #[buildImageMethod, containerType.simpleName])
                ]
                imageRef = buildImageMethod.call(containerInstance)
                if (imageRef === null)
                    throw new IllegalArgumentException(
                        "Method '%s' on class '%s' must return a non-null value to be used to define the image to use" <<<
                            #[buildImageMethod, containerType.simpleName])
            }
            if (imageTag === null || imageTag.empty)
                imageTag = defaultImageTag(containerType)
            if (imageTag.contains(IMAGE_TAG_DYNAMIC_PLACEHOLDER))
                imageTag = imageTag.replace(IMAGE_TAG_DYNAMIC_PLACEHOLDER, UUID.randomUUID.toString)
            val Map<String, Object> content = new HashMap
            containerType.getAnnotationsByType(BuildImageContentEntry).stream.forEach [
                l.debug [ "Adding entry name '%s' with content '%s'" <<< #[name, value] ]
                content.put(name, value.normalize(containerType))
            ]
            containerType.findMethods(expectingNoParameters && annotatedWith(BuildImageContent))
                    .stream
                    .forEach [
                        if (!isOfReturnType(Map))
                            throw new IllegalArgumentException(
                                    "Method '%s' on class '%s' must return a Map to be used to define the image to use" <<<
                                            #[it, containerType.simpleName])
                        val newContent = call(containerInstance) as Map<String, Object>
                        if (newContent !== null) content.putAll(newContent)
                    ]

            val cleanImageTag = imageTag.toLowerCase
            l.debug [ "image for container class '%s' will be build and tagged as '%s'" <<< #[containerType.simpleName, cleanImageTag] ]
            val generatedImageId =
                if (imageRef instanceof File)
                    ctx.environment.dockerClient.buildImage(imageRef, cleanImageTag, forcePull)
                else {
                    val tar = buildDockerTAR(imageRef, content, containerType)
                    ctx.environment.dockerClient.buildImage(tar, cleanImageTag, forcePull)
                }
            l.debug [ "image for container class '%s' build with id '%s' and tagged as '%s'" <<< #[containerType.simpleName, generatedImageId, cleanImageTag] ]
            imageId = cleanImageTag
            autoRemove = true
            imageBuilt = true
            containerInstance.invokeContainerLifecycleListeners(AfterImageBuilt)
        }

        val imagePresent = imageBuilt ||
            try {
                imageId = ctx.environment.dockerClient.inspectImage(imageId).id
                true
            } catch (NotFoundException ex) {
                false
            }
        if (imagePresent && !imageBuilt) autoRemove = false
        if (forcePull || !imagePresent) {
            imageId = ctx.environment.dockerClient.pullImage(imageId).id
        }

        ctx.imageId = imageId
        ctx.autoRemoveImage = autoRemove
        ctx.processLifecycleStage(IMAGE_PREPARED)
        containerInstance.invokeContainerLifecycleListeners(AfterImagePrepared)
    }

    private static def <T> void createContainer(ContainerObjectContextImpl<T> ctx) {
        val containerInstance = ctx.instance
        containerInstance.invokeContainerLifecycleListeners(BeforeCreatingContainer)

        val environment = collectContainerEnvironmentVariables(containerInstance)
        val containerId = ctx.environment.dockerClient.createContainer(ctx.imageId, environment).id

        ctx.containerId = containerId
        ctx.processLifecycleStage(CONTAINER_CREATED)
        containerInstance.invokeContainerLifecycleListeners(AfterContainerCreated)
    }

    private static def collectContainerEnvironmentVariables(Object containerInstance) {
        val containerType = containerInstance.class

        val List<String> environment = new ArrayList
        // check for environment defined as class annotations
        environment.addAll(
                containerType.getAnnotationsByType(EnvironmentEntry)
                .stream.map [ if (name.empty) value else name+"="+value ].collect(toList))
        // check for environment defined as methods
        containerType.findMethods(expectingNoParameters && annotatedWith(Environment))
        .stream
        .forEach [
            if (!isOfReturnType(Map))
                throw new IllegalArgumentException(
                        "Method '%s' on class '%s' must return a Map to be used to specify environment variales" <<<
                                #[it, containerType.simpleName])
            val newEnvironment = call(containerInstance) as Map<String, String>
            if (newEnvironment !== null)
                environment.addAll(newEnvironment.entrySet.stream.map [ key+"="+value ].collect(toList))
        ]

        environment
    }

    private static def <T> void startContainer(ContainerObjectContextImpl<T> ctx) {
        val containerInstance = ctx.instance
        containerInstance.invokeContainerLifecycleListeners(BeforeStartingContainer)
        val response = ctx.environment.dockerClient.startContainer(ctx.containerId)

        ctx.networkSettings = response.networkSettings
        ctx.processLifecycleStage(CONTAINER_STARTED)
        containerInstance.invokeContainerLifecycleListeners(AfterContainerStarted)
    }

    private static def <T> void stopContainer(ContainerObjectContextImpl<T> ctx) {
        val containerInstance = ctx.instance
        containerInstance.invokeContainerLifecycleListeners(BeforeStoppingContainer)
        ctx.environment.dockerClient.stopContainer(ctx.containerId)

        ctx.processLifecycleStage(CONTAINER_STOPPED)
        containerInstance.invokeContainerLifecycleListeners(AfterContainerStopped)
     }

    private static def <T> void removeContainer(ContainerObjectContextImpl<T> ctx) {
        val containerInstance = ctx.instance
        containerInstance.invokeContainerLifecycleListeners(BeforeRemovingContainer)
        ctx.environment.dockerClient.removeContainer(ctx.containerId)

        ctx.processLifecycleStage(CONTAINER_REMOVED)
        containerInstance.invokeContainerLifecycleListeners(AfterContainerRemoved)
    }

    private static def <T> void teardownImage(ContainerObjectContextImpl<T> ctx) {
        val containerInstance = ctx.instance
        containerInstance.invokeContainerLifecycleListeners(BeforeReleasingImage)
        // try to remove image if requested
        if (ctx.autoRemoveImage)
            try {
                containerInstance.invokeContainerLifecycleListeners(BeforeRemovingImage)
                ctx.environment.dockerClient.removeImage(ctx.imageId)
                containerInstance.invokeContainerLifecycleListeners(AfterImageRemoved)
            } catch (NotFoundException e) {
                // if the image is already removed, log and ignore
                l.warn("Image '%s' requested to be auto-removed, but was not found" <<< ctx.imageId, e)
            } catch (DockerException e) {
                // TODO check if there is a more specific exception for this case
                // if the image is still in use, log and ignore
                l.warn("Image '%s' requested to be auto-removed, but seems to be still in use" <<< ctx.imageId, e)
            }

        ctx.processLifecycleStage(IMAGE_RELEASED)
        containerInstance.invokeContainerLifecycleListeners(AfterImageReleased)
    }

    private static def <T> void discardInstance(ContainerObjectContextImpl<T> ctx) {
        val containerInstance = ctx.instance
        ctx.environment.enhancer.teardownInstance(containerInstance)
        ctx.processLifecycleStage(INSTANCE_DISCARDED)
    }

    private static def void invokeContainerLifecycleListeners(Object containerInstance,
            Class<? extends Annotation> annotationType) {
        l.debug [ "Invoking life-cycle event '%s'" <<< annotationType.simpleName ]
        containerInstance.invokeInstanceMethods(
                onInstance && expectingNoParameters && annotatedWith(annotationType), empty)
    }

    private static def buildDockerTAR(Object dockerfile, Map<String, Object> content, Class<?> containerType) throws IOException {
        buildTARGZ [
            withGenericEntry(DOCKERFILE_DEFAULT_NAME, dockerfile.normalize(containerType))
            if (content !== null)
                content.forEach [k, v| withGenericEntry(k, v.normalize(containerType)) ]
        ]
    }

    private static def withGenericEntry(TarArchiveOutputStream out, String filename, Object content) throws IOException {
        l.debug [ "Adding TAR entry with name '%s' and content of type '%s'" <<< #[filename, content.class.simpleName] ]
        switch (content) {
            URL:
                out.withEntry(filename, content)
            URI:
                out.withEntry(filename, content)
            InputStream:
                out.withEntry(filename, content)
            byte[]:
                out.withEntry(filename, content)
            default:
                throw new IllegalArgumentException("Content is not of a supported type")
        }
    }

    private static def normalize(Object resource, Class<?> loader) {
        if (resource instanceof String) {
            if (resource.startsWith(SCHEME_CLASSPATH_PREFIX)) {
                val result = loader.getResource(resource.substring(SCHEME_CLASSPATH_PREFIX.length))
                if (result === null)
                    throw new IllegalArgumentException("Resource not found: "+resource)
                return result
            }
            if (resource.startsWith(SCHEME_FILE_PREFIX))
                return URI.create(resource.substring(SCHEME_FILE_PREFIX.length))
            if (resource.startsWith(SCHEME_HTTP_PREFIX))
                return URI.create(resource.substring(SCHEME_HTTP_PREFIX.length))
            if (resource.startsWith(SCHEME_HTTPS_PREFIX))
                return URI.create(resource.substring(SCHEME_HTTPS_PREFIX.length))
        }
        resource
    }

    private static def defaultImageTag(Class<?> containerType) {
        String.format(IMAGE_TAG_DEFAULT_TEMPLATE, containerType.simpleName.toSnakeCase, IMAGE_TAG_DYNAMIC_PLACEHOLDER)
    }

    @SuppressWarnings("MissingOverride")
    protected static class ContainerObjectContextImpl<T> implements ContainerObjectContext<T> {

        @Accessors(PUBLIC_GETTER) val ContainerObjectsEnvironment environment
        @Accessors(PUBLIC_GETTER) val Class<T> type
        @Accessors(#[PUBLIC_GETTER, PROTECTED_SETTER]) var T instance
        @Accessors(#[PUBLIC_GETTER, PROTECTED_SETTER]) var ContainerObjectLifecycleStage stage
        @Accessors(#[PUBLIC_GETTER, PROTECTED_SETTER]) var NetworkSettings networkSettings
        @Accessors(#[PUBLIC_GETTER, PROTECTED_SETTER]) var String imageId
        @Accessors(#[PUBLIC_GETTER, PROTECTED_SETTER]) var boolean autoRemoveImage
        @Accessors(#[PUBLIC_GETTER, PROTECTED_SETTER]) var String containerId

        new(ContainerObjectsEnvironment environment, Class<T> type) {
            this.environment = environment
            this.type = type
        }
    }
}
