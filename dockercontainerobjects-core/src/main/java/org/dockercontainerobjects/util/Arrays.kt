@file:JvmName("Arrays")
@file:Suppress("NOTHING_TO_INLINE")
package org.dockercontainerobjects.util

import java.util.Arrays
import java.util.stream.DoubleStream
import java.util.stream.IntStream
import java.util.stream.LongStream
import java.util.stream.Stream

inline fun <T: Any> Array<T>.stream(): Stream<T> = Arrays.stream(this)
inline fun IntArray.stream(): IntStream = Arrays.stream(this)
inline fun LongArray.stream(): LongStream = Arrays.stream(this)
inline fun DoubleArray.stream(): DoubleStream = Arrays.stream(this)
