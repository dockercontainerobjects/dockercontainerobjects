package org.dockercontainerobjects.docker.impl.dockerjava

import org.dockercontainerobjects.docker.ImageId
import org.dockercontainerobjects.docker.ImageName
import java.time.Instant

const val ENVREF_NAME = "TOMCAT_VERSION"
const val ENVREF_VALUE = "8.5.12"

const val UPSTREAM_IMAGE_NAME_VALUE = "tomcat:${ENVREF_VALUE}-jre8-alpine"
const val UPSTREAM_IMAGE_ID_VALUE = "sha256:cc88dcd982a0adb0813f78ee741d9409bb1574431e0727f95abb32b54d2480e7"
const val UPSTREAM_IMAGE_OS = "linux"
const val UPSTREAM_IMAGE_ARCHITECTURE = "amd64"
const val UPSTREAM_IMAGE_SIZE = 110694282L
const val UPSTREAM_IMAGE_AUTHOR = ""
const val UPSTREAM_IMAGE_CREATED_VALUE = "2017-03-16T19:04:59Z"

const val DOCKERFILE_FILENAME = "Dockerfile"

const val ENTRYPOINT = "java"
const val CMD_ARG1 = "-version"

@JvmField val UPSTREAM_IMAGE_NAME = ImageName(UPSTREAM_IMAGE_NAME_VALUE)
@JvmField val UPSTREAM_IMAGE_ID = ImageId(UPSTREAM_IMAGE_ID_VALUE)
@JvmField val UPSTREAM_IMAGE_CREATED = Instant.parse(UPSTREAM_IMAGE_CREATED_VALUE)

@JvmField val DOCKERFILE_CONTENT = """
    |FROM ${UPSTREAM_IMAGE_NAME_VALUE}
    |ENTRYPOINT ["${ENTRYPOINT}"]
    |CMD ["${CMD_ARG1}"]
    |""".trimMargin()

@JvmField val NOIMAGE_NAME = ImageName("z".repeat(50))
