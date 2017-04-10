package org.dockercontainerobjects.util

import java.util.Optional

class Optionals {

    @Pure
    public static def <T> Optional<T> confirmed(T instance) {
        Optional.of(instance)
    }

    @Pure
    public static def <T> Optional<T> unsure(T instance) {
        Optional.ofNullable(instance)
    }

    @Pure
    public static def <T> T value(Optional<T> instance) {
        instance.orElse(null)
    }
}
