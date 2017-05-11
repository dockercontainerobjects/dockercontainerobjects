package org.dockercontainerobjects.extensions

import static org.dockercontainerobjects.util.Predicates.nothing

import java.lang.reflect.Field
import java.util.function.Predicate
import org.dockercontainerobjects.ContainerObjectsEnvironment
import org.dockercontainerobjects.ContainerObjectsExtension
import org.dockercontainerobjects.ContainerObjectContext

class BaseContainerObjectsExtension implements ContainerObjectsExtension {

    override setupEnvironment(ContainerObjectsEnvironment env) {
    }

    override teardownEnvironment(ContainerObjectsEnvironment env) {
    }

    override <T> getFieldSelector(ContainerObjectContext<T> ctx) {
        switch (ctx.stage) {
            case INSTANCE_CREATED:
                getFieldSelectorOnInstanceCreated(ctx)
            case IMAGE_PREPARED:
                getFieldSelectorOnImagePrepared(ctx)
            case CONTAINER_CREATED:
                getFieldSelectorOnContainerCreated(ctx)
            case CONTAINER_STARTED:
                getFieldSelectorOnContainerStarted(ctx)
            case CONTAINER_STOPPED:
                getFieldSelectorOnContainerStopped(ctx)
            case CONTAINER_REMOVED:
                getFieldSelectorOnContainerRemoved(ctx)
            case IMAGE_RELEASED:
                getFieldSelectorOnImageReleased(ctx)
            case INSTANCE_DISCARDED:
                getFieldSelectorOnInstanceDiscarded(ctx)
        }
    }

    override <T> getFieldValue(ContainerObjectContext<T> ctx, Field field) {
        switch (ctx.stage) {
            case INSTANCE_CREATED:
                getFieldValueOnInstanceCreated(ctx, field)
            case IMAGE_PREPARED:
                getFieldValueOnImagePrepared(ctx, field)
            case CONTAINER_CREATED:
                getFieldValueOnContainerCreated(ctx, field)
            case CONTAINER_STARTED:
                getFieldValueOnContainerStarted(ctx, field)
            case CONTAINER_STOPPED:
                getFieldValueOnContainerStopped(ctx, field)
            case CONTAINER_REMOVED:
                getFieldValueOnContainerRemoved(ctx, field)
            case IMAGE_RELEASED:
                getFieldValueOnImageReleased(ctx, field)
            case INSTANCE_DISCARDED:
                getFieldValueOnInstanceDiscarded(ctx, field)
        }
    }

    protected def <T> Predicate<Field> getFieldSelectorOnInstanceCreated(ContainerObjectContext<T> ctx) {
        nothing
    }

    protected def <T> Object getFieldValueOnInstanceCreated(ContainerObjectContext<T> ctx, Field field) {
        null
    }

    protected def <T> Predicate<Field> getFieldSelectorOnImagePrepared(ContainerObjectContext<T> ctx) {
        nothing
    }

    protected def <T> Object getFieldValueOnImagePrepared(ContainerObjectContext<T> ctx, Field field) {
        null
    }

    protected def <T> Predicate<Field> getFieldSelectorOnContainerCreated(ContainerObjectContext<T> ctx) {
        nothing
    }

    protected def <T> Object getFieldValueOnContainerCreated(ContainerObjectContext<T> ctx, Field field) {
        null
    }

    protected def <T> Predicate<Field> getFieldSelectorOnContainerStarted(ContainerObjectContext<T> ctx) {
        nothing
    }

    protected def <T> Object getFieldValueOnContainerStarted(ContainerObjectContext<T> ctx, Field field) {
        null
    }

    protected def <T> Predicate<Field> getFieldSelectorOnContainerStopped(ContainerObjectContext<T> ctx) {
        nothing
    }

    protected def <T> Predicate<Field> getFieldValueOnContainerStopped(ContainerObjectContext<T> ctx, Field field) {
        null
    }

    protected def <T> Predicate<Field> getFieldSelectorOnContainerRemoved(ContainerObjectContext<T> ctx) {
        nothing
    }

    protected def <T> Object getFieldValueOnContainerRemoved(ContainerObjectContext<T> ctx, Field field) {
        null
    }

    protected def <T> Predicate<Field> getFieldSelectorOnImageReleased(ContainerObjectContext<T> ctx) {
        nothing
    }

    protected def <T> Object getFieldValueOnImageReleased(ContainerObjectContext<T> ctx, Field field) {
        null
    }

    protected def <T> Predicate<Field> getFieldSelectorOnInstanceDiscarded(ContainerObjectContext<T> ctx) {
        nothing
    }

    protected def <T> Object getFieldValueOnInstanceDiscarded(ContainerObjectContext<T> ctx, Field field) {
        null
    }
}
