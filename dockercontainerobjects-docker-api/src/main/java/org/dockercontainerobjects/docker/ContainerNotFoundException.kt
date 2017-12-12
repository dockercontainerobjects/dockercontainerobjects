package org.dockercontainerobjects.docker

class ContainerNotFoundException(
        message: String? = null,
        cause: Exception? = null
): RuntimeException(message, cause) {

    constructor(cause: Exception): this(cause.message, cause)
}
