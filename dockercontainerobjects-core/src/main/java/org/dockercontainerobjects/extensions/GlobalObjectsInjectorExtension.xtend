package org.dockercontainerobjects.extensions

import static org.dockercontainerobjects.util.Fields.ofOneType

import java.lang.reflect.Field
import java.net.Proxy
import java.util.function.Predicate
import com.github.dockerjava.api.DockerClient
import org.dockercontainerobjects.ContainerObjectContext
import org.dockercontainerobjects.ContainerObjectsEnvironment
import org.dockercontainerobjects.ContainerObjectsManager

class GlobalObjectsInjectorExtension extends BaseContainerObjectsExtension {

    static val Predicate<Field> FIELD_SELECTOR = ofOneType(ContainerObjectsEnvironment, ContainerObjectsManager, DockerClient, Proxy)

    override <T> getFieldSelectorOnInstanceCreated(ContainerObjectContext<T> ctx) {
        FIELD_SELECTOR
    }

    override <T> getFieldSelectorOnInstanceDiscarded(ContainerObjectContext<T> ctx) {
        FIELD_SELECTOR
    }

    override <T> getFieldValueOnInstanceCreated(ContainerObjectContext<T> ctx, Field field) {
        switch (field.type) {
            case ContainerObjectsEnvironment:
                ctx.environment
            case ContainerObjectsManager:
                ctx.environment.manager
            case DockerClient:
                ctx.environment.dockerClient
            case Proxy:
                ctx.environment.dockerNetworkProxy
        }
    }
}
