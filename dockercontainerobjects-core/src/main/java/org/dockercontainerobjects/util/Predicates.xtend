package org.dockercontainerobjects.util

import java.util.function.Predicate

class Predicates {

    public static def <T> Predicate<T> &&(Predicate<T> it, Predicate<? super T> other) {
        and(other)
    }

    public static def <T> Predicate<T> ||(Predicate<T> it, Predicate<? super T> other) {
        or(other)
    }

    public static def <T> Predicate<T> !(Predicate<T> it) {
        negate
    }
}
