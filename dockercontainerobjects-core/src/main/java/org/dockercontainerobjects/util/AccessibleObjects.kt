@file:JvmName("AccessibleObjects")
@file:Suppress("NOTHING_TO_INLINE")

package org.dockercontainerobjects.util

import java.lang.reflect.AccessibleObject
import java.security.AccessController
import java.security.PrivilegedAction
import kotlin.reflect.KClass

private val l = loggerFor("org.dockercontainerobjects.util.AccessibleObjects")

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
