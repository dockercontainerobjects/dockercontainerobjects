package org.dockercontainerobjects.extensions

import org.dockercontainerobjects.ContainerObjectContext
import org.dockercontainerobjects.annotations.ContainerId
import org.dockercontainerobjects.docker.ContainerLocator
import org.dockercontainerobjects.util.and
import org.dockercontainerobjects.util.annotatedWith
import org.dockercontainerobjects.util.ofType
import org.dockercontainerobjects.util.or
import java.lang.reflect.Field

class ContainerIdInjectorExtension: BaseContainerObjectsExtension() {

    companion object {
        private val FIELD_SELECTOR = (ofType<String>() or ofType<ContainerLocator>()) and annotatedWith<Field>(ContainerId::class)
    }

    override fun <T: Any> getFieldSelectorOnContainerCreated(ctx: ContainerObjectContext<T>) = FIELD_SELECTOR

    override fun <T: Any> getFieldSelectorOnContainerRemoved(ctx: ContainerObjectContext<T>) = FIELD_SELECTOR

    override fun <T: Any> getFieldValueOnContainerCreated(ctx: ContainerObjectContext<T>, field: Field): Any? =
            when (field.type.kotlin) {
                ContainerLocator::class -> ctx.container
                String::class -> ctx.container?.toString()
                else -> throw IllegalStateException()
            }
}
