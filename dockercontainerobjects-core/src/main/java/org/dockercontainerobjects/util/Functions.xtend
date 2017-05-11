package org.dockercontainerobjects.util

import java.util.function.Function

class Functions {

    @Pure
    public static def <R, T extends R> Function<T, R> identity() {
        [T value| value ]
    }

    @Pure
    public static def <R, T> Function<T, R> constant(R value) {
        [ value ]
    }

    @Pure
    public static def <R, T> Function<T, R> nil() {
        constant(null)
    }
}
