package org.dockercontainerobjects.extensions

import com.github.dockerjava.api.DockerClient
import org.dockercontainerobjects.ContainerObjectContext
import org.dockercontainerobjects.ContainerObjectsEnvironment
import org.dockercontainerobjects.ContainerObjectsManager
import org.dockercontainerobjects.util.Fields.ofOneType
import java.lang.reflect.Field
import java.net.Proxy

class GlobalObjectsInjectorExtension: BaseContainerObjectsExtension() {

    companion object {
        private val FIELD_SELECTOR =
                ofOneType(ContainerObjectsEnvironment::class, ContainerObjectsManager::class, DockerClient::class, Proxy::class)
    }

    override fun <T: Any> getFieldSelectorOnInstanceCreated(ctx: ContainerObjectContext<T>) = FIELD_SELECTOR

    override fun <T: Any> getFieldSelectorOnInstanceDiscarded(ctx: ContainerObjectContext<T>) = FIELD_SELECTOR

    override fun <T: Any> getFieldValueOnInstanceCreated(ctx: ContainerObjectContext<T>, field: Field): Any {
        return when (field.type.kotlin) {
            ContainerObjectsEnvironment::class -> ctx.environment
            ContainerObjectsManager::class -> ctx.environment.manager
            DockerClient::class -> ctx.environment.dockerClient
            Proxy::class -> ctx.environment.dockerNetworkProxy
            else -> throw IllegalStateException()
        }
    }
}
