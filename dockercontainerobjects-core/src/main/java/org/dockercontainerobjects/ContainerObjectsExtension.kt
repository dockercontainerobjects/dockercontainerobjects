package org.dockercontainerobjects

import java.lang.reflect.Field
import java.util.function.Predicate

interface ContainerObjectsExtension {

    fun setupEnvironment(env: ContainerObjectsEnvironment) {}
    fun teardownEnvironment(env: ContainerObjectsEnvironment) {}

    fun <T: Any> getFieldSelector(ctx: ContainerObjectContext<T>): Predicate<Field>?
    fun <T: Any> getFieldValue(ctx: ContainerObjectContext<T>, field: Field): Any?
}
