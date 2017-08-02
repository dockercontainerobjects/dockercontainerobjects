package org.dockercontainerobjects

import com.github.dockerjava.api.exception.DockerException
import com.github.dockerjava.api.exception.NotFoundException
import com.github.dockerjava.api.model.NetworkSettings
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
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
import org.dockercontainerobjects.docker.DockerClientExtensions.buildImage
import org.dockercontainerobjects.docker.DockerClientExtensions.createContainer
import org.dockercontainerobjects.docker.DockerClientExtensions.fetchContainerLogs
import org.dockercontainerobjects.docker.DockerClientExtensions.inetAddressOfType
import org.dockercontainerobjects.docker.DockerClientExtensions.inspectContainer
import org.dockercontainerobjects.docker.DockerClientExtensions.inspectImage
import org.dockercontainerobjects.docker.DockerClientExtensions.pullImage
import org.dockercontainerobjects.docker.DockerClientExtensions.removeContainer
import org.dockercontainerobjects.docker.DockerClientExtensions.removeImage
import org.dockercontainerobjects.docker.DockerClientExtensions.startContainer
import org.dockercontainerobjects.docker.DockerClientExtensions.stopContainer
import org.dockercontainerobjects.docker.MethodCallerLogResultCallback
import org.dockercontainerobjects.util.and
import org.dockercontainerobjects.util.annotatedWith
import org.dockercontainerobjects.util.buildTARGZ
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

        private fun <T: Any> ContainerObjectContextImpl<T>.createInstance() {
            // create container instance
            val containerInstance = type.instantiate()
            instance = containerInstance

            // initialize containers in instance fields
            environment.enhancer.setupInstance(containerInstance)
            stage = INSTANCE_CREATED
        }

        private fun <T: Any> ContainerObjectContextImpl<T>.prepareImage() {
            val containerType = type
            val containerInstance = instance ?: throw IllegalStateException()
            containerInstance.invokeContainerLifecycleListeners<BeforePreparingImage>()
            l.debug { "preparing image for container class '${containerType.simpleName}'" }
            // check for image from annotation or method
            val registryImageAnnotation = containerType.getAnnotation<RegistryImage>()
            val registryImageMethods = containerType.findMethods(expectingNoParameters() and annotatedWith<Method>(RegistryImage::class))
                    .stream()
                    .collect(Collectors.toList())
            val buildImageAnnotation = containerType.getAnnotation<BuildImage>()
            val buildImageMethods = containerType.findMethods(expectingNoParameters() and annotatedWith<Method>(BuildImage::class))
                    .stream()
                    .collect(Collectors.toList())
            var options = 0
            if (registryImageAnnotation !== null) options++
            options += registryImageMethods.size
            if (buildImageAnnotation !== null) options++
            options += buildImageMethods.size
            if (options != 1)
                throw IllegalArgumentException(
                        "Container class '${containerType.simpleName}' has more than one way to define the image to use")
            var imageId: String? = null
            var forcePull: Boolean = false
            var autoRemove: Boolean = false
            var imageBuilt: Boolean = false

            if (registryImageAnnotation !== null) {
                imageId = registryImageAnnotation.value
                if (imageId.isNullOrEmpty())
                    throw IllegalArgumentException(
                            "Annotation '${RegistryImage::class.java.simpleName}' on class '${containerType.simpleName}' must define a value to be used to define the image to use")
                forcePull = registryImageAnnotation.forcePull
                autoRemove = registryImageAnnotation.autoRemove
            } else if (registryImageMethods.isNotEmpty()) {
                val registryImageMethod = registryImageMethods[0]
                if (!registryImageMethod.isOfReturnType<String>())
                    throw IllegalArgumentException(
                            "Method '$registryImageMethod' on class '${containerType.simpleName}' must return '${String::class.java.simpleName}' to be used to define the image to use")
                val registryImageAnnotationValue = registryImageMethod.getAnnotation<RegistryImage>()!!
                imageId = registryImageAnnotationValue.value
                if (!imageId.isNullOrEmpty())
                    throw IllegalArgumentException(
                            "Annotation '${RegistryImage::class.java.simpleName}' on method '$registryImageMethod' on class '${containerType.simpleName}' cannot define a value to be used to define the image to use")
                imageId = registryImageMethod.call(containerInstance) as String
                if (imageId.isNullOrEmpty())
                    throw IllegalArgumentException(
                            "Method '$registryImageMethod' on class '${containerType.simpleName}' must return a non-null value to be used to define the image to use")
                forcePull = registryImageAnnotationValue.forcePull
                autoRemove = registryImageAnnotationValue.autoRemove
            } else {
                containerInstance.invokeContainerLifecycleListeners<BeforeBuildingImage>()
                var imageRef: Any? = null
                var imageTag: String? = null
                if (buildImageAnnotation !== null) {
                    imageRef = buildImageAnnotation.value
                    if (imageRef.isEmpty())
                        throw IllegalArgumentException(
                                "Annotation '${BuildImage::class.java.simpleName}' on class '${containerType.simpleName}' must define a non-empty value pointing to a Dockerfile")
                    imageTag = buildImageAnnotation.tag
                } else if (buildImageMethods.isNotEmpty()) {
                    val buildImageMethod = buildImageMethods[0]
                    listOf(String::class, URI::class, URL::class, File::class, InputStream::class).stream().filter { buildImageMethod.isOfReturnType(it) }.findAny().orElseThrow {
                        IllegalArgumentException(
                                "Method '$buildImageMethod' on class '${containerType.simpleName}' must return a valid type pointing to a Dockerfile")
                    }
                    imageRef = buildImageMethod.call(containerInstance)
                    if (imageRef == null)
                        throw IllegalArgumentException(
                                "Method '$buildImageMethod' on class '${containerType.simpleName}' must return a non-null value to be used to define the image to use")
                }

                if (imageTag == null || imageTag.isEmpty())
                    imageTag = defaultImageTag(containerType)
                if (imageTag.contains(IMAGE_TAG_DYNAMIC_PLACEHOLDER))
                    imageTag = imageTag.replace(IMAGE_TAG_DYNAMIC_PLACEHOLDER, UUID.randomUUID().toString())
                val content = mutableMapOf<String, Any>()
                containerType.getAnnotationsByType<BuildImageContentEntry>().stream().forEach {
                    l.debug { "Adding entry name '${it.name}' with content '${it.value}'" }
                    content.put(it.name, it.value.normalize(containerType))
                }
                containerType.findMethods(expectingNoParameters() and annotatedWith<Method>(BuildImageContent::class))
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
                        if (imageRef is File)
                            environment.dockerClient.buildImage(imageRef, cleanImageTag, forcePull)
                        else {
                            val tar = buildDockerTAR(imageRef!!, content, containerType)
                            environment.dockerClient.buildImage(tar, cleanImageTag, forcePull)
                        }
                l.debug { "image for container class '${containerType.simpleName}' build with id '$generatedImageId' and tagged as '$cleanImageTag'" }
                imageId = cleanImageTag
                autoRemove = true
                imageBuilt = true
                containerInstance.invokeContainerLifecycleListeners<AfterImageBuilt>()
            }

            val imagePresent = imageBuilt ||
                    try {
                        imageId = environment.dockerClient.inspectImage(imageId).id
                        true
                    } catch (e: NotFoundException) {
                        false
                    }
            if (imagePresent && !imageBuilt) autoRemove = false
            if (forcePull || !imagePresent) {
                imageId = environment.dockerClient.pullImage(imageId).id
            }

            this.imageId = imageId
            autoRemoveImage = autoRemove
            stage = IMAGE_PREPARED
            containerInstance.invokeContainerLifecycleListeners<AfterImagePrepared>()
        }

        private fun <T: Any> ContainerObjectContextImpl<T>.createContainer() {
            val containerInstance = instance ?: throw IllegalStateException()
            val containerImageId = imageId ?: throw IllegalStateException()
            containerInstance.invokeContainerLifecycleListeners<BeforeCreatingContainer>()

            val containerEnvironment = collectContainerEnvironmentVariables(containerInstance)
            containerId = environment.dockerClient.createContainer(containerImageId, containerEnvironment).id

            stage = CONTAINER_CREATED
            containerInstance.invokeContainerLifecycleListeners<AfterContainerCreated>()
        }

        private fun collectContainerEnvironmentVariables(containerInstance: Any): List<String> {
            val containerType = containerInstance.javaClass

            val environment = mutableListOf<String>()
            // check for environment defined as class annotations
            environment.addAll(
                    containerType.getAnnotationsByType<EnvironmentEntry>()
                            .stream()
                            .map { if (it.name.isNullOrEmpty()) it.value else it.name+"="+it.value }
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

        private fun <T: Any> ContainerObjectContextImpl<T>.startContainer() {
            val containerInstance = instance ?: throw IllegalStateException()
            val currContainerId = containerId ?: throw IllegalStateException()

            containerInstance.invokeContainerLifecycleListeners<BeforeStartingContainer>()
            val response = environment.dockerClient.startContainer(currContainerId)

            networkSettings = response.networkSettings
            stage = CONTAINER_STARTED
            containerInstance.invokeContainerLifecycleListeners<AfterContainerStarted>()
            registerContainerLogReceivers()
        }

        private fun <T: Any> ContainerObjectContextImpl<T>.stopContainer() {
            val containerInstance = instance ?: throw IllegalStateException()
            val currContainerId = containerId ?: throw IllegalStateException()

            containerInstance.invokeContainerLifecycleListeners<BeforeStoppingContainer>()
            environment.dockerClient.stopContainer(currContainerId)

            stage = CONTAINER_STOPPED
            containerInstance.invokeContainerLifecycleListeners<AfterContainerStopped>()
        }

        private fun <T: Any> ContainerObjectContextImpl<T>.restartContainer() {
            val containerInstance = instance ?: throw IllegalStateException()

            containerInstance.invokeContainerLifecycleListeners<BeforeRestartingContainer>()
            stopContainer()
            startContainer()
            containerInstance.invokeContainerLifecycleListeners<AfterContainerRestarted>()
        }

        private fun <T: Any> ContainerObjectContextImpl<T>.removeContainer() {
            val containerInstance = instance ?: throw IllegalStateException()
            val currContainerId = containerId ?: throw IllegalStateException()

            containerInstance.invokeContainerLifecycleListeners<BeforeRemovingContainer>()
            environment.dockerClient.removeContainer(currContainerId)

            stage = CONTAINER_REMOVED
            containerInstance.invokeContainerLifecycleListeners<AfterContainerRemoved>()
        }

        private fun <T: Any> ContainerObjectContextImpl<T>.teardownImage() {
            val containerInstance = instance ?: throw IllegalStateException()
            val currImageId = imageId ?: throw IllegalStateException()

            containerInstance.invokeContainerLifecycleListeners<BeforeReleasingImage>()
            // try to remove image if requested
            if (autoRemoveImage == true) // verifies for not null and true in one go
                try {
                    containerInstance.invokeContainerLifecycleListeners<BeforeRemovingImage>()
                    environment.dockerClient.removeImage(currImageId)
                    containerInstance.invokeContainerLifecycleListeners<AfterImageRemoved>()
                } catch (e: NotFoundException) {
                    // if the image is already removed, log and ignore
                    l.warn("Image '$imageId' requested to be auto-removed, but was not found", e)
                } catch (e: DockerException) {
                    // TODO check if there is a more specific exception for this case
                    // if the image is still in use, log and ignore
                    l.warn("Image '$imageId' requested to be auto-removed, but seems to be still in use", e)
                }

            stage = IMAGE_RELEASED
            containerInstance.invokeContainerLifecycleListeners<AfterImageReleased>()
        }

        private fun <T: Any> ContainerObjectContextImpl<T>.discardInstance() {
            val containerInstance = instance ?: throw IllegalStateException()

            environment.enhancer.teardownInstance(containerInstance)
            stage = INSTANCE_DISCARDED
        }

        private fun <T: Any> ContainerObjectContextImpl<T>.registerContainerLogReceivers() {
            val containerInstance = instance ?: throw IllegalStateException()
            val currContainerId = containerId ?: throw IllegalStateException()

            val type = containerInstance.javaClass
            type.findMethods(onInstance<Method>() and expectingParameterCount(1) and annotatedWith<Method>(OnLogEntry::class))
                    .forEach { method ->
                        val logEntryAnotation = method.getAnnotation<OnLogEntry>()!!
                        val paramType: Class<*> = method.parameterTypes[0]
                        val callback =
                                when (paramType.kotlin) {
                                    LogEntryContext::class -> MethodCallerLogResultCallback.LogEntryContextArgumentMethodCallback(this, method)
                                    String::class -> MethodCallerLogResultCallback.StringArgumentMethodCallback(this, method)
                                    ByteArray::class -> MethodCallerLogResultCallback.ByteArrayArgumentMethodCallback(this, method)
                                    else -> null
                                }
                        if (callback !== null)
                            environment.dockerClient.fetchContainerLogs(
                                    currContainerId, logEntryAnotation.includeStdOut, logEntryAnotation.includeStdErr, callback)
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
        private fun buildDockerTAR(dockerfile: Any, content: Map<String, Any>?, containerType: Class<*>) =
            buildTARGZ {
                it.withGenericEntry(DOCKERFILE_DEFAULT_NAME, dockerfile.normalize(containerType))
                if (content !== null)
                    content.forEach { k, v -> it.withGenericEntry(k, v.normalize(containerType)) }
            }

        @Throws(IOException::class)
        private fun TarArchiveOutputStream.withGenericEntry(filename: String, content: Any)  {
            l.debug { "Adding TAR entry with name '$filename' and content of type '${content.javaClass.simpleName}'" }
            when (content) {
                is URL -> withEntry(filename, content)
                is URI -> withEntry(filename, content)
                is InputStream -> withEntry(filename, content)
                is ByteArray -> withEntry(filename, content)
                else -> IllegalArgumentException("Content is not of a supported type")
            }
        }

        private fun Any.normalize(loader: Class<*>) =
            if (this is String)
                when {
                    startsWith(SCHEME_CLASSPATH_PREFIX) ->
                        loader.getResource(substring(SCHEME_CLASSPATH_PREFIX.length))
                                ?: throw IllegalArgumentException("Resource not found: "+this)
                    startsWith(SCHEME_FILE_PREFIX) -> URI.create(substring(SCHEME_FILE_PREFIX.length))
                    startsWith(SCHEME_HTTP_PREFIX) -> URI.create(substring(SCHEME_HTTP_PREFIX.length))
                    startsWith(SCHEME_HTTPS_PREFIX) -> URI.create(substring(SCHEME_HTTPS_PREFIX.length))
                    else -> this
                }
            else
                this

        private fun defaultImageTag(containerType: Class<*>) =
            IMAGE_TAG_DEFAULT_TEMPLATE.format(containerType.simpleName.toSnakeCase(), IMAGE_TAG_DYNAMIC_PLACEHOLDER)
    }

    @Throws(IOException::class)
    override fun close() = env.close()

    override fun <T: Any> create(containerType: Class<T>): T {
        val ctx = ContainerObjectContextImpl(env, containerType)
        // instance creation stage
        ctx.createInstance()
        // image preparation stage
        ctx.prepareImage()
        // container creation stage
        ctx.createContainer()
        // container started stage
        ctx.startContainer()
        // register container
        env.registerContainerObject(ctx)

        return ctx.instance!!
    }

    override fun <T: Any> destroy(containerInstance: T) {
        @Suppress("UNCHECKED_CAST")
        val ctx = env.getContainerObjectRegistration(containerInstance) as ContainerObjectContextImpl<T>
        env.unregisterContainerObject(ctx)

        // container stopping stage
        ctx.stopContainer()
        // container removing stage
        ctx.removeContainer()
        // image release stage
        ctx.teardownImage()
        // instance discarded stage
        ctx.discardInstance()
    }

    override fun <T: Any> restart(containerInstance: T) {
        @Suppress("UNCHECKED_CAST")
        val ctx = env.getContainerObjectRegistration(containerInstance) as ContainerObjectContextImpl<T>
        ctx.restartContainer()
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
