package org.dockercontainerobjects.extensions

import static org.dockercontainerobjects.util.Fields.ofType

import java.lang.reflect.Field
import java.util.function.Predicate
import org.dockercontainerobjects.ContainerObjectContext
import com.github.dockerjava.api.model.NetworkSettings

class NetworkSettingsInjectorExtension extends BaseContainerObjectsExtension {

    static val Predicate<Field> FIELD_SELECTOR = ofType(NetworkSettings)

    override <T> getFieldSelectorOnContainerCreated(ContainerObjectContext<T> ctx) {
        FIELD_SELECTOR
    }

    override <T> getFieldSelectorOnContainerRemoved(ContainerObjectContext<T> ctx) {
        FIELD_SELECTOR
    }

    override <T> getFieldValueOnInstanceCreated(ContainerObjectContext<T> ctx, Field field) {
        ctx.networkSettings
    }
}
