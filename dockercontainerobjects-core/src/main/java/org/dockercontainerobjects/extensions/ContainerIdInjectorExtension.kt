package org.dockercontainerobjects.extensions

import org.dockercontainerobjects.ContainerObjectContext
import org.dockercontainerobjects.annotations.ContainerId
import org.dockercontainerobjects.util.AccessibleObjects.annotatedWith
import org.dockercontainerobjects.util.Fields.ofType
import org.dockercontainerobjects.util.Predicates.and
import java.lang.reflect.Field

class ContainerIdInjectorExtension: BaseContainerObjectsExtension() {

    companion object {
        private val FIELD_SELECTOR = ofType<String>() and annotatedWith<Field>(ContainerId::class)
    }

    override fun <T: Any> getFieldSelectorOnContainerCreated(ctx: ContainerObjectContext<T>) = FIELD_SELECTOR

    override fun <T: Any> getFieldSelectorOnContainerRemoved(ctx: ContainerObjectContext<T>) = FIELD_SELECTOR

    override fun <T: Any> getFieldValueOnContainerCreated(ctx: ContainerObjectContext<T>, field: Field) =
            ctx.containerId
}
