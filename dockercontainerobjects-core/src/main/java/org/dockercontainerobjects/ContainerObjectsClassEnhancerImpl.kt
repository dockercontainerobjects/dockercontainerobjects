package org.dockercontainerobjects

import org.dockercontainerobjects.annotations.ContainerObject
import org.dockercontainerobjects.util.AccessibleObjects.annotatedWith
import org.dockercontainerobjects.util.Fields.findFields
import org.dockercontainerobjects.util.Fields.read
import org.dockercontainerobjects.util.Fields.update
import org.dockercontainerobjects.util.Loggers.debug
import org.dockercontainerobjects.util.Loggers.loggerFor
import org.dockercontainerobjects.util.Members.isReadOnly
import org.dockercontainerobjects.util.Members.onClass
import org.dockercontainerobjects.util.Members.onInstance
import org.dockercontainerobjects.util.Optionals.confirmed
import org.dockercontainerobjects.util.Optionals.value
import org.dockercontainerobjects.util.Predicates.and
import java.io.IOException
import java.lang.reflect.Field
import java.util.Optional
import java.util.Optional.empty
import java.util.function.Predicate
import kotlin.reflect.KClass

class ContainerObjectsClassEnhancerImpl(private val env: ContainerObjectsEnvironment): ContainerObjectsClassEnhancer {

    companion object {
        private val l = loggerFor<ContainerObjectsClassEnhancerImpl>()
    }

    @Throws(IOException::class)
    override fun close() {
        env.close()
    }

    override fun setupClass(type: Class<*>) {
        l.debug { "Setting up class containers in class '${type.simpleName}'" }
        type.setupContainerFields(empty(), onClass<Field>() and annotatedWith<Field>(ContainerObject::class))
    }

    override fun setupInstance(instance: Any) {
        l.debug { "Setting up instance containers in class '${instance.javaClass.simpleName}'" }
        instance.javaClass.setupContainerFields(instance.confirmed(), onInstance<Field>() and annotatedWith<Field>(ContainerObject::class))
    }

    override fun teardownClass(type: Class<*>) {
        l.debug { "Tearing down class containers in class '${type.simpleName}'" }
        type.teardownContainerFields(empty(), onClass<Field>() and annotatedWith<Field>(ContainerObject::class))
    }

    override fun teardownInstance(instance: Any) {
        l.debug { "Tearing down instance containers in class '${instance.javaClass.simpleName}'" }
        instance.javaClass.teardownContainerFields(instance.confirmed(), onInstance<Field>() and annotatedWith<Field>(ContainerObject::class))
    }

    private fun <T: Any> Class<T>.setupContainerFields(instance: Optional<T>, containerFieldSelector: Predicate<Field>) {
        findFields(containerFieldSelector).stream().forEach { setupContainerField(instance, it) }
    }

    private inline fun <T: Any> KClass<T>.setupContainerFields(instance: Optional<T>, containerFieldSelector: Predicate<Field>) =
        java.setupContainerFields(instance, containerFieldSelector)

    private fun <T: Any> Class<T>.teardownContainerFields(instance: Optional<T>, containerFieldSelector: Predicate<Field>) {
        findFields(containerFieldSelector).stream().forEach { teardownContainerField(instance, it) }
    }

    private fun <T: Any> KClass<T>.teardownContainerFields(instance: Optional<T>, containerFieldSelector: Predicate<Field>) =
        java.teardownContainerFields(instance, containerFieldSelector)

    private fun setupContainerField(instance: Optional<*>, field: Field) {
        if (field.isReadOnly)
            throw IllegalArgumentException(
                    "Cannot inject container in final field '${field.name}'")
        val containerInstance = env.manager.create(field.type)
        field.update(instance.value(), containerInstance)
    }

    private fun teardownContainerField(instance: Optional<*>, field: Field) {
        val containerInstance = field.read(instance.value()) ?: throw IllegalStateException()
        env.manager.destroy(containerInstance)
        field.update(instance.value(), null)
    }
}
