package org.dockercontainerobjects.util

import org.dockercontainerobjects.util.AccessibleObjects.reachable
import org.dockercontainerobjects.util.Loggers.debug
import org.dockercontainerobjects.util.Loggers.loggerFor
import org.dockercontainerobjects.util.Loggers.warn
import org.dockercontainerobjects.util.Optionals.confirmed
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.Arrays
import java.util.Optional
import java.util.function.Predicate
import java.util.stream.Collector
import kotlin.reflect.KClass

@Suppress("NOTHING_TO_INLINE")
object Methods {

    private val l = loggerFor<Methods>()

    fun Class<*>.findMethods(methodSelector: Predicate<Method>): Collection<Method> {
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
            instance: Optional<C>,
            methodSelector: Predicate<Method>,
            parameterSupplier: Optional<(Method) -> Array<Any>> = Optional.empty(),
            collector: Optional<Collector<T, A, R>> = Optional.empty()): R? {
        val accum = collector.map { it.supplier().get() }.orElse(null)
        findMethods(methodSelector).stream().forEach { method ->
            val parameters = parameterSupplier.map { it.invoke(method) }.orElse(emptyArray())
            l.debug { "Invoking method '$method'" }
            @Suppress("UNCHECKED_CAST")
            val result = method.call(instance.get(), *parameters) as T?
            collector.ifPresent { it.accumulator().accept(accum, result) }
        }
        return collector.map { it.finisher().apply(accum) }.orElse(null)
    }

    inline fun <C: Any, T: Any, A: Any, R: Any> C.invokeInstanceMethods(
            methodSelector: Predicate<Method>,
            parameterSupplier: Optional<(Method) -> Array<Any>> = Optional.empty(),
            collector: Optional<Collector<T, A, R>> = Optional.empty()) =
        javaClass.invokeMethods(this.confirmed(), methodSelector, parameterSupplier, collector)

    inline fun <C: Any, T: Any, A: Any, R: Any> Class<C>.invokeClassMethods(
            methodSelector: Predicate<Method>,
            parameterSupplier: Optional<(Method) -> Array<Any>> = Optional.empty(),
            collector: Optional<Collector<T, A, R>> = Optional.empty()) =
        invokeMethods(Optional.empty(), methodSelector, parameterSupplier, collector)
}
