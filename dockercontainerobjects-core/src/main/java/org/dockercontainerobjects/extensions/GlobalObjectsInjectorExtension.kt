package org.dockercontainerobjects.extensions

import org.dockercontainerobjects.ContainerObjectContext
import org.dockercontainerobjects.ContainerObjectsEnvironment
import org.dockercontainerobjects.ContainerObjectsManager
import org.dockercontainerobjects.docker.Docker
import org.dockercontainerobjects.util.ofOneType
import java.lang.reflect.Field
import java.net.Proxy
import javax.net.SocketFactory

class GlobalObjectsInjectorExtension: BaseContainerObjectsExtension() {

    companion object {
        private val FIELD_SELECTOR =
                ofOneType(ContainerObjectsEnvironment::class, ContainerObjectsManager::class, Docker::class, Proxy::class, SocketFactory::class)
    }

    override fun <T: Any> getFieldSelectorOnInstanceCreated(ctx: ContainerObjectContext<T>) = FIELD_SELECTOR

    override fun <T: Any> getFieldSelectorOnInstanceDiscarded(ctx: ContainerObjectContext<T>) = FIELD_SELECTOR

    override fun <T: Any> getFieldValueOnInstanceCreated(ctx: ContainerObjectContext<T>, field: Field): Any? =
            when (field.type.kotlin) {
                ContainerObjectsEnvironment::class -> ctx.environment
                ContainerObjectsManager::class -> ctx.environment.manager
                Docker::class -> ctx.environment.docker
                Proxy::class -> ctx.environment.dockerNetworkProxy
                SocketFactory::class -> ProxiedInetSocketFactory(ctx.environment.dockerNetworkProxy)
                else -> throw IllegalStateException()
            }
}
