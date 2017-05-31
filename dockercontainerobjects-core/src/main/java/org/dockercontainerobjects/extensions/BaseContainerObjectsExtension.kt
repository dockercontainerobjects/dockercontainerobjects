package org.dockercontainerobjects.extensions

import org.dockercontainerobjects.ContainerObjectContext
import org.dockercontainerobjects.ContainerObjectLifecycleStage.CONTAINER_CREATED
import org.dockercontainerobjects.ContainerObjectLifecycleStage.CONTAINER_REMOVED
import org.dockercontainerobjects.ContainerObjectLifecycleStage.CONTAINER_STARTED
import org.dockercontainerobjects.ContainerObjectLifecycleStage.CONTAINER_STOPPED
import org.dockercontainerobjects.ContainerObjectLifecycleStage.IMAGE_PREPARED
import org.dockercontainerobjects.ContainerObjectLifecycleStage.IMAGE_RELEASED
import org.dockercontainerobjects.ContainerObjectLifecycleStage.INSTANCE_CREATED
import org.dockercontainerobjects.ContainerObjectLifecycleStage.INSTANCE_DISCARDED
import org.dockercontainerobjects.ContainerObjectsEnvironment
import org.dockercontainerobjects.ContainerObjectsExtension
import org.dockercontainerobjects.util.Predicates.nothing
import java.lang.reflect.Field
import java.util.function.Predicate

open class BaseContainerObjectsExtension: ContainerObjectsExtension {

    override fun setupEnvironment(env: ContainerObjectsEnvironment) {}
    override fun teardownEnvironment(env: ContainerObjectsEnvironment) {}

    override fun <T: Any> getFieldSelector(ctx: ContainerObjectContext<T>) =
        when (ctx.stage) {
            INSTANCE_CREATED -> getFieldSelectorOnInstanceCreated(ctx)
            IMAGE_PREPARED -> getFieldSelectorOnImagePrepared(ctx)
            CONTAINER_CREATED -> getFieldSelectorOnContainerCreated(ctx)
            CONTAINER_STARTED -> getFieldSelectorOnContainerStarted(ctx)
            CONTAINER_STOPPED -> getFieldSelectorOnContainerStopped(ctx)
            CONTAINER_REMOVED -> getFieldSelectorOnContainerRemoved(ctx)
            IMAGE_RELEASED -> getFieldSelectorOnImageReleased(ctx)
            INSTANCE_DISCARDED -> getFieldSelectorOnInstanceDiscarded(ctx)
        }

    override fun <T: Any> getFieldValue(ctx: ContainerObjectContext<T>, field: Field) =
        when (ctx.stage) {
            INSTANCE_CREATED -> getFieldValueOnInstanceCreated(ctx, field)
            IMAGE_PREPARED -> getFieldValueOnImagePrepared(ctx, field)
            CONTAINER_CREATED -> getFieldValueOnContainerCreated(ctx, field)
            CONTAINER_STARTED -> getFieldValueOnContainerStarted(ctx, field)
            CONTAINER_STOPPED -> getFieldValueOnContainerStopped(ctx, field)
            CONTAINER_REMOVED -> getFieldValueOnContainerRemoved(ctx, field)
            IMAGE_RELEASED -> getFieldValueOnImageReleased(ctx, field)
            INSTANCE_DISCARDED -> getFieldValueOnInstanceDiscarded(ctx, field)
        }

    protected open fun <T: Any> getFieldSelectorOnInstanceCreated(ctx: ContainerObjectContext<T>): Predicate<Field>? = nothing()
    protected open fun <T: Any> getFieldValueOnInstanceCreated(ctx: ContainerObjectContext<T>, field: Field): Any? = null

    protected open fun <T: Any> getFieldSelectorOnImagePrepared(ctx: ContainerObjectContext<T>): Predicate<Field>? = nothing()
    protected open fun <T: Any> getFieldValueOnImagePrepared(ctx: ContainerObjectContext<T>, field: Field): Any? = null

    protected open fun <T: Any> getFieldSelectorOnContainerCreated(ctx: ContainerObjectContext<T>): Predicate<Field>? = nothing()
    protected open fun <T: Any> getFieldValueOnContainerCreated(ctx: ContainerObjectContext<T>, field: Field): Any? = null

    protected open fun <T: Any> getFieldSelectorOnContainerStarted(ctx: ContainerObjectContext<T>): Predicate<Field>? = nothing()
    protected open fun <T: Any> getFieldValueOnContainerStarted(ctx: ContainerObjectContext<T>, field: Field): Any? = null

    protected open fun <T: Any> getFieldSelectorOnContainerStopped(ctx: ContainerObjectContext<T>): Predicate<Field>? = nothing()
    protected open fun <T: Any> getFieldValueOnContainerStopped(ctx: ContainerObjectContext<T>, field: Field): Any? = null

    protected open fun <T: Any> getFieldSelectorOnContainerRemoved(ctx: ContainerObjectContext<T>): Predicate<Field>? = nothing()
    protected open fun <T: Any> getFieldValueOnContainerRemoved(ctx: ContainerObjectContext<T>, field: Field): Any? = null

    protected open fun <T: Any> getFieldSelectorOnImageReleased(ctx: ContainerObjectContext<T>): Predicate<Field>? = nothing()
    protected open fun <T: Any> getFieldValueOnImageReleased(ctx: ContainerObjectContext<T>, field: Field): Any? = null

    protected open fun <T: Any> getFieldSelectorOnInstanceDiscarded(ctx: ContainerObjectContext<T>): Predicate<Field>? = nothing()
    protected open fun <T: Any> getFieldValueOnInstanceDiscarded(ctx: ContainerObjectContext<T>, field: Field): Any? = null
}
