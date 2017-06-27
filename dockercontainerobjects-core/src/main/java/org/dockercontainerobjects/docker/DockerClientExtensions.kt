package org.dockercontainerobjects.docker

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.CreateContainerResponse
import com.github.dockerjava.api.command.InspectContainerResponse
import com.github.dockerjava.api.command.InspectImageResponse
import com.github.dockerjava.api.exception.NotFoundException
import com.github.dockerjava.api.exception.NotModifiedException
import com.github.dockerjava.api.model.NetworkSettings
import com.github.dockerjava.core.command.BuildImageResultCallback
import com.github.dockerjava.core.command.PullImageResultCallback
import com.github.dockerjava.core.command.WaitContainerResultCallback
import org.dockercontainerobjects.util.debug
import org.dockercontainerobjects.util.loggerFor
import org.dockercontainerobjects.util.warn
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.UnknownHostException
import java.security.AccessController
import java.security.PrivilegedAction
import kotlin.reflect.KClass

object DockerClientExtensions {

    private val l = loggerFor<DockerClientExtensions>()

    @Throws(NotFoundException::class)
    @JvmStatic
    fun DockerClient.inspectImage(imageId: String): InspectImageResponse {
        l.debug { "Inspecting docker image with id '$imageId'" }
        val response = inspectImageCmd(imageId).exec()
        return response
    }

    @JvmStatic
    fun DockerClient.isImageAvailable(imageId: String): Boolean {
        return try {
            inspectImage(imageId)
            true
        } catch (e:NotFoundException) {
            false
        }
    }

    @JvmStatic
    fun DockerClient.pullImage(imageId: String): InspectImageResponse {
        pullImageCmd(imageId)
                .exec(PullImageResultCallback())
                .awaitSuccess()
        return inspectImage(imageId)
    }

    @Throws(NotFoundException::class)
    @JvmStatic
    fun DockerClient.removeImage(imageId: String) {
        removeImageCmd(imageId).exec()
    }

    @JvmStatic
    fun DockerClient.buildImage(dockerFileOrFolder: File, imageTag: String, forcePull: Boolean): String {
        l.debug { "Building docker image with '$dockerFileOrFolder'" }
        return buildImageCmd(dockerFileOrFolder)
                .withTag(imageTag)
                .withPull(forcePull)
                .exec(BuildImageResultCallback())
                .awaitImageId()
    }

    @JvmStatic
    fun DockerClient.buildImage(dockerTar: ByteArray, imageTag: String, forcePull: Boolean): String {
        return ByteArrayInputStream(dockerTar).use {
            buildImage(it, imageTag, forcePull)
        }
    }

    @JvmStatic
    fun DockerClient.buildImage(dockerTar: InputStream, imageTag: String, forcePull: Boolean): String {
        l.debug("Building docker image from tar")
        return buildImageCmd(dockerTar)
            .withTag(imageTag)
            .withPull(forcePull)
            .exec(BuildImageResultCallback())
            .awaitImageId()
    }

    @JvmStatic
    fun DockerClient.createContainer(imageId: String, environment: List<String>): CreateContainerResponse {
        l.debug { "Creating docker container from image '$imageId'" }
        val cmd = createContainerCmd(imageId)
        if (environment.isNotEmpty())
            cmd.withEnv(environment)
        val response = cmd.exec()
        l.debug { "Docker container from image '$imageId' created with id '${response.id}'" }
        return response
    }

    @Throws(NotFoundException::class, NotModifiedException::class)
    @JvmStatic
    fun DockerClient.startContainer(containerId: String): InspectContainerResponse {
        l.debug { "Starting docker container with id '$containerId'" }
        startContainerCmd(containerId).exec()
        l.debug { "Docker container with id '$containerId' started" }
        val response = inspectContainer(containerId)
        val running = response.state?.running ?: false
        if (!running)
            throw IllegalStateException(
                    "Docker container with id '$containerId' did not start correctly. Exit code: ${response.state.exitCode}, error: ${response.state.error}")
        return response
    }

    @Throws(NotFoundException::class)
    @JvmStatic
    fun DockerClient.inspectContainer(containerId: String): InspectContainerResponse {
        l.debug { "Inspecting docker container with id '$containerId'" }
        val response = inspectContainerCmd(containerId).exec()
        l.debug { "Docker container with id '$containerId' is in state '${response.state}'" }
        return response
    }

    @Throws(NotFoundException::class, NotModifiedException::class)
    @JvmStatic
    fun DockerClient.stopContainer(containerId: String) {
        l.debug { "Stopping docker container with id '$containerId'" }
        stopContainerCmd(containerId).exec()
        l.debug { "Waiting for docker container with id '$containerId'" }
        waitContainerCmd(containerId)
            .exec(WaitContainerResultCallback())
            .awaitStatusCode()
    }

    @Throws(NotFoundException::class)
    @JvmStatic
    fun DockerClient.removeContainer(containerId: String, force: Boolean = true, removeVolumes: Boolean = true) {
        l.debug { "Removing docker container with id '$containerId'" }
        removeContainerCmd(containerId)
            .withRemoveVolumes(removeVolumes)
            .withForce(force)
            .exec()
        l.debug { "Docker container with id '$containerId' removed" }
    }

    @Suppress("deprecation")
    @JvmStatic
    fun NetworkSettings.inet4Address(): Inet4Address? {
        try {
            val addr = ipAddress
            return if (!addr.isNullOrEmpty()) InetAddress.getByName(addr) as Inet4Address else null
        } catch (e: UnknownHostException) {
            l.warn(e)
            throw RuntimeException(e)
        }
    }

    @JvmStatic
    fun NetworkSettings.inet6Address():Inet6Address? {
        try {
            val addr = globalIPv6Address
            return if (!addr.isNullOrEmpty()) InetAddress.getByName(addr) as Inet6Address else null
        } catch (e: UnknownHostException) {
            l.warn(e)
            throw RuntimeException(e)
        }
    }

    @JvmStatic
    fun NetworkSettings.inetAddress():InetAddress? {
        val action:PrivilegedAction<Boolean> = PrivilegedAction { java.lang.Boolean.getBoolean("java.net.preferIPv6Addresses") }
        val preferIPv6Addresses =
            try {
                AccessController.doPrivileged(action)
            } catch (e: SecurityException) {
                false
            }
        return if (preferIPv6Addresses)
                inet6Address() ?: inet4Address()
            else
                inet4Address() ?: inet6Address()
    }

    @JvmStatic
    inline fun <ADDR: Any> NetworkSettings.inetAddressOfType(addrType: KClass<ADDR>): ADDR? {
        @Suppress("UNCHECKED_CAST")
        return when (addrType) {
            String::class -> inetAddress()?.hostAddress
            Inet4Address::class -> inet4Address()
            Inet6Address::class -> inet6Address()
            InetAddress::class -> inetAddress()
            else -> throw IllegalArgumentException("Cannot convert address to '${addrType.java.simpleName}'")
        } as ADDR
    }

    inline fun <ADDR: Any> NetworkSettings.inetAddressOfType(addrType: Class<ADDR>): ADDR? =
            inetAddressOfType(addrType.kotlin)

    inline fun <reified ADDR: Any> NetworkSettings.inetAddressOfType(): ADDR? =
            inetAddressOfType(ADDR::class)
}
