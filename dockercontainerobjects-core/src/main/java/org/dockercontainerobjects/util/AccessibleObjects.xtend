package org.dockercontainerobjects.util

import static extension org.dockercontainerobjects.util.Loggers.warn
import static extension org.dockercontainerobjects.util.Strings.operator_tripleLessThan

import java.lang.^annotation.Annotation
import java.lang.reflect.AccessibleObject
import java.util.function.Predicate
import java.security.AccessController
import java.security.PrivilegedAction
import org.slf4j.LoggerFactory

class AccessibleObjects {

    private static val l = LoggerFactory.getLogger(Fields)

    public static def <T extends AccessibleObject> isAnnotatedWith(T o, Class<? extends Annotation>... annotations) {
        !annotations.stream.filter[ !o.isAnnotationPresent(it) ].findAny.present
    }

    public static def <T extends AccessibleObject> Predicate<T> annotatedWith(Class<? extends Annotation>... annotations) {
        [ isAnnotatedWith(annotations) ]
    }

    public static def <T extends AccessibleObject> T reachable(T o) {
        if (!o.accessible) {
            val PrivilegedAction<Void> action = [
                o.accessible = true
                null as Void
            ]
            AccessController.doPrivileged(action)
        }
        o
    }

    public static def <T> T instantiate(Class<T> type) {
        try {
            type.declaredConstructor.reachable.newInstance
        } catch (Exception e) {
            l.warn(e)
            throw new IllegalArgumentException(
                "Cannot create new instance of '%s' due to: %s" <<< #[type.simpleName, e.localizedMessage], e)
        }
    }
}
