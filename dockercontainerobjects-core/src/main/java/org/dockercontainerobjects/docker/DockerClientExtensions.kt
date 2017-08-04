@file:JvmName("DockerClientExtensions")
@file:Suppress("NOTHING_TO_INLINE")
package org.dockercontainerobjects.docker

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.command.CreateContainerResponse
import com.github.dockerjava.api.command.InspectContainerResponse
import com.github.dockerjava.api.command.InspectImageResponse
import com.github.dockerjava.api.exception.NotFoundException
import com.github.dockerjava.api.exception.NotModifiedException
import com.github.dockerjava.api.model.Frame
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
import java.time.Duration
import java.time.Instant
import java.time.temporal.Temporal
import kotlin.reflect.KClass

private val l = loggerFor("org.dockercontainerobjects.docker.DockerClientExtensions")

@Throws(NotFoundException::class)
fun DockerClient.inspectImage(imageId: String): InspectImageResponse {
    l.debug { "Inspecting docker image with id '$imageId'" }
    val response = inspectImageCmd(imageId).exec()
    return response
}

fun DockerClient.isImageAvailable(imageId: String): Boolean {
    return try {
        inspectImage(imageId)
        true
    } catch (e:NotFoundException) {
        false
    }
}

fun DockerClient.pullImage(imageId: String): InspectImageResponse {
    pullImageCmd(imageId)
            .exec(PullImageResultCallback())
            .awaitSuccess()
    return inspectImage(imageId)
}

@Throws(NotFoundException::class)
fun DockerClient.removeImage(imageId: String) {
    removeImageCmd(imageId).exec()
}

fun DockerClient.buildImage(dockerFileOrFolder: File, imageTag: String, forcePull: Boolean): String {
    l.debug { "Building docker image with '$dockerFileOrFolder'" }
    return buildImageCmd(dockerFileOrFolder)
            .withTag(imageTag)
            .withPull(forcePull)
            .exec(BuildImageResultCallback())
            .awaitImageId()
}

fun DockerClient.buildImage(dockerTar: ByteArray, imageTag: String, forcePull: Boolean): String {
    return ByteArrayInputStream(dockerTar).use {
        buildImage(it, imageTag, forcePull)
    }
}

fun DockerClient.buildImage(dockerTar: InputStream, imageTag: String, forcePull: Boolean): String {
    l.debug("Building docker image from tar")
    return buildImageCmd(dockerTar)
        .withTag(imageTag)
        .withPull(forcePull)
        .exec(BuildImageResultCallback())
        .awaitImageId()
}

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
fun DockerClient.inspectContainer(containerId: String): InspectContainerResponse {
    l.debug { "Inspecting docker container with id '$containerId'" }
    val response = inspectContainerCmd(containerId).exec()
    l.debug { "Docker container with id '$containerId' is in state '${response.state}'" }
    return response
}

fun <T: ResultCallback<Frame>> DockerClient.fetchContainerLogs(
        containerId: String, since: Temporal = Instant.EPOCH,
        includeStdOut: Boolean = true, includeStdErr: Boolean = true, includeTimestamps: Boolean = false, callback: T) {
    logContainerCmd(containerId)
            .withStdOut(includeStdOut)
            .withStdErr(includeStdErr)
            .withSince(Duration.between(Instant.EPOCH, since).seconds.toInt())
            .withTimestamps(includeTimestamps)
            .withFollowStream(true)
            .withTailAll()
            .exec(callback)
}

@Throws(NotFoundException::class, NotModifiedException::class)
fun DockerClient.stopContainer(containerId: String) {
    l.debug { "Stopping docker container with id '$containerId'" }
    stopContainerCmd(containerId).exec()
    l.debug { "Waiting for docker container with id '$containerId'" }
    waitContainerCmd(containerId)
        .exec(WaitContainerResultCallback())
        .awaitStatusCode()
}

@Throws(NotFoundException::class)
fun DockerClient.removeContainer(containerId: String, force: Boolean = true, removeVolumes: Boolean = true) {
    l.debug { "Removing docker container with id '$containerId'" }
    removeContainerCmd(containerId)
        .withRemoveVolumes(removeVolumes)
        .withForce(force)
        .exec()
    l.debug { "Docker container with id '$containerId' removed" }
}

@Suppress("deprecation")
fun NetworkSettings.inet4Address(): Inet4Address? {
    try {
        val addr = ipAddress
        return if (!addr.isNullOrEmpty()) InetAddress.getByName(addr) as Inet4Address else null
    } catch (e: UnknownHostException) {
        l.warn(e)
        throw RuntimeException(e)
    }
}

fun NetworkSettings.inet6Address():Inet6Address? {
    try {
        val addr = globalIPv6Address
        return if (!addr.isNullOrEmpty()) InetAddress.getByName(addr) as Inet6Address else null
    } catch (e: UnknownHostException) {
        l.warn(e)
        throw RuntimeException(e)
    }
}

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
