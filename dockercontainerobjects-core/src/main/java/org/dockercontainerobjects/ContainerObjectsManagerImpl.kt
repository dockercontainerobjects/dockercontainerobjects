package org.dockercontainerobjects

import com.github.dockerjava.api.exception.DockerException
import com.github.dockerjava.api.exception.NotFoundException
import com.github.dockerjava.api.model.NetworkSettings
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
import org.dockercontainerobjects.docker.buildImage
import org.dockercontainerobjects.docker.createContainer
import org.dockercontainerobjects.docker.fetchContainerLogs
import org.dockercontainerobjects.docker.inetAddressOfType
import org.dockercontainerobjects.docker.inspectContainer
import org.dockercontainerobjects.docker.inspectImage
import org.dockercontainerobjects.docker.pullImage
import org.dockercontainerobjects.docker.removeContainer
import org.dockercontainerobjects.docker.removeImage
import org.dockercontainerobjects.docker.startContainer
import org.dockercontainerobjects.docker.stopContainer
import org.dockercontainerobjects.docker.MethodCallerLogResultCallback
import org.dockercontainerobjects.util.and
import org.dockercontainerobjects.util.annotatedWith
import org.dockercontainerobjects.util.targz
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
import org.dockercontainerobjects.util.toSnakeCase
import org.dockercontainerobjects.util.withEntry
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.lang.reflect.Method
import java.net.InetAddress
import java.net.URI
import java.net.URL
import java.time.Instant
import java.time.temporal.Temporal
import java.util.UUID
import java.util.stream.Collectors
import java.util.stream.Collectors.toList

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
            var imageId: String?
            var forcePull = false
            var autoRemove: Boolean
            var imageBuilt = false

            if (registryImageAnnotation !== null) {
                imageId = registryImageAnnotation.value
                if (imageId.isNullOrEmpty()) {
                    throw IllegalArgumentException(
                            "Annotation '${RegistryImage::class.java.simpleName}' on class '${containerType.simpleName}' must define a value to be used to define the image to use")
                }
                forcePull = registryImageAnnotation.forcePull
                autoRemove = registryImageAnnotation.autoRemove
            } else if (registryImageMethods.isNotEmpty()) {
                val registryImageMethod = registryImageMethods[0]
                if (!registryImageMethod.isOfReturnType<String>()) {
                    throw IllegalArgumentException(
                            "Method '$registryImageMethod' on class '${containerType.simpleName}' must return '${String::class.java.simpleName}' to be used to define the image to use")
                }
                val registryImageAnnotationValue = registryImageMethod.getAnnotation<RegistryImage>()!!
                imageId = registryImageAnnotationValue.value
                if (!imageId.isNullOrEmpty()) {
                    throw IllegalArgumentException(
                            "Annotation '${RegistryImage::class.java.simpleName}' on method '$registryImageMethod' on class '${containerType.simpleName}' cannot define a value to be used to define the image to use")
                }
                imageId = registryImageMethod.call(containerInstance) as String
                if (imageId.isNullOrEmpty()) {
                    throw IllegalArgumentException(
                            "Method '$registryImageMethod' on class '${containerType.simpleName}' must return a non-null value to be used to define the image to use")
                }
                forcePull = registryImageAnnotationValue.forcePull
                autoRemove = registryImageAnnotationValue.autoRemove
            } else {
                containerInstance.invokeContainerLifecycleListeners<BeforeBuildingImage>()
                var imageRef: Any? = null
                var imageTag: String? = null
                if (buildImageAnnotation !== null) {
                    imageRef = buildImageAnnotation.value
                    if (imageRef.isEmpty()) {
                        throw IllegalArgumentException(
                                "Annotation '${BuildImage::class.java.simpleName}' on class '${containerType.simpleName}' must define a non-empty value pointing to a Dockerfile")
                    }
                    imageTag = buildImageAnnotation.tag
                } else if (buildImageMethods.isNotEmpty()) {
                    val buildImageMethod = buildImageMethods[0]
                    listOf(String::class, URI::class, URL::class, File::class, InputStream::class).stream().filter { buildImageMethod.isOfReturnType(it) }.findAny().orElseThrow {
                        IllegalArgumentException(
                                "Method '$buildImageMethod' on class '${containerType.simpleName}' must return a valid type pointing to a Dockerfile")
                    }
                    imageRef = buildImageMethod.call(containerInstance)
                    if (imageRef == null) {
                        throw IllegalArgumentException(
                                "Method '$buildImageMethod' on class '${containerType.simpleName}' must return a non-null value to be used to define the image to use")
                    }
                }

                if (imageTag == null || imageTag.isEmpty()) {
                    imageTag = defaultImageTag(containerType)
                }
                if (imageTag.contains(IMAGE_TAG_DYNAMIC_PLACEHOLDER)) {
                    imageTag = imageTag.replace(IMAGE_TAG_DYNAMIC_PLACEHOLDER, UUID.randomUUID().toString())
                }
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

                val cleanImageTag = imageTag.toLowerCase()
                l.debug { "image for container class '${containerType.simpleName}' will be build and tagged as '$cleanImageTag'"  }
                val generatedImageId =
                        if (imageRef is File) {
                            ctx.environment.dockerClient.buildImage(imageRef, cleanImageTag, forcePull)
                        } else {
                            val tar = buildDockerImageContent(imageRef!!, content, containerType)
                            ctx.environment.dockerClient.buildImage(tar, cleanImageTag, forcePull)
                        }
                l.debug { "image for container class '${containerType.simpleName}' build with id '$generatedImageId' and tagged as '$cleanImageTag'" }
                imageId = cleanImageTag
                autoRemove = true
                imageBuilt = true
                containerInstance.invokeContainerLifecycleListeners<AfterImageBuilt>()
            }

            val imagePresent = imageBuilt ||
                    try {
                        imageId = ctx.environment.dockerClient.inspectImage(imageId).id
                        true
                    } catch (e: NotFoundException) {
                        false
                    }
            if (imagePresent && !imageBuilt) autoRemove = false
            if (forcePull || !imagePresent) {
                imageId = ctx.environment.dockerClient.pullImage(imageId).id
            }

            ctx.imageId = imageId
            ctx.autoRemoveImage = autoRemove
            ctx.stage = IMAGE_PREPARED
            containerInstance.invokeContainerLifecycleListeners<AfterImagePrepared>()
        }

        private fun <T: Any> createContainer(ctx: ContainerObjectContextImpl<T>) {
            val containerInstance = ctx.instance ?: throw IllegalStateException()
            val containerImageId = ctx.imageId ?: throw IllegalStateException()
            containerInstance.invokeContainerLifecycleListeners<BeforeCreatingContainer>()

            val containerEnvironment = collectContainerEnvironmentVariables(containerInstance)
            ctx.containerId = ctx.environment.dockerClient.createContainer(containerImageId, containerEnvironment).id

            ctx.stage = CONTAINER_CREATED
            containerInstance.invokeContainerLifecycleListeners<AfterContainerCreated>()
        }

        private fun collectContainerEnvironmentVariables(containerInstance: Any): List<String> {
            val containerType = containerInstance.javaClass

            val environment = mutableListOf<String>()
            // check for environment defined as class annotations
            environment.addAll(
                    containerType.getAnnotationsByType<EnvironmentEntry>()
                            .stream()
                            .map { if (it.name.isEmpty()) it.value else it.name+"="+it.value }
                            .collect(toList()))
            // check for environment defined as methods
            containerType.findMethods(expectingNoParameters() and annotatedWith<Method>(Environment::class))
                    .stream()
                    .forEach {
                        if (!it.isOfReturnType<Map<String, String>>())
                            throw IllegalArgumentException(
                                    "Method '$it' on class '${containerType.simpleName}' must return a Map to be used to specify environment variales")
                        @Suppress("UNCHECKED_CAST")
                        val newEnvironment = it.call(containerInstance) as Map<String, String>?
                        if (newEnvironment !== null)
                            environment.addAll(
                                    newEnvironment.entries.stream()
                                            .map { (key, value) -> key+"="+value }
                                            .collect(toList()))
            }

            return environment
        }

        private fun <T: Any> startContainer(ctx: ContainerObjectContextImpl<T>) {
            val containerInstance = ctx.instance ?: throw IllegalStateException()
            val currContainerId = ctx.containerId ?: throw IllegalStateException()

            containerInstance.invokeContainerLifecycleListeners<BeforeStartingContainer>()
            val start = Instant.now()
            val response = ctx.environment.dockerClient.startContainer(currContainerId)

            ctx.networkSettings = response.networkSettings
            ctx.stage = CONTAINER_STARTED
            containerInstance.invokeContainerLifecycleListeners<AfterContainerStarted>()
            registerContainerLogReceivers(ctx, start)
        }

        private fun <T: Any> stopContainer(ctx: ContainerObjectContextImpl<T>) {
            val containerInstance = ctx.instance ?: throw IllegalStateException()
            val currContainerId = ctx.containerId ?: throw IllegalStateException()

            containerInstance.invokeContainerLifecycleListeners<BeforeStoppingContainer>()
            ctx.environment.dockerClient.stopContainer(currContainerId)

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
            val currContainerId = ctx.containerId ?: throw IllegalStateException()

            containerInstance.invokeContainerLifecycleListeners<BeforeRemovingContainer>()
            ctx.environment.dockerClient.removeContainer(currContainerId)

            ctx.stage = CONTAINER_REMOVED
            containerInstance.invokeContainerLifecycleListeners<AfterContainerRemoved>()
        }

        private fun <T: Any> teardownImage(ctx: ContainerObjectContextImpl<T>) {
            val containerInstance = ctx.instance ?: throw IllegalStateException()
            val currImageId = ctx.imageId ?: throw IllegalStateException()

            containerInstance.invokeContainerLifecycleListeners<BeforeReleasingImage>()
            // try to remove image if requested
            if (ctx.autoRemoveImage == true) { // verifies for not null and true in one go
                try {
                    containerInstance.invokeContainerLifecycleListeners<BeforeRemovingImage>()
                    ctx.environment.dockerClient.removeImage(currImageId)
                    containerInstance.invokeContainerLifecycleListeners<AfterImageRemoved>()
                } catch (e: NotFoundException) {
                    // if the image is already removed, log and ignore
                    l.warn("Image '${ctx.imageId}' requested to be auto-removed, but was not found", e)
                } catch (e: DockerException) {
                    // TODO check if there is a more specific exception for this case
                    // if the image is still in use, log and ignore
                    l.warn("Image '${ctx.imageId}' requested to be auto-removed, but seems to be still in use", e)
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

        private fun <T: Any> registerContainerLogReceivers(ctx: ContainerObjectContextImpl<T>, since: Temporal) {
            val containerInstance = ctx.instance ?: throw IllegalStateException()
            val currContainerId = ctx.containerId ?: throw IllegalStateException()

            val type = containerInstance.javaClass
            type.findMethods(
                        onInstance<Method>() and expectingParameterCount(1) and annotatedWith(OnLogEntry::class))
                    .forEach { method ->
                        val logEntryAnotation = method.getAnnotation<OnLogEntry>()!!
                        val paramType: Class<*> = method.parameterTypes[0]
                        val callback =
                                when (paramType.kotlin) {
                                    LogEntryContext::class -> MethodCallerLogResultCallback.LogEntryContextArgumentMethodCallback(ctx, method)
                                    String::class -> MethodCallerLogResultCallback.StringArgumentMethodCallback(ctx, method)
                                    ByteArray::class -> MethodCallerLogResultCallback.ByteArrayArgumentMethodCallback(ctx, method)
                                    else -> null
                                }
                        if (callback !== null)
                            ctx.environment.dockerClient.fetchContainerLogs(
                                    currContainerId, since,
                                    logEntryAnotation.includeStdOut, logEntryAnotation.includeStdErr, logEntryAnotation.includeTimestamps,
                                    callback)
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

    override fun getContainerId(containerInstance: Any): String? =
            env.getContainerObjectRegistration(containerInstance).containerId

    override fun getContainerStatus(containerInstance: Any): ContainerObjectsManager.ContainerStatus {
        val containerId = getContainerId(containerInstance)
        if (containerId === null) return ContainerObjectsManager.ContainerStatus.UNKNOWN
        val state = env.dockerClient.inspectContainer(containerId).state
        return when (state.status) {
            "running" -> ContainerObjectsManager.ContainerStatus.STARTED
            "created" -> ContainerObjectsManager.ContainerStatus.CREATED
            "exited" -> ContainerObjectsManager.ContainerStatus.STOPPED
            else -> ContainerObjectsManager.ContainerStatus.UNKNOWN
        }
    }

    override fun getContainerNetworkSettings(containerInstance: Any): NetworkSettings? =
            env.getContainerObjectRegistration(containerInstance).networkSettings

    override fun <ADDR: InetAddress> getContainerAddress(containerInstance: Any, addrType: Class<ADDR>): ADDR? =
            env.getContainerObjectRegistration(containerInstance).networkSettings?.inetAddressOfType(addrType)
}
