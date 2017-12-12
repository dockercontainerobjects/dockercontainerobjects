package org.dockercontainerobjects

import org.dockercontainerobjects.ContainerObjectLifecycleStage.CONTAINER_CREATED
import org.dockercontainerobjects.ContainerObjectLifecycleStage.CONTAINER_REMOVED
import org.dockercontainerobjects.ContainerObjectLifecycleStage.CONTAINER_STARTED
import org.dockercontainerobjects.ContainerObjectLifecycleStage.CONTAINER_STOPPED
import org.dockercontainerobjects.ContainerObjectLifecycleStage.IMAGE_PREPARED
import org.dockercontainerobjects.ContainerObjectLifecycleStage.IMAGE_RELEASED
import org.dockercontainerobjects.ContainerObjectLifecycleStage.INSTANCE_CREATED
import org.dockercontainerobjects.ContainerObjectLifecycleStage.INSTANCE_DISCARDED
import org.dockercontainerobjects.ContainerObjectsManager.Companion.DOCKERFILE_DEFAULT_NAME
import org.dockercontainerobjects.ContainerObjectsManager.Companion.IMAGE_TAG_DYNAMIC_PLACEHOLDER
import org.dockercontainerobjects.ContainerObjectsManager.Companion.SCHEME_CLASSPATH
import org.dockercontainerobjects.ContainerObjectsManager.Companion.SCHEME_FILE
import org.dockercontainerobjects.ContainerObjectsManager.Companion.SCHEME_HTTP
import org.dockercontainerobjects.ContainerObjectsManager.Companion.SCHEME_HTTPS
import org.dockercontainerobjects.annotations.AfterContainerCreated
import org.dockercontainerobjects.annotations.AfterContainerRemoved
import org.dockercontainerobjects.annotations.AfterContainerRestarted
import org.dockercontainerobjects.annotations.AfterContainerStarted
import org.dockercontainerobjects.annotations.AfterContainerStopped
import org.dockercontainerobjects.annotations.AfterImageBuilt
import org.dockercontainerobjects.annotations.AfterImagePrepared
import org.dockercontainerobjects.annotations.AfterImageReleased
import org.dockercontainerobjects.annotations.AfterImageRemoved
import org.dockercontainerobjects.annotations.BeforeBuildingImage
import org.dockercontainerobjects.annotations.BeforeCreatingContainer
import org.dockercontainerobjects.annotations.BeforePreparingImage
import org.dockercontainerobjects.annotations.BeforeReleasingImage
import org.dockercontainerobjects.annotations.BeforeRemovingContainer
import org.dockercontainerobjects.annotations.BeforeRemovingImage
import org.dockercontainerobjects.annotations.BeforeRestartingContainer
import org.dockercontainerobjects.annotations.BeforeStartingContainer
import org.dockercontainerobjects.annotations.BeforeStoppingContainer
import org.dockercontainerobjects.annotations.BuildImage
import org.dockercontainerobjects.annotations.BuildImageContent
import org.dockercontainerobjects.annotations.BuildImageContentEntry
import org.dockercontainerobjects.annotations.Environment
import org.dockercontainerobjects.annotations.EnvironmentEntry
import org.dockercontainerobjects.annotations.OnLogEntry
import org.dockercontainerobjects.annotations.RegistryImage
import org.dockercontainerobjects.docker.Addresses
import org.dockercontainerobjects.docker.ContainerLocator
import org.dockercontainerobjects.docker.ContainerSpec
import org.dockercontainerobjects.docker.ContainerStatus.CREATED
import org.dockercontainerobjects.docker.ContainerStatus.EXITED
import org.dockercontainerobjects.docker.ContainerStatus.RUNNING
import org.dockercontainerobjects.docker.ImageName
import org.dockercontainerobjects.docker.ImageNotFoundException
import org.dockercontainerobjects.docker.ImageSpec
import org.dockercontainerobjects.docker.NetworkSettings
import org.dockercontainerobjects.util.and
import org.dockercontainerobjects.util.annotatedWith
import org.dockercontainerobjects.util.call
import org.dockercontainerobjects.util.debug
import org.dockercontainerobjects.util.expectingNoParameters
import org.dockercontainerobjects.util.expectingParameterCount
import org.dockercontainerobjects.util.findMethods
import org.dockercontainerobjects.util.getAnnotation
import org.dockercontainerobjects.util.getAnnotationsByType
import org.dockercontainerobjects.util.instantiate
import org.dockercontainerobjects.util.invokeInstanceMethods
import org.dockercontainerobjects.util.isOfReturnType
import org.dockercontainerobjects.util.loggerFor
import org.dockercontainerobjects.util.onInstance
import org.dockercontainerobjects.util.stream
import org.dockercontainerobjects.util.targz
import org.dockercontainerobjects.util.toSnakeCase
import org.dockercontainerobjects.util.withEntry
import java.io.File
import java.io.IOException
import java.lang.reflect.Method
import java.net.URI
import java.time.Instant
import java.util.UUID
import java.util.stream.Collectors

class ContainerObjectsManagerImpl(private val env: ContainerObjectsEnvironment): ContainerObjectsManager {

    companion object {

        const val IMAGE_TAG_DEFAULT_TEMPLATE = "%s_%s:latest"

        const val SCHEME_PATH_SEPARATOR = "://"

        const val SCHEME_CLASSPATH_PREFIX = SCHEME_CLASSPATH+SCHEME_PATH_SEPARATOR
        const val SCHEME_FILE_PREFIX = SCHEME_FILE+SCHEME_PATH_SEPARATOR
        const val SCHEME_HTTP_PREFIX = SCHEME_HTTP+SCHEME_PATH_SEPARATOR
        const val SCHEME_HTTPS_PREFIX = SCHEME_HTTPS+SCHEME_PATH_SEPARATOR

        private val l = loggerFor<ContainerObjectsManagerImpl>()

        private fun <T: Any> createInstance(ctx: ContainerObjectContextImpl<T>) {
            // create container instance
            val containerInstance = ctx.type.instantiate()
            ctx.instance = containerInstance

            // initialize containers in instance fields
            ctx.environment.enhancer.setupInstance(containerInstance)
            ctx.stage = INSTANCE_CREATED
        }

        private fun <T: Any> prepareImage(ctx: ContainerObjectContextImpl<T>) {
            val containerType = ctx.type
            val containerInstance = ctx.instance ?: throw IllegalStateException()
            containerInstance.invokeContainerLifecycleListeners<BeforePreparingImage>()
            l.debug { "preparing image for container class '${containerType.simpleName}'" }
            // check for image from annotation or method
            val registryImageAnnotation = containerType.getAnnotation<RegistryImage>()
            val registryImageMethods = containerType.findMethods(
                        expectingNoParameters() and annotatedWith(RegistryImage::class))
                    .stream()
                    .collect(Collectors.toList())
            val buildImageAnnotation = containerType.getAnnotation<BuildImage>()
            val buildImageMethods = containerType.findMethods(
                        expectingNoParameters() and annotatedWith(BuildImage::class))
                    .stream()
                    .collect(Collectors.toList())
            var imageDefinitionOptions = 0
            if (registryImageAnnotation !== null) imageDefinitionOptions++
            imageDefinitionOptions += registryImageMethods.size
            if (buildImageAnnotation !== null) imageDefinitionOptions++
            imageDefinitionOptions += buildImageMethods.size
            if (imageDefinitionOptions != 1) {
                throw IllegalArgumentException(
                        "Container class '${containerType.simpleName}' has more than one way to define the image to use")
            }
            val containerConfig: ContainerConfiguration
            var imageBuilt = false

            if (registryImageAnnotation != null) {
                containerConfig = containerConfigFromClass(registryImageAnnotation, containerType)
            } else if (registryImageMethods.isNotEmpty()) {
                containerConfig = containerConfigFromMethod(registryImageMethods.first(), containerType, containerInstance)
            } else {
                containerInstance.invokeContainerLifecycleListeners<BeforeBuildingImage>()
                val imageConfig = when {
                    buildImageAnnotation !== null -> imageConfigFromClass(buildImageAnnotation, containerType)
                    buildImageMethods.isNotEmpty() -> imageConfigFromMethod(buildImageMethods.first(), containerType, containerInstance)
                    else -> throw IllegalStateException() // we would have failed earlier
                }

                if (imageConfig.tag.isEmpty()) {
                    imageConfig.tag = defaultImageTag(containerType)
                }
                if (imageConfig.tag.contains(IMAGE_TAG_DYNAMIC_PLACEHOLDER)) {
                    imageConfig.tag = imageConfig.tag.replace(IMAGE_TAG_DYNAMIC_PLACEHOLDER, UUID.randomUUID().toString())
                }
                imageConfig.tag = imageConfig.tag.toLowerCase()
                val content = mutableMapOf<String, Any>()
                containerType.getAnnotationsByType<BuildImageContentEntry>().stream().forEach {
                    l.debug { "Adding entry name '${it.name}' with content '${it.value}'" }
                    content.put(it.name, it.value.normalize(containerType))
                }
                containerType.findMethods(
                            expectingNoParameters() and annotatedWith(BuildImageContent::class))
                        .stream()
                        .forEach {
                            if (!it.isOfReturnType<Map<String, Any>>())
                                throw IllegalArgumentException (
                                        "Method '$it' on class '${containerType.simpleName}' must return a Map to be used to define the image to use")
                            @Suppress("UNCHECKED_CAST")
                            val newContent = it.call(containerInstance) as Map<String, Any>?
                            if (newContent !== null) content.putAll(newContent)
                        }

                l.debug { "image for container class '${containerType.simpleName}' will be build and tagged as '${imageConfig.tag}'"  }
                val image = ImageName(imageConfig.tag)
                val specs =
                        imageConfig.ref.let {
                            when (it) {
                                is File -> ImageSpec(if (it.isDirectory()) File(it, DOCKERFILE_DEFAULT_NAME) else it)
                                else -> ImageSpec(buildDockerImageContent(it, content, containerType))
                            }
                        }
                        .withTag(image)
                        .withPull(imageConfig.forcePull)
                val generatedImageId = ctx.environment.docker.images.build(specs)
                l.debug { "image for container class '${containerType.simpleName}' built with id '$generatedImageId' and tagged as '${imageConfig.tag}'" }
                containerConfig = ContainerConfiguration(
                        spec = ContainerSpec(image),
                        autoRemove = true,
                        forcePull = false
                )
                imageBuilt = true
                containerInstance.invokeContainerLifecycleListeners<AfterImageBuilt>()
            }

            val imagePresent =
                    imageBuilt ||
                            ctx.environment.docker.images.isAvailable(containerConfig.spec.image)
            if (imagePresent && !imageBuilt) containerConfig.autoRemove = false
            if (containerConfig.forcePull || !imagePresent) {
                ctx.environment.docker.images.pull(containerConfig.spec.image as ImageName)
            }

            ctx.image = containerConfig.spec.image
            ctx.autoRemoveImage = containerConfig.autoRemove
            ctx.stage = IMAGE_PREPARED
            containerInstance.invokeContainerLifecycleListeners<AfterImagePrepared>()
        }

        private fun <T: Any> createContainer(ctx: ContainerObjectContextImpl<T>) {
            val containerInstance = ctx.instance ?: throw IllegalStateException()
            val containerImageId = ctx.image ?: throw IllegalStateException()
            containerInstance.invokeContainerLifecycleListeners<BeforeCreatingContainer>()

            val containerEnvironment = collectContainerEnvironmentVariables(containerInstance)
            val spec = ContainerSpec(containerImageId).withEnvironmentVariables(containerEnvironment)
            ctx.container = ctx.environment.docker.containers.create(spec)

            ctx.stage = CONTAINER_CREATED
            containerInstance.invokeContainerLifecycleListeners<AfterContainerCreated>()
        }

        private fun collectContainerEnvironmentVariables(containerInstance: Any): Map<String, String> {
            val containerType = containerInstance.javaClass

            val environment = mutableMapOf<String, String>()
            // check for environment defined as class annotations
            environment +=
                    containerType.getAnnotationsByType<EnvironmentEntry>()
                            .map {
                                if (it.name.isEmpty()) {
                                    it.value.substringBefore('=') to it.value.substringAfter('=', "")
                                } else {
                                    it.name to it.value
                                }
                            }.toMap()
            // check for environment defined as methods
            containerType.findMethods(expectingNoParameters() and annotatedWith<Method>(Environment::class))
                    .forEach {
                        if (!it.isOfReturnType<Map<String, String>>()) {
                            throw IllegalArgumentException(
                                    "Method '$it' on class '${containerType.simpleName}' must return a Map to be used to specify environment variales"
                            )
                        }
                        @Suppress("UNCHECKED_CAST")
                        val newEnvironment = it.call(containerInstance) as Map<String, String>?
                        if (newEnvironment !== null) environment += newEnvironment
            }

            return environment
        }

        private fun <T: Any> startContainer(ctx: ContainerObjectContextImpl<T>) {
            val containerInstance = ctx.instance ?: throw IllegalStateException()
            val currContainerId = ctx.container ?: throw IllegalStateException()

            containerInstance.invokeContainerLifecycleListeners<BeforeStartingContainer>()
            val start = Instant.now()
            ctx.environment.docker.containers.start(currContainerId)
            val info = ctx.environment.docker.containers.inspect(currContainerId)

            ctx.networkSettings = info.network
            ctx.stage = CONTAINER_STARTED
            containerInstance.invokeContainerLifecycleListeners<AfterContainerStarted>()
            registerContainerLogReceivers(ctx, start)
        }

        private fun <T: Any> stopContainer(ctx: ContainerObjectContextImpl<T>) {
            val containerInstance = ctx.instance ?: throw IllegalStateException()
            val currContainerId = ctx.container ?: throw IllegalStateException()

            containerInstance.invokeContainerLifecycleListeners<BeforeStoppingContainer>()
            ctx.environment.docker.containers.stop(currContainerId)

            ctx.stage = CONTAINER_STOPPED
            containerInstance.invokeContainerLifecycleListeners<AfterContainerStopped>()
        }

        private fun <T: Any> restartContainer(ctx: ContainerObjectContextImpl<T>) {
            val containerInstance = ctx.instance ?: throw IllegalStateException()

            containerInstance.invokeContainerLifecycleListeners<BeforeRestartingContainer>()
            stopContainer(ctx)
            startContainer(ctx)
            containerInstance.invokeContainerLifecycleListeners<AfterContainerRestarted>()
        }

        private fun <T: Any> removeContainer(ctx: ContainerObjectContextImpl<T>) {
            val containerInstance = ctx.instance ?: throw IllegalStateException()
            val currContainerId = ctx.container ?: throw IllegalStateException()

            containerInstance.invokeContainerLifecycleListeners<BeforeRemovingContainer>()
            ctx.environment.docker.containers.remove(currContainerId)

            ctx.stage = CONTAINER_REMOVED
            containerInstance.invokeContainerLifecycleListeners<AfterContainerRemoved>()
        }

        private fun <T: Any> teardownImage(ctx: ContainerObjectContextImpl<T>) {
            val containerInstance = ctx.instance ?: throw IllegalStateException()
            val currImageId = ctx.image ?: throw IllegalStateException()

            containerInstance.invokeContainerLifecycleListeners<BeforeReleasingImage>()
            // try to remove image if requested
            if (ctx.autoRemoveImage == true) { // verifies for not null and true in one go
                try {
                    containerInstance.invokeContainerLifecycleListeners<BeforeRemovingImage>()
                    ctx.environment.docker.images.remove(currImageId)
                    containerInstance.invokeContainerLifecycleListeners<AfterImageRemoved>()
                } catch (e: ImageNotFoundException) {
                    // if the image is already removed, log and ignore
                    l.warn("Image '${ctx.image}' requested to be auto-removed, but was not found", e)
                } catch (e: RuntimeException) {
                    // TODO check if there is a more specific exception for this case
                    // if the image is still in use, log and ignore
                    l.warn("Image '${ctx.image}' requested to be auto-removed, but seems to be still in use", e)
                }
            }

            ctx.stage = IMAGE_RELEASED
            containerInstance.invokeContainerLifecycleListeners<AfterImageReleased>()
        }

        private fun <T: Any> discardInstance(ctx: ContainerObjectContextImpl<T>) {
            val containerInstance = ctx.instance ?: throw IllegalStateException()

            ctx.environment.enhancer.teardownInstance(containerInstance)
            ctx.stage = INSTANCE_DISCARDED
        }

        private fun <T: Any> registerContainerLogReceivers(ctx: ContainerObjectContextImpl<T>, since: Instant) {
            val containerInstance = ctx.instance ?: throw IllegalStateException()
            val currContainerId = ctx.container ?: throw IllegalStateException()

            val type = containerInstance.javaClass
            type.findMethods(
                        onInstance<Method>() and expectingParameterCount(1) and annotatedWith(OnLogEntry::class))
                    .forEach { method ->
                        val spec = containerLogSpecFromMethod(method, type, containerInstance, since)
                        ctx.environment.docker.containers.logs(currContainerId, spec)
                    }
        }

        private fun Any.invokeContainerLifecycleListeners(annotationType: Class<out Annotation>) {
            l.debug { "Invoking life-cycle event '${annotationType.simpleName}'" }
            invokeInstanceMethods<Any, Any, Any, Any>(
                    onInstance<Method>() and expectingNoParameters() and annotatedWith<Method>(annotationType))
        }

        inline private fun <reified A: Annotation> Any.invokeContainerLifecycleListeners() =
            invokeContainerLifecycleListeners(A::class.java)

        @Throws(IOException::class)
        private fun buildDockerImageContent(dockerfile: Any, entries: Map<String, Any>?, containerType: Class<*>) =
            targz {
                it.withEntry(DOCKERFILE_DEFAULT_NAME, dockerfile.normalize(containerType))
                entries?.forEach { k, v -> it.withEntry(k, v.normalize(containerType)) }
            }

        private fun Any.normalize(loader: Class<*>) =
            if (this is String) {
                when {
                    startsWith(SCHEME_CLASSPATH_PREFIX) -> {
                        loader.getResource(substring(SCHEME_CLASSPATH_PREFIX.length))
                                ?: throw IllegalArgumentException("Resource not found: " + this)
                    }
                    startsWith(SCHEME_FILE_PREFIX) -> URI.create(substring(SCHEME_FILE_PREFIX.length)) as URI
                    startsWith(SCHEME_HTTP_PREFIX) -> URI.create(substring(SCHEME_HTTP_PREFIX.length)) as URI
                    startsWith(SCHEME_HTTPS_PREFIX) -> URI.create(substring(SCHEME_HTTPS_PREFIX.length)) as URI
                    else -> this
                }
            } else {
                this
            }

        private fun defaultImageTag(containerType: Class<*>) =
            IMAGE_TAG_DEFAULT_TEMPLATE.format(
                    containerType.simpleName.toSnakeCase(),
                    IMAGE_TAG_DYNAMIC_PLACEHOLDER)
    }

    @Throws(IOException::class)
    override fun close() = env.close()

    override fun <T: Any> create(containerType: Class<T>): T {
        val ctx = ContainerObjectContextImpl(env, containerType)
        // instance creation stage
        createInstance(ctx)
        // image preparation stage
        prepareImage(ctx)
        // container creation stage
        createContainer(ctx)
        // container started stage
        startContainer(ctx)
        // register container
        env.registerContainerObject(ctx)

        return ctx.instance!!
    }

    override fun <T: Any> destroy(containerInstance: T) {
        @Suppress("UNCHECKED_CAST")
        val ctx = env.getContainerObjectRegistration(containerInstance) as ContainerObjectContextImpl<T>
        env.unregisterContainerObject(ctx)

        // container stopping stage
        stopContainer(ctx)
        // container removing stage
        removeContainer(ctx)
        // image release stage
        teardownImage(ctx)
        // instance discarded stage
        discardInstance(ctx)
    }

    override fun <T: Any> restart(containerInstance: T) {
        @Suppress("UNCHECKED_CAST")
        val ctx = env.getContainerObjectRegistration(containerInstance) as ContainerObjectContextImpl<T>
        restartContainer(ctx)
    }

    override fun getContainerId(containerInstance: Any): ContainerLocator? =
            env.getContainerObjectRegistration(containerInstance).container

    override fun getContainerStatus(containerInstance: Any): ContainerObjectsManager.ContainerStatus {
        val containerId = getContainerId(containerInstance)
        if (containerId === null) return ContainerObjectsManager.ContainerStatus.UNKNOWN
        val status = env.docker.containers.status(containerId)
        return when (status) {
            CREATED -> ContainerObjectsManager.ContainerStatus.CREATED
            RUNNING -> ContainerObjectsManager.ContainerStatus.STARTED
            EXITED -> ContainerObjectsManager.ContainerStatus.STOPPED
            else -> ContainerObjectsManager.ContainerStatus.UNKNOWN
        }
    }

    override fun getContainerNetworkSettings(containerInstance: Any): NetworkSettings? =
            env.getContainerObjectRegistration(containerInstance).networkSettings

    override fun getContainerAddresses(containerInstance: Any): Addresses? =
            env.getContainerObjectRegistration(containerInstance).networkSettings?.addresses
}
