package org.dockercontainerobjects.util

import static extension org.dockercontainerobjects.util.Loggers.debug
import static extension org.dockercontainerobjects.util.Loggers.warn
import static extension org.dockercontainerobjects.util.Optionals.confirmed
import static extension org.dockercontainerobjects.util.Optionals.value
import static extension org.dockercontainerobjects.util.Strings.operator_tripleLessThan

import java.lang.reflect.Field
import java.util.ArrayList
import java.util.Collection
import java.util.Optional
import java.util.function.Function
import java.util.function.Predicate
import org.slf4j.LoggerFactory

class Fields extends Members {

    private static val l = LoggerFactory.getLogger(Fields)

    @Pure
    public static def Collection<Field> findFields(Class<?> type, Predicate<Field> fieldSelector) {
        val fields = new ArrayList<Field>
        for (var iter = type; iter !== null; iter = iter.superclass)
            iter.declaredFields.stream.filter(fieldSelector).forEach [ fields.add(it) ]
        fields
    }

    @Pure
    public static def Collection<Field> findAllFields(Class<?> type) {
        val fields = new ArrayList<Field>
        for (var iter = type; iter !== null; iter = iter.superclass)
            iter.declaredFields.stream.forEach [ fields.add(it) ]
        fields
    }

    @Pure
    public static def isOfType(Field f, Class<?> type) {
        type.isAssignableFrom(f.type)
    }

    @Pure
    public static def Predicate<Field> ofType(Class<?> type) {
        [ isOfType(type) ]
    }

    public static def Object read(Field field, Object instance) {
        try {
            field.reachable.get(instance)
        } catch (IllegalAccessException e) {
            l.warn(e)
            throw new IllegalArgumentException("Cannot access field '%s' due to: %s" <<< #[field, e.localizedMessage], e)
        }
    }

    public static def void update(Field field, Object instance, Object value) {
        try {
            field.reachable.set(instance, value)
            l.debug [ "Field '%s' updated" <<< field ]
        } catch (IllegalAccessException e) {
            l.warn(e)
            throw new IllegalArgumentException("Cannot access field '%s' due to: %s" <<< #[field, e.localizedMessage], e)
        }
    }

    public static def <T, F> void updateFields(Class<T> type, Optional<T> instance, Predicate<Field> fieldSelector,
            Function<Class<?>, ?> valueSupplier) {
        type.findFields(fieldSelector).stream.forEach [ field|
            l.debug [ "Preparing to update field '%s'" <<< field ]
            if (field.readOnly)
                throw new IllegalArgumentException(
                        "Cannot modify final field '%s'" <<< field.name)
            field.update(instance.value, valueSupplier.apply(field.type))
        ]
    }

    public static def <T, F> void updateFields(T instance, Predicate<Field> fieldSelector,
            Function<Class<?>, ?> valueSupplier) {
        val type = instance.class as Class<T>
        type.updateFields(instance.confirmed, fieldSelector, valueSupplier);
    }
}
