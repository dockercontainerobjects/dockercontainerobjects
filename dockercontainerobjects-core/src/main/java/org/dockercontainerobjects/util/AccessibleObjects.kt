package org.dockercontainerobjects.util

import org.dockercontainerobjects.util.Loggers.loggerFor
import org.dockercontainerobjects.util.Loggers.warn
import java.lang.reflect.AccessibleObject
import java.security.AccessController
import java.security.PrivilegedAction
import java.util.Arrays
import java.util.function.Predicate
import kotlin.reflect.KClass

object AccessibleObjects {

    private val l = loggerFor<AccessibleObjects>()

    inline fun AccessibleObject.isAnnotatedWith(type: Class<out Annotation>) = isAnnotationPresent(type)
    inline fun AccessibleObject.isAnnotatedWith(type: KClass<out Annotation>) = isAnnotationPresent(type.java)
    inline fun <reified T: Annotation> AccessibleObject.isAnnotatedWith() = isAnnotatedWith(T::class)

    inline fun <T: AccessibleObject> annotatedWith(type: Class<out Annotation>) =
            Predicate<T> { it.isAnnotatedWith(type) }
    inline fun <T: AccessibleObject> annotatedWith(type: KClass<out Annotation>) =
            Predicate<T> { it.isAnnotatedWith(type.java) }

    inline fun AccessibleObject.isAnnotatedWithAll(vararg annotations: Class<out Annotation>) =
        !Arrays.stream(annotations).filter { !isAnnotationPresent(it) }.findAny().isPresent
    inline fun AccessibleObject.isAnnotatedWithAll(vararg annotations: KClass<out Annotation>) =
            !Arrays.stream(annotations).filter { !isAnnotationPresent(it.java) }.findAny().isPresent

    inline fun annotatedWithAll(vararg annotations: Class<out Annotation>) =
            Predicate<AccessibleObject> { it.isAnnotatedWithAll(*annotations) }
    inline fun annotatedWithAll(vararg annotations: KClass<out Annotation>) =
            Predicate<AccessibleObject> { it.isAnnotatedWithAll(*annotations) }

    fun <T: AccessibleObject> T.reachable(): T {
        if (!isAccessible)
            AccessController.doPrivileged(PrivilegedAction<Unit> { isAccessible = true })
        return this
    }

    fun <T: Any> Class<T>.instantiate(): T {
        try {
            return getDeclaredConstructor().reachable().newInstance()
        } catch (e: Exception) {
            l.warn(e)
            throw IllegalArgumentException(
                "Cannot create new instance of '$simpleName' due to: ${e.localizedMessage}", e)
        }
    }
    inline fun <T: Any> KClass<T>.instantiate() = java.instantiate()
}
