package org.dockercontainerobjects.docker.impl.dockerjava

import com.github.dockerjava.api.exception.NotFoundException
import com.github.dockerjava.core.command.BuildImageResultCallback
import com.github.dockerjava.core.command.PullImageResultCallback
import org.dockercontainerobjects.docker.ImageId
import org.dockercontainerobjects.docker.ImageLocator
import org.dockercontainerobjects.docker.ImageName
import org.dockercontainerobjects.docker.ImageNotFoundException
import org.dockercontainerobjects.docker.ImageSpec
import org.dockercontainerobjects.docker.support.AbstractDockerImagesImpl
import java.io.ByteArrayInputStream

class DockerJavaDockerImagesImpl(docker: DockerJavaDockerImpl)
        : AbstractDockerImagesImpl<DockerJavaDockerImpl>(docker) {

    override fun list(nameFilter: String?, labels: Map<String, String>?) =
            docker.client.listImagesCmd()
                    .also {
                        if (nameFilter != null) it.withImageNameFilter(nameFilter)
                        if (labels != null) it.withLabelFilter(labels)
                    }.exec()
                    .map {
                        DockerJavaModelImageInfoImpl(it)
                    }

    override fun inspect(locator: ImageLocator) =
            try {
                DockerJavaInspectImageDetailedInfoImpl(
                        docker.client.inspectImageCmd(locator.toString()).exec()
                )
            } catch (e: NotFoundException) {
                throw ImageNotFoundException(e.message, e)
            }

    override fun pull(name: ImageName) {
        try {
            docker.client.pullImageCmd(name.toString())
                    .exec(PullImageResultCallback())
                    .awaitSuccess()
        } catch (e: NotFoundException) {
            throw ImageNotFoundException(e.message, e)
        }
    }

    override fun build(spec: ImageSpec) =
        docker.client.buildImageCmd().apply {
            spec.dockerFile.let {
                if (it != null) withDockerfile(it)
            }
            spec.imageContent.let {
                if (it != null) withTarInputStream(ByteArrayInputStream(it))
            }
            if (spec.tags.isNotEmpty()) {
                withTags(spec.tags.mapTo(mutableSetOf(), ImageName::toString))
            }
            if (spec.labels.isNotEmpty()) {
                withLabels(spec.labels)
            }
            withPull(spec.pull)
        }.exec(BuildImageResultCallback())
                .awaitImageId()
                .let { ImageId(it) }

    override fun remove(locator: ImageLocator, force: Boolean) {
        try {
            docker.client.removeImageCmd(locator.toString())
                    .withForce(force)
                    .exec()
        } catch (e: NotFoundException) {
            throw ImageNotFoundException(e)
        }
    }
}
