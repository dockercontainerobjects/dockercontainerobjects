package org.dockercontainerobjects.util

import java.util.function.Function

@Suppress("NOTHING_TO_INLINE")
object Functions {

    inline fun <R, T: R> identity() = Function<T, R> { value: T -> value }

    inline fun <R, T> constant(value: R) = Function<T, R> { _ -> value }

    inline fun <R, T> nil() = Function<T, R?> { _ -> null }
}
