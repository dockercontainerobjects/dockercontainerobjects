package org.dockercontainerobjects.util

import java.util.function.Function

class Functions {

    public static def <R, T extends R> Function<T, R> identity() {
        [T value| value ]
    }

    public static def <R, T> Function<T, R> constant(R value) {
        [ value ]
    }

    public static def <R, T> Function<T, R> nil() {
        constant(null)
    }
}
