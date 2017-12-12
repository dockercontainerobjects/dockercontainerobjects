package org.dockercontainerobjects.docker.impl.dockerjava

import com.github.dockerjava.api.exception.NotFoundException
import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.api.model.StreamType.STDERR
import com.github.dockerjava.api.model.StreamType.STDOUT
import com.github.dockerjava.core.async.ResultCallbackTemplate
import com.github.dockerjava.core.command.WaitContainerResultCallback
import org.dockercontainerobjects.docker.ContainerId
import org.dockercontainerobjects.docker.ContainerLocator
import org.dockercontainerobjects.docker.ContainerLogEntryContext
import org.dockercontainerobjects.docker.ContainerLogSpec
import org.dockercontainerobjects.docker.ContainerNotFoundException
import org.dockercontainerobjects.docker.ContainerSpec
import org.dockercontainerobjects.docker.ContainerStatus
import org.dockercontainerobjects.docker.ImageLocator
import org.dockercontainerobjects.docker.ImageNotFoundException
import org.dockercontainerobjects.docker.support.AbstractDockerContainersImpl
import java.io.Closeable
import java.time.Duration
import java.time.Instant
import javax.ws.rs.ProcessingException

class DockerJavaDockerContainersImpl(docker: DockerJavaDockerImpl)
        : AbstractDockerContainersImpl<DockerJavaDockerImpl>(docker) {

    override fun list(
            nameFilter: String?,
            idFilter: String?,
            fromImage: ImageLocator?,
            status: ContainerStatus?,
            labels: Map<String, String>?,
            includeAll: Boolean
    ) =
            docker.client.listContainersCmd()
                    .also {
                        if (nameFilter != null) {
                            (it.filters as MutableMap<String, List<String>>)["name"] = listOf(nameFilter)
                        }
                        if (idFilter != null) {
                            (it.filters as MutableMap<String, List<String>>)["id"] = listOf(idFilter)
                        }
                        if (status != null) it.withStatusFilter(status.name.toLowerCase())
                        if (labels != null) it.withLabelFilter(labels)
                        it.withShowAll(includeAll)
                    }.exec()
                    .map {
                        DockerJavaModelContainerInfoImpl(it)
                    }

    override fun inspect(locator: ContainerLocator) =
            try {
                DockerJavaInspectContainerDetailedInfoImpl(
                        docker.client.inspectContainerCmd(locator.toString()).exec()
                )
            } catch (e: NotFoundException) {
                throw ContainerNotFoundException(e.message, e)
            }

    override fun create(spec: ContainerSpec) =
            try {
                docker.client.createContainerCmd(spec.image.toString()).apply {
                    spec.name.let {
                        if (it != null) withName(it)
                    }
                    if (spec.labels.isNotEmpty()) {
                        withLabels(spec.labels)
                    }
                    if (spec.environment.isNotEmpty()) {
                        withEnv(spec.environment.map { (name, value) -> "$name=$value" })
                    }
                    spec.entrypoint.let {
                        if (it.isNotEmpty()) withEntrypoint(it)
                    }
                    spec.cmd.let {
                        if (it.isNotEmpty()) withCmd(it)
                    }
                }.exec().id.let { ContainerId(it) }
            } catch (e: NotFoundException) {
                throw ImageNotFoundException(e)
            }

    override fun start(locator: ContainerLocator) {
        try {
            docker.client.startContainerCmd(locator.toString()).exec()
        } catch (e: NotFoundException) {
            throw ContainerNotFoundException(e)
        }
    }

    override fun stop(locator: ContainerLocator): Int =
        try {
            docker.client.stopContainerCmd(locator.toString()).exec()
            docker.client.waitContainerCmd(locator.toString())
                    .exec(WaitContainerResultCallback())
                    .awaitStatusCode()
        } catch (e: NotFoundException) {
            throw ContainerNotFoundException(e)
        }

    override fun restart(locator: ContainerLocator) {
        try {
            docker.client.restartContainerCmd(locator.toString()).exec()
        } catch (e: NotFoundException) {
            throw ContainerNotFoundException(e)
        }
    }

    override fun pause(locator: ContainerLocator) {
        try {
            docker.client.pauseContainerCmd(locator.toString()).exec()
        } catch (e: NotFoundException) {
            throw ContainerNotFoundException(e)
        }
    }

    override fun unpause(locator: ContainerLocator) {
        try {
            docker.client.unpauseContainerCmd(locator.toString()).exec()
        } catch (e: NotFoundException) {
            throw ContainerNotFoundException(e)
        }
    }

    override fun remove(locator: ContainerLocator, force: Boolean, removeVolumes: Boolean) {
        try {
            docker.client.removeContainerCmd(locator.toString())
                    .withForce(force)
                    .withRemoveVolumes(removeVolumes)
                    .exec()
        } catch (e: NotFoundException) {
            throw ContainerNotFoundException(e)
        }
    }

    override fun logs(locator: ContainerLocator, spec: ContainerLogSpec) {
        try {
            docker.client.logContainerCmd(locator.toString())
                    .withStdOut(spec.standartOutputIncluded)
                    .withStdErr(spec.standarErrorIncluded)
                    .withSince(Duration.between(Instant.EPOCH, spec.since).seconds.toInt())
                    .withTimestamps(spec.timestampsIncluded)
                    .withFollowStream(true)
                    .withTailAll()
                    .exec(AdapterResultCallback(spec))
        } catch (e: NotFoundException) {
            throw ContainerNotFoundException(e)
        }
    }

    class AdapterResultCallback(
            private val spec: ContainerLogSpec
    ): ResultCallbackTemplate<AdapterResultCallback, Frame>() {

        override fun onStart(stream: Closeable?) {
            super.onStart(stream)
            spec.logStartHandler()
        }

        override fun onNext(frame: Frame) {
            spec.logEntryHandler(FrameLogEntryContext(frame, this))
        }

        override fun onComplete() {
            super.onComplete()
            spec.logDoneHandler()
        }
    }

    class FrameLogEntryContext(
            private val frame: Frame,
            private val adapter: AdapterResultCallback
    ): ContainerLogEntryContext {

        override fun stop() {
            try {
                adapter.close()
            } catch (e: ProcessingException) {
                // ignore any errors closing the log
            }
        }

        override val bytes: ByteArray
            get() = frame.payload
        override val fromStandardOutput: Boolean
            get() = frame.streamType == STDOUT
        override val fromStandardError: Boolean
            get() = frame.streamType == STDERR

    }
}
