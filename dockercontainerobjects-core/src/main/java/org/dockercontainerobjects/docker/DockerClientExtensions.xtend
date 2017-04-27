package org.dockercontainerobjects.docker

import static extension org.dockercontainerobjects.util.Loggers.debug
import static extension org.dockercontainerobjects.util.Loggers.warn
import static extension org.dockercontainerobjects.util.Strings.operator_tripleLessThan

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.File
import java.net.InetAddress
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.UnknownHostException
import java.security.AccessController
import java.security.PrivilegedAction
import java.util.List
import java.util.Optional
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.exception.NotFoundException
import com.github.dockerjava.api.model.NetworkSettings
import com.github.dockerjava.core.command.BuildImageResultCallback
import com.github.dockerjava.core.command.PullImageResultCallback
import com.github.dockerjava.core.command.WaitContainerResultCallback
import org.slf4j.LoggerFactory

class DockerClientExtensions {

    private static val l = LoggerFactory.getLogger(DockerClientExtensions)

    static def inspectImage(DockerClient dockerClient, String imageId) throws NotFoundException {
        l.debug [ "Inspecting docker image with id '%s'" <<< imageId ]
        val response = dockerClient.inspectImageCmd(imageId).exec
        l.debug [ "Docker image with id '%s' created on '%s'" <<< #[imageId, response.created] ]
        response
    }

    static def isImageAvailable(DockerClient dockerClient, String imageId) {
        try {
            dockerClient.inspectImage(imageId)
            true
        } catch (NotFoundException e) {
            false
        }
    }

    static def pullImage(DockerClient dockerClient, String imageId) {
        dockerClient.pullImageCmd(imageId)
            .exec(new PullImageResultCallback)
            .awaitSuccess
        dockerClient.inspectImage(imageId)
    }

    static def removeImage(DockerClient dockerClient, String imageId) throws NotFoundException {
        dockerClient.removeImageCmd(imageId)
            .exec
    }

    static def String buildImage(DockerClient dockerClient, File dockerFileOrFolder, String imageTag, boolean forcePull) {
        l.debug [ "Building docker image with '%s'" <<< dockerFileOrFolder ]
        dockerClient.buildImageCmd(dockerFileOrFolder)
            .withTag(imageTag)
            .withPull(forcePull)
            .exec(new BuildImageResultCallback)
            .awaitImageId
    }

    static def String buildImage(DockerClient dockerClient, byte[] dockerTar, String imageTag, boolean forcePull) {
        val in = new ByteArrayInputStream(dockerTar)
        try {
            dockerClient.buildImage(in, imageTag, forcePull)
        } finally {
            in.close
        }
    }

    static def String buildImage(DockerClient dockerClient, InputStream dockerTar, String imageTag, boolean forcePull) {
        l.debug [ "Building docker image from tar" ]
        dockerClient.buildImageCmd(dockerTar)
            .withTag(imageTag)
            .withPull(forcePull)
            .exec(new BuildImageResultCallback)
            .awaitImageId
    }

    static def createContainer(DockerClient dockerClient, String imageId, Optional<List<String>> environment) {
        l.debug [ "Creating docker container from image '%s'" <<< imageId ]
        val cmd = dockerClient.createContainerCmd(imageId)
        environment.ifPresent[ cmd.withEnv(it) ]
        val response = cmd.exec
        l.debug [ "Docker container from image '%s' created with id '%s'" <<< #[imageId, response.id] ]
        response
    }

    static def startContainer(DockerClient dockerClient, String containerId) {
        l.debug [ "Starting docker container with id '%s'" <<< containerId ]
        dockerClient.startContainerCmd(containerId).exec
        l.debug [ "Docker container with id '%s' started" <<< containerId ]
        val response = dockerClient.inspectContainer(containerId)
        if (response.state.running != Boolean.TRUE)
            throw new IllegalStateException(
                    "Docker container with id '%s' did not start correctly. Exit code: %d, error: %s" <<<
                            #[containerId, response.state.exitCode, response.state.error ])
        response
    }

    static def inspectContainer(DockerClient dockerClient, String containerId) throws NotFoundException {
        l.debug [ "Inspecting docker container with id '%s'" <<< containerId ]
        val response = dockerClient.inspectContainerCmd(containerId).exec
        l.debug [ "Docker container with id '%s' is in state '%s'" <<< #[containerId, response.state] ]
        response
    }

    static def stopContainer(DockerClient dockerClient, String containerId) {
        l.debug [ "Stopping docker container with id '%s'" <<< containerId ]
        dockerClient.stopContainerCmd(containerId).exec
        l.debug [ "Waiting for docker container with id '%s'" <<< containerId ]
        dockerClient.waitContainerCmd(containerId)
            .exec(new WaitContainerResultCallback)
            .awaitStatusCode.intValue
    }

    static def void removeContainer(DockerClient dockerClient, String containerId, boolean force, boolean removeVolumes) {
        l.debug [ "Removing docker container with id '%s'" <<< containerId ]
        dockerClient.removeContainerCmd(containerId)
            .withRemoveVolumes(removeVolumes)
            .withForce(force)
            .exec
        l.debug [ "Docker container with id '%s' removed" <<< containerId ]
    }

    static def void removeContainer(DockerClient dockerClient, String containerId) {
        removeContainer(dockerClient, containerId, true, true)
    }

    @SuppressWarnings("deprecation")
    static def Inet4Address inet4Address(NetworkSettings networkSettings) {
        try {
            val addr = networkSettings.ipAddress
            if (addr !== null && !addr.empty) InetAddress.getByName(addr) as Inet4Address else null
        } catch (UnknownHostException e) {
            l.warn(e)
            throw new RuntimeException(e)
        }
    }

    static def Inet6Address inet6Address(NetworkSettings networkSettings) {
        try {
            val addr = networkSettings.globalIPv6Address
            if (addr !== null && !addr.empty) InetAddress.getByName(addr) as Inet6Address else null
        } catch (UnknownHostException e) {
            l.warn(e)
            throw new RuntimeException(e)
        }
    }

    static def inetAddress(NetworkSettings networkSettings) {
        val PrivilegedAction<Boolean> action = [ Boolean.getBoolean("java.net.preferIPv6Addresses") ]
        val preferIPv6Addresses =
            try {
                AccessController.doPrivileged(action).booleanValue
            } catch (SecurityException e) {
                false
            }
        if (preferIPv6Addresses)
            networkSettings.inet6Address ?: networkSettings.inet4Address
        else
            networkSettings.inet4Address ?: networkSettings.inet6Address
    }

    static def <ADDR> ADDR inetAddressOfType(NetworkSettings networkSettings, Class<ADDR> type) {
        switch (type) {
            case String: networkSettings.inetAddress.hostAddress
            case Inet4Address: networkSettings.inet4Address
            case Inet6Address: networkSettings.inet6Address
            case InetAddress: networkSettings.inetAddress
            default: throw new IllegalArgumentException("Cannot convert address to '%s'" <<< type.simpleName)
        } as ADDR
    }
}
