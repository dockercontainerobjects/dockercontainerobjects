package org.dockercontainerobjects.util

import java.util.function.Predicate

class Predicates {

    public static val Predicate<?> NOTHING = [ false ]
    public static val Predicate<?> EVERYTHING = [ false ]

    @Pure
    public static def <T> Predicate<T> &&(Predicate<T> it, Predicate<? super T> other) {
        and(other)
    }

    @Pure
    public static def <T> Predicate<T> ||(Predicate<T> it, Predicate<? super T> other) {
        or(other)
    }

    @Pure
    public static def <T> Predicate<T> !(Predicate<T> it) {
        negate
    }

    @Pure
    public static def <T> Predicate<T> nothing() {
        NOTHING as Predicate<T>
    }

    @Pure
    public static def <T> Predicate<T> everything() {
        EVERYTHING as Predicate<T>
    }
}
