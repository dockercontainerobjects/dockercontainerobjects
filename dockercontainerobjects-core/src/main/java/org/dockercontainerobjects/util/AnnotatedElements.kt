@file:JvmName("AnnotatedElements")
@file:Suppress("NOTHING_TO_INLINE")

package org.dockercontainerobjects.util

import java.lang.reflect.AnnotatedElement
import java.util.function.Predicate
import kotlin.reflect.KClass

inline fun AnnotatedElement.isAnnotatedWith(type: Class<out Annotation>) = isAnnotationPresent(type)
inline fun AnnotatedElement.isAnnotatedWith(type: KClass<out Annotation>) = isAnnotationPresent(type.java)
inline fun <reified T: Annotation> AnnotatedElement.isAnnotatedWith() = isAnnotatedWith(T::class)

inline fun <T: AnnotatedElement> annotatedWith(type: Class<out Annotation>) =
        Predicate<T> { it.isAnnotatedWith(type) }
inline fun <T: AnnotatedElement> annotatedWith(type: KClass<out Annotation>) =
        Predicate<T> { it.isAnnotatedWith(type.java) }

inline fun AnnotatedElement.isAnnotatedWithAll(vararg annotations: Class<out Annotation>) =
        !annotations.stream().filter { !isAnnotationPresent(it) }.findAny().isPresent
inline fun AnnotatedElement.isAnnotatedWithAll(vararg annotations: KClass<out Annotation>) =
        !annotations.stream().filter { !isAnnotationPresent(it.java) }.findAny().isPresent

inline fun annotatedWithAll(vararg annotations: Class<out Annotation>) =
        Predicate<AnnotatedElement> { it.isAnnotatedWithAll(*annotations) }
inline fun annotatedWithAll(vararg annotations: KClass<out Annotation>) =
        Predicate<AnnotatedElement> { it.isAnnotatedWithAll(*annotations) }

inline fun <T: Annotation> AnnotatedElement.getAnnotation(type: KClass<T>): T? = getAnnotation(type.java)
inline fun <reified T: Annotation> AnnotatedElement.getAnnotation() = getAnnotation(T::class)

inline fun <T: Annotation> AnnotatedElement.getAnnotationsByType(type: KClass<T>): Array<T> = getAnnotationsByType(type.java)
inline fun <reified T: Annotation> AnnotatedElement.getAnnotationsByType() = getAnnotationsByType(T::class)
