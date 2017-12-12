package org.dockercontainerobjects.extensions

import org.dockercontainerobjects.ContainerObjectContext
import org.dockercontainerobjects.annotations.ContainerAddress
import org.dockercontainerobjects.util.and
import org.dockercontainerobjects.util.annotatedWith
import org.dockercontainerobjects.util.ofOneType
import java.lang.reflect.Field
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

class ContainerAddressInjectorExtension: BaseContainerObjectsExtension() {

    companion object {
        private val FIELD_SELECTOR = ofOneType(String::class, InetAddress::class) and
                annotatedWith<Field>(ContainerAddress::class)
    }

    override fun <T : Any> getFieldSelectorOnContainerStarted(ctx: ContainerObjectContext<T>) = FIELD_SELECTOR

    override fun <T : Any> getFieldSelectorOnContainerStopped(ctx: ContainerObjectContext<T>) = FIELD_SELECTOR

    override fun <T : Any> getFieldValueOnContainerStarted(ctx: ContainerObjectContext<T>, field: Field): Any? {
        val addresses = ctx.networkSettings?.addresses ?: return null
        val type = field.type.kotlin
        return when {
            type == String::class -> addresses.preferred.hostAddress
            type == Inet4Address::class -> addresses.ip4
            type == Inet6Address::class -> addresses.ip6
            type != InetAddress::class -> throw IllegalStateException("unsupported type")
            else -> addresses.preferred
        }
    }
}
