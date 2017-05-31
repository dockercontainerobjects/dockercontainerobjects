package org.dockercontainerobjects.util

import org.dockercontainerobjects.util.AccessibleObjects.reachable
import org.dockercontainerobjects.util.Arrays.stream
import org.dockercontainerobjects.util.Loggers.debug
import org.dockercontainerobjects.util.Loggers.loggerFor
import org.dockercontainerobjects.util.Loggers.warn
import org.dockercontainerobjects.util.Members.isReadOnly
import java.lang.reflect.Field
import java.util.Arrays
import java.util.function.Predicate
import kotlin.reflect.KClass

object Fields {

    private val l = loggerFor<Fields>()

    fun <T: Any> Class<T>.findFields(fieldSelector: Predicate<Field>): Collection<Field> {
        val fields = mutableListOf<Field>()
        var iter: Class<*> = this
        while (iter != Any::class.java) {
            iter.declaredFields.stream().filter(fieldSelector).forEach { fields += it }
            iter = iter.superclass
        }
        return fields
    }

    fun <T: Any> Class<T>.findAllFields(): Collection<Field> {
        val fields = mutableListOf<Field>()
        var iter: Class<*> = this
        while (iter != Any::class.java) {
            iter.declaredFields.stream().forEach { fields += it }
            iter = iter.superclass
        }
        return fields
    }

    inline fun Field.isOfType(type: Class<*>) = type.isAssignableFrom(this.type)
    inline fun <reified T: Any> Field.isOfType() = isOfType(T::class.java)

    inline fun ofType(type: Class<*>) = Predicate<Field> { it.isOfType(type) }
    inline fun <reified T: Any> ofType() = ofType(T::class.java)

    inline fun Field.isOfOneType(vararg types: Class<*>) =
            Arrays.stream(types).filter { it.isAssignableFrom(this.type) }.findAny().isPresent
    inline fun Field.isOfOneType(vararg types: KClass<*>) =
            Arrays.stream(types).filter { it.java.isAssignableFrom(this.type) }.findAny().isPresent

    inline fun ofOneType(vararg types: Class<*>) = Predicate<Field> { it.isOfOneType(*types) }
    inline fun ofOneType(vararg types: KClass<*>) = Predicate<Field> { it.isOfOneType(*types) }

    fun Field.read(instance: Any?): Any? {
        try {
            return reachable().get(instance)
        } catch (e:IllegalAccessException) {
            l.warn(e)
            throw IllegalArgumentException("Cannot access field '$this' due to: ${e.localizedMessage}", e)
        }
    }

    fun Field.update(instance: Any?, value: Any?) {
        try {
            reachable().set(instance, value)
            l.debug { "Field '$this' updated" }
        } catch (e: IllegalAccessException) {
            l.warn(e)
            throw IllegalArgumentException("Cannot access field '$this' due to: ${e.localizedMessage}", e)
        }
    }

    fun <T:Any> Class<T>.updateFields(instance:T?, fieldSelector:Predicate<Field>, valueSupplier:(Field) -> Any?) {
        findFields(fieldSelector).stream().forEach { field ->
            l.debug { "Preparing to update field '$field'" }
            if (field.isReadOnly)
                throw IllegalArgumentException("Cannot modify final field '${field.name}'")
            field.update(instance, valueSupplier.invoke(field))
        }
    }

    fun <T:Any> T.updateFields(fieldSelector:Predicate<Field>, valueSupplier:(Field) -> Any?) {
        this.javaClass.updateFields(this, fieldSelector, valueSupplier)
    }
}
