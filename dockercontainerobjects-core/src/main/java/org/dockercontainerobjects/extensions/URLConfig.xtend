package org.dockercontainerobjects.extensions

import java.lang.^annotation.Documented
import java.lang.^annotation.Inherited
import java.lang.^annotation.Retention
import java.lang.^annotation.Target
import org.dockercontainerobjects.ContainerObjectsManager

@Documented
@Retention(RUNTIME)
@Target(FIELD)
@Inherited
annotation URLConfig {
    String value = ""
    String scheme = ContainerObjectsManager.SCHEME_HTTP
    int port = 8080
    String path = ""
}