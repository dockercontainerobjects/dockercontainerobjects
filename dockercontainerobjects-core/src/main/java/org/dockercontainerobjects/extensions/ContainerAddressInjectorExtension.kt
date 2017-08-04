package org.dockercontainerobjects.extensions

import org.dockercontainerobjects.ContainerObjectContext
import org.dockercontainerobjects.annotations.ContainerAddress
import org.dockercontainerobjects.docker.inetAddressOfType
import org.dockercontainerobjects.util.annotatedWith
import org.dockercontainerobjects.util.ofOneType
import org.dockercontainerobjects.util.and
import java.lang.reflect.Field
import java.net.InetAddress

class ContainerAddressInjectorExtension: BaseContainerObjectsExtension() {

    companion object {
        private val FIELD_SELECTOR = ofOneType(String::class, InetAddress::class) and
                annotatedWith<Field>(ContainerAddress::class)
    }

    override fun <T: Any> getFieldSelectorOnContainerStarted(ctx: ContainerObjectContext<T>) = FIELD_SELECTOR

    override fun <T: Any> getFieldSelectorOnContainerStopped(ctx: ContainerObjectContext<T>) = FIELD_SELECTOR

    override fun <T: Any> getFieldValueOnContainerStarted(ctx: ContainerObjectContext<T>, field: Field) =
        ctx.networkSettings?.inetAddressOfType(field.type)
}
