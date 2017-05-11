package org.dockercontainerobjects.extensions

import static extension org.dockercontainerobjects.util.Predicates.operator_and

import static org.dockercontainerobjects.util.Fields.annotatedWith
import static org.dockercontainerobjects.util.Fields.ofType

import java.lang.reflect.Field
import org.dockercontainerobjects.ContainerObjectContext
import org.dockercontainerobjects.annotations.ContainerId

class ContainerIdInjectorExtension extends BaseContainerObjectsExtension {

    private static val FIELD_SELECTOR = ofType(String) && annotatedWith(ContainerId)

    override <T> getFieldSelectorOnContainerCreated(ContainerObjectContext<T> ctx) {
        FIELD_SELECTOR
    }

    override <T> getFieldSelectorOnContainerRemoved(ContainerObjectContext<T> ctx) {
        FIELD_SELECTOR
    }

    override <T> getFieldValueOnContainerCreated(ContainerObjectContext<T> ctx, Field field) {
        ctx.containerId
    }
}
