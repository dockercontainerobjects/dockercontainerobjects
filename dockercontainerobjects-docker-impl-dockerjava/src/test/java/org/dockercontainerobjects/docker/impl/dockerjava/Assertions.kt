@file:Suppress("NOTHING_TO_INLINE")

package org.dockercontainerobjects.docker.impl.dockerjava

import org.junit.jupiter.api.Assertions
import kotlin.reflect.KClass

inline fun <T: Throwable> assertThrows(
        expectedExceptionType: KClass<T>, noinline executable: () -> Unit): T =
            Assertions.assertThrows(expectedExceptionType.java, executable)
inline fun <reified T: Throwable> assertThrows(noinline executable: () -> Unit) =
        assertThrows(T::class, executable)
