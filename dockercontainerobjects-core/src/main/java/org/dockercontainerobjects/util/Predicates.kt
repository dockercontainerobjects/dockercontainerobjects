package org.dockercontainerobjects.util

import java.util.function.Predicate

object Predicates {

    val NOTHING: Predicate<*> = Predicate<Any> { false }
    val EVERYTHING: Predicate<*> = Predicate<Any> { true }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> nothing(): Predicate<T> = NOTHING as Predicate<T>

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> everything(): Predicate<T> = EVERYTHING as Predicate<T>

    infix fun <T> Predicate<T>.and(other: Predicate<T>): Predicate<T> = this.and(other)
    infix fun <T> Predicate<T>.or(other: Predicate<T>): Predicate<T> = this.or(other)
    operator fun <T> Predicate<T>.not(): Predicate<T> = this.negate()
}
