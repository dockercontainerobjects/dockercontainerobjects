@file:JvmName("Methods")
@file:Suppress("NOTHING_TO_INLINE")
package org.dockercontainerobjects.util

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.Arrays
import java.util.function.Predicate
import java.util.stream.Collector
import kotlin.reflect.KClass

private val l = loggerFor("org.dockercontainerobjects.util.Methods")

fun <T: Any> Class<T>.findMethods(methodSelector: Predicate<Method>): Collection<Method> {
    val methods = mutableListOf<Method>()
    var iter: Class<*> = this
    while (iter != Any::class.java) {
        Arrays.stream(iter.declaredMethods).filter(methodSelector).forEach { methods += it }
        iter = iter.superclass
    }
    return methods
}

fun <T: Any> Class<T>.findAllMethods(): Collection<Method> {
    val methods = mutableListOf<Method>()
    var iter: Class<*> = this
    while (iter != Any::class.java) {
        Arrays.stream(iter.declaredMethods).forEach { methods += it }
        iter = iter.superclass
    }
    return methods
}

inline fun Method.isOfReturnType(type: Class<*>) = type.isAssignableFrom(this.returnType)
inline fun Method.isOfReturnType(type: KClass<*>) = type.java.isAssignableFrom(this.returnType)
inline fun <reified T: Any> Method.isOfReturnType() = isOfReturnType(T::class.java)

inline fun ofReturnType(type: Class<*>) = Predicate<Method> { it.isOfReturnType(type) }
inline fun <reified T: Class<*>> ofReturnType() = ofReturnType(T::class.java)

inline fun Method.isOfVoidReturnType() = isOfReturnType(Void.TYPE)
inline fun ofVoidReturnType() = Predicate<Method> { it.isOfVoidReturnType() }

inline fun Method.isExpectingParameterCount(parameterCount: Int) = this.parameterCount == parameterCount
inline fun expectingParameterCount(parameterCount: Int) = Predicate<Method> { it.isExpectingParameterCount(parameterCount) }

inline fun Method.isExpectingNoParameters() = this.parameterCount == 0
inline fun expectingNoParameters() = Predicate<Method> { it.isExpectingNoParameters() }

fun Method.call(instance: Any?, vararg parameters: Any): Any? {
    try {
        return reachable().invoke(instance, *parameters)
    } catch (e: IllegalAccessException) {
        l.warn(e)
        throw IllegalArgumentException("Cannot access method '$this' due to: ${e.localizedMessage}", e)
    } catch (e: InvocationTargetException) {
        l.warn(e)
        throw IllegalStateException("Exception invoking method '$this' due to: ${e.localizedMessage}", e)
    }
}

fun <C: Any, T: Any, A: Any, R: Any> Class<C>.invokeMethods(
        instance: C?,
        methodSelector: Predicate<Method>,
        parameterSupplier: ((Method) -> Array<Any>)? = null,
        collector: Collector<T, A, R>? = null): R? {
    val accum = collector?.supplier()?.get()
    findMethods(methodSelector).stream().forEach { method ->
        val parameters = parameterSupplier?.invoke(method) ?: emptyArray()
        l.debug { "Invoking method '$method'" }
        @Suppress("UNCHECKED_CAST")
        val result = method.call(instance, *parameters) as T?
        collector?.accumulator()?.accept(accum, result)
    }
    return collector?.finisher()?.apply(accum)
}

inline fun <C: Any, T: Any, A: Any, R: Any> C.invokeInstanceMethods(
        methodSelector: Predicate<Method>,
        noinline parameterSupplier: ((Method) -> Array<Any>)? = null,
        collector: Collector<T, A, R>? = null) =
    javaClass.invokeMethods(this, methodSelector, parameterSupplier, collector)

inline fun <C: Any, T: Any, A: Any, R: Any> Class<C>.invokeClassMethods(
        methodSelector: Predicate<Method>,
        noinline parameterSupplier: ((Method) -> Array<Any>)? = null,
        collector: Collector<T, A, R>? = null) =
    invokeMethods(null, methodSelector, parameterSupplier, collector)
