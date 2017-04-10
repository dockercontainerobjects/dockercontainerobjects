package org.dockercontainerobjects.util

import static extension org.dockercontainerobjects.util.Loggers.debug
import static extension org.dockercontainerobjects.util.Loggers.warn
import static extension org.dockercontainerobjects.util.Optionals.confirmed
import static extension org.dockercontainerobjects.util.Strings.operator_tripleLessThan

import java.lang.reflect.Method
import java.util.ArrayList
import java.util.Collection
import java.util.Optional
import java.util.function.Function
import java.util.function.Predicate
import java.util.stream.Collector
import org.slf4j.LoggerFactory
import java.lang.reflect.InvocationTargetException

class Methods extends Members {

    private static val l = LoggerFactory.getLogger(Fields)

    @Pure
    public static def Collection<Method> findMethods(Class<?> type, Predicate<Method> methodSelector) {
        val methods = new ArrayList<Method>
        for (var iter = type; iter !== null; iter = iter.superclass)
            iter.declaredMethods.stream.filter(methodSelector).forEach [ methods.add(it) ]
        methods
    }

    @Pure
    public static def Collection<Method> findAllMethods(Class<?> type) {
        val methods = new ArrayList<Method>
        for (var iter = type; iter !== null; iter = iter.superclass)
            iter.declaredMethods.stream.forEach [ methods.add(it) ]
        methods
    }

    @Pure
    public static def isOfReturnType(Method method, Class<?> type) {
        type.isAssignableFrom(method.returnType)
    }

    @Pure
    public static def Predicate<Method> ofReturnType(Class<?> type) {
        [ isOfReturnType(type) ]
    }

    @Pure
    public static def isOfVoidReturnType(Method method) {
        method.isOfReturnType(Void.TYPE)
    }

    @Pure
    public static def Predicate<Method> ofVoidReturnType(Class<?> type) {
        [ isOfVoidReturnType ]
    }

    @Pure
    public static def isExpectingParameterCount(Method m, int parameterCount) {
        m.parameterCount == parameterCount
    }

    @Pure
    public static def Predicate<Method> expectingParameterCount(int parameterCount) {
        [ isExpectingParameterCount(parameterCount) ]
    }

    @Pure
    public static def isExpectingNoParameters(Method m) {
        m.parameterCount == 0
    }

    @Pure
    public static def Predicate<Method> expectingNoParameters() {
        [ isExpectingNoParameters ]
    }

    public static def Object call(Method method, Object instance, Object... parameters) {
        try {
            method.reachable.invoke(instance, parameters)
        } catch (IllegalAccessException e) {
            l.warn(e)
            throw new IllegalArgumentException("Cannot access method '%s' due to: %s" <<< #[method, e.localizedMessage], e)
        } catch (InvocationTargetException e) {
            l.warn(e)
            throw new IllegalStateException("Exception invoking method '%s': %s" <<< #[method, e.localizedMessage], e)
        }
    }

    public static def <C, T, A, R> R invokeMethods(Class<C> type, Optional<C> instance,
            Predicate<Method> methodSelector, Optional<Function<Method, Object[]>> parameterSupplier,
            Optional<Collector<T, A, R>> collector) {
        val accum = collector.map[ supplier.get ].orElse(null)
        type.findMethods(methodSelector).stream.forEach [ method|
            val parameters = parameterSupplier.map[ apply(method) ].orElse(null)
            l.debug [ "Invoking method '%s'" <<< method ]
            val result = method.call(instance.get, parameters) as T
            collector.ifPresent[ accumulator.accept(accum, result) ]
        ]
        collector.map[ finisher.apply(accum) ].orElse(null)
    }

    public static def <C> void invokeMethods(Class<C> type, Optional<C> instance,
    Predicate<Method> methodSelector, Optional<Function<Method, Object[]>> parameterSupplier) {
        invokeMethods(type, instance, methodSelector, parameterSupplier, Optional.empty);
    }

    public static def <C> void invokeInstanceMethods(C instance, Predicate<Method> methodSelector,
            Optional<Function<Method, Object[]>> parameterSupplier) {
        invokeMethods(instance.class as Class<C>, instance.confirmed, methodSelector, parameterSupplier, Optional.empty);
    }

    public static def <C, T, A, R> R invokeInstanceMethods(C instance, Predicate<Method> methodSelector,
            Optional<Function<Method, Object[]>> parameterSupplier, Optional<Collector<T, A, R>> collector) {
        invokeMethods(instance.class as Class<C>, instance.confirmed, methodSelector, parameterSupplier, collector);
    }

    public static def <C> void invokeClassMethods(Class<C> type, Predicate<Method> methodSelector,
            Optional<Function<Method, Object[]>> parameterSupplier) {
        invokeMethods(type, Optional.empty, methodSelector, parameterSupplier, Optional.empty);
    }

    public static def <C, T, A, R> R invokeClassMethods(Class<C> type, Predicate<Method> methodSelector,
            Optional<Function<Method, Object[]>> parameterSupplier, Optional<Collector<T, A, R>> collector) {
        invokeMethods(type, Optional.empty, methodSelector, parameterSupplier, collector);
    }
}
