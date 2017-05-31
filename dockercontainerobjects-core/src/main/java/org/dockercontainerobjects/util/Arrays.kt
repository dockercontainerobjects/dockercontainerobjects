package org.dockercontainerobjects.util

import java.util.stream.DoubleStream
import java.util.stream.IntStream
import java.util.stream.LongStream
import java.util.stream.Stream

object Arrays {
    inline fun <T: Any> Array<T>.stream(): Stream<T> = java.util.Arrays.stream(this)
    inline fun IntArray.stream(): IntStream = java.util.Arrays.stream(this)
    inline fun LongArray.stream(): LongStream = java.util.Arrays.stream(this)
    inline fun DoubleArray.stream(): DoubleStream = java.util.Arrays.stream(this)
}
