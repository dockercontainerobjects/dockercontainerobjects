package org.dockercontainerobjects

import org.dockercontainerobjects.util.NOTHING
import org.dockercontainerobjects.util.and
import org.dockercontainerobjects.util.annotatedWith
import org.dockercontainerobjects.util.debug
import org.dockercontainerobjects.util.onInstance
import org.dockercontainerobjects.util.updateFields
import org.slf4j.LoggerFactory
import java.lang.reflect.Field
import java.util.ServiceLoader
import javax.inject.Inject

object ExtensionManager {

    @JvmStatic private val l = LoggerFactory.getLogger(ExtensionManager::class.java)

    val extensions:List<ContainerObjectsExtension> = loadExtensions()

    internal fun setupEnvironment(env: ContainerObjectsEnvironment) {
        extensions.forEach { it.setupEnvironment(env) }
    }

    internal fun teardownEnvironment(env: ContainerObjectsEnvironment) {
        extensions.forEach { it.teardownEnvironment(env) }
    }

    internal fun <T: Any> updateContainerObjectFields(ctx: ContainerObjectContext<T>) {
        val containerInstance = ctx.instance ?: throw IllegalArgumentException("Received content must contain a valid instance")
        extensions.forEach { e ->
            l.debug { "Requesting field filter on stage '${ctx.stage}' to extension '${e.javaClass.simpleName}'" }
            val extensionSelector = e.getFieldSelector(ctx)
            if (extensionSelector !== null && extensionSelector !== NOTHING) {
                val selector = onInstance<Field>() and annotatedWith<Field>(Inject::class) and extensionSelector
                containerInstance.updateFields(selector) { field ->
                    l.debug { "Injecting value on stage '${ctx.stage}' from extension '${e.javaClass.simpleName}' to field '${field.name}'" }
                    e.getFieldValue(ctx, field)
                }
            }
        }
    }

    @JvmStatic private fun loadExtensions(): List<ContainerObjectsExtension> {
        val result = mutableListOf<ContainerObjectsExtension>()
        result.addAll(ServiceLoader.load(ContainerObjectsExtension::class.java))
        return result
    }
}
