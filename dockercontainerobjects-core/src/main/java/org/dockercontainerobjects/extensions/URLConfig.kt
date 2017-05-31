package org.dockercontainerobjects.extensions

import org.dockercontainerobjects.ContainerObjectsManager
import java.lang.annotation.Inherited

@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
@Inherited
annotation class URLConfig(
        val value: String = "",
        val scheme: String = ContainerObjectsManager.SCHEME_HTTP,
        val port: Int = 8080,
        val path: String = ""
)
