package org.dockercontainerobjects.extensions

import com.github.dockerjava.api.model.NetworkSettings
import org.dockercontainerobjects.ContainerObjectContext
import org.dockercontainerobjects.util.ofType
import java.lang.reflect.Field

class NetworkSettingsInjectorExtension: BaseContainerObjectsExtension() {

    companion object {
        private val FIELD_SELECTOR = ofType<NetworkSettings>()
    }

    override fun <T: Any> getFieldSelectorOnContainerCreated(ctx: ContainerObjectContext<T>) = FIELD_SELECTOR

    override fun <T: Any> getFieldSelectorOnContainerRemoved(ctx: ContainerObjectContext<T>) = FIELD_SELECTOR

    override fun <T: Any> getFieldValueOnContainerCreated(ctx: ContainerObjectContext<T>, field: Field) =
            ctx.networkSettings
}
