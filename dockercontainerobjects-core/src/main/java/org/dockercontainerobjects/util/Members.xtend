package org.dockercontainerobjects.util

import static java.lang.reflect.Modifier.isFinal
import static java.lang.reflect.Modifier.isStatic

import java.lang.reflect.Member
import java.util.function.Predicate

class Members extends AccessibleObjects {

    public static def <T extends Member> isOnClass(T it) {
        isStatic(modifiers)
    }

    public static def <T extends Member> Predicate<T> onClass() {
        [ isOnClass ]
    }

    public static def <T extends Member> isOnInstance(T it) {
        !isStatic(modifiers)
    }

    public static def <T extends Member> Predicate<T> onInstance() {
        [ isOnInstance ]
    }

    public static def <T extends Member> isReadOnly(T it) {
        isFinal(modifiers)
    }

    public static def <T extends Member> Predicate<T> readOnly() {
        [ isReadOnly ]
    }

    public static def <T extends Member> isReadWrite(T it) {
        !isFinal(modifiers)
    }

    public static def <T extends Member> Predicate<T> readWrite() {
        [ isReadWrite ]
    }
}
