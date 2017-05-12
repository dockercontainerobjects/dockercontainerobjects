package org.dockercontainerobjects.extensions

import static extension org.dockercontainerobjects.docker.DockerClientExtensions.inetAddress
import static extension org.dockercontainerobjects.util.Predicates.operator_and

import static org.dockercontainerobjects.util.Fields.annotatedWith
import static org.dockercontainerobjects.util.Fields.ofType

import java.lang.reflect.Field
import java.net.URL
import java.util.function.Predicate
import org.dockercontainerobjects.ContainerObjectContext

class ContainerURLInjectorExtension extends BaseContainerObjectsExtension {

    public static val HOST_DYNAMIC_PLACEHOLDER = "*"

    private static val Predicate<Field> FIELD_SELECTOR = ofType(URL) && annotatedWith(URLConfig)

    override <T> getFieldSelectorOnContainerStarted(ContainerObjectContext<T> ctx) {
        FIELD_SELECTOR
    }

    override <T> getFieldSelectorOnContainerStopped(ContainerObjectContext<T> ctx) {
        FIELD_SELECTOR
    }

    override <T> getFieldValueOnContainerStarted(ContainerObjectContext<T> ctx, Field field) {
        val config = field.getAnnotation(URLConfig)
        val addr = ctx.networkSettings.inetAddress.hostAddress
        if (config.value !== null && !config.value.empty)
            new URL(config.value.replace(HOST_DYNAMIC_PLACEHOLDER, addr))
        else
            new URL(config.scheme, addr, config.port, config.path)
    }
}
