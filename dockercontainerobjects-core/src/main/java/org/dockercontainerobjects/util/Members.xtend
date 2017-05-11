package org.dockercontainerobjects.util

import static java.lang.reflect.Modifier.isFinal
import static java.lang.reflect.Modifier.isStatic

import java.lang.reflect.Member
import java.util.function.Predicate

class Members extends AccessibleObjects {

    @Pure
    public static def <T extends Member> isOnClass(T it) {
        isStatic(modifiers)
    }

    @Pure
    public static def <T extends Member> Predicate<T> onClass() {
        [ isOnClass ]
    }

    @Pure
    public static def <T extends Member> isOnInstance(T it) {
        !isStatic(modifiers)
    }

    @Pure
    public static def <T extends Member> Predicate<T> onInstance() {
        [ isOnInstance ]
    }

    @Pure
    public static def <T extends Member> isReadOnly(T it) {
        isFinal(modifiers)
    }

    @Pure
    public static def <T extends Member> Predicate<T> readOnly() {
        [ isReadOnly ]
    }

    @Pure
    public static def <T extends Member> isReadWrite(T it) {
        !isFinal(modifiers)
    }

    @Pure
    public static def <T extends Member> Predicate<T> readWrite() {
        [ isReadWrite ]
    }
}
