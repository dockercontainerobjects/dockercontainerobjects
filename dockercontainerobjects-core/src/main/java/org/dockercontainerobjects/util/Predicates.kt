@file:JvmName("Predicates")
@file:Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
package org.dockercontainerobjects.util

import java.util.function.Predicate

@JvmField val NOTHING: Predicate<*> = Predicate<Any> { false }
@JvmField val EVERYTHING: Predicate<*> = Predicate<Any> { true }

inline fun <reified T: Any> nothing() = NOTHING as Predicate<T>
inline fun <reified T: Any> everything() = EVERYTHING as Predicate<T>

inline infix fun <T> Predicate<T>.and(other: Predicate<T>): Predicate<T> = this.and(other)
inline infix fun <T> Predicate<T>.or(other: Predicate<T>): Predicate<T> = this.or(other)
inline operator fun <T> Predicate<T>.not(): Predicate<T> = this.negate()
