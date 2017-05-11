package org.dockercontainerobjects.extensions

import static extension org.dockercontainerobjects.docker.DockerClientExtensions.inetAddressOfType
import static extension org.dockercontainerobjects.util.Predicates.operator_and
import static extension org.dockercontainerobjects.util.Predicates.operator_or

import static org.dockercontainerobjects.util.Fields.annotatedWith
import static org.dockercontainerobjects.util.Fields.ofType

import java.lang.reflect.Field
import java.net.InetAddress
import java.util.function.Predicate
import org.dockercontainerobjects.ContainerObjectContext
import org.dockercontainerobjects.annotations.ContainerAddress

class ContainerAddressInjectorExtension extends BaseContainerObjectsExtension {

    private static val Predicate<Field> FIELD_SELECTOR = (ofType(String) || ofType(InetAddress)) && annotatedWith(ContainerAddress)

    override <T> getFieldSelectorOnContainerStarted(ContainerObjectContext<T> ctx) {
        FIELD_SELECTOR
    }

    override <T> getFieldSelectorOnContainerStopped(ContainerObjectContext<T> ctx) {
        FIELD_SELECTOR
    }

    override <T> getFieldValueOnContainerStarted(ContainerObjectContext<T> ctx, Field field) {
        ctx.networkSettings.inetAddressOfType(field.type)
    }
}
