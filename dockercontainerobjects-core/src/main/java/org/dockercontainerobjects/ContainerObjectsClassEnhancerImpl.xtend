package org.dockercontainerobjects

import static extension java.util.Optional.empty
import static extension org.dockercontainerobjects.util.AccessibleObjects.annotatedWith
import static extension org.dockercontainerobjects.util.Loggers.debug
import static extension org.dockercontainerobjects.util.Members.isReadOnly
import static extension org.dockercontainerobjects.util.Members.onClass
import static extension org.dockercontainerobjects.util.Members.onInstance
import static extension org.dockercontainerobjects.util.Fields.findFields
import static extension org.dockercontainerobjects.util.Fields.read
import static extension org.dockercontainerobjects.util.Fields.update
import static extension org.dockercontainerobjects.util.Optionals.confirmed
import static extension org.dockercontainerobjects.util.Optionals.value
import static extension org.dockercontainerobjects.util.Predicates.operator_and
import static extension org.dockercontainerobjects.util.Strings.operator_tripleLessThan

import java.lang.reflect.Field
import java.io.IOException
import java.util.Optional
import java.util.function.Predicate
import org.dockercontainerobjects.annotations.ContainerObject
import org.slf4j.LoggerFactory

class ContainerObjectsClassEnhancerImpl implements ContainerObjectsClassEnhancer {

    private static val l = LoggerFactory.getLogger(ContainerObjectsClassEnhancerImpl)

    private extension val ContainerObjectsEnvironment env

    new(ContainerObjectsEnvironment env) {
        this.env = env
    }

    override close() throws IOException {
        env.close
    }

    override setupClass(Class<?> type) {
        l.debug [ "Setting up class containers in class '%s'" <<< type.simpleName ]
        (type as Class<Object>).setupContainerFields(empty, onClass && annotatedWith(ContainerObject))
    }

    override setupInstance(Object instance) {
        l.debug [ "Setting up instance containers in class '%s'" <<< instance.class.simpleName ]
        (instance.class as Class<Object>).setupContainerFields(instance.confirmed, onInstance && annotatedWith(ContainerObject))
    }

    override teardownClass(Class<?> type) {
        l.debug [ "Tearing down class containers in class '%s'" <<< type.simpleName ]
        (type as Class<Object>).teardownContainerFields(empty, onClass && annotatedWith(ContainerObject))
    }

    override teardownInstance(Object instance) {
        l.debug [ "Tearing down instance containers in class '%s'" <<< instance.class.simpleName ]
        (instance.class as Class<Object>).teardownContainerFields(instance.confirmed, onInstance && annotatedWith(ContainerObject))
    }

    private def <T> void setupContainerFields(Class<T> type, Optional<T> instance,
            Predicate<Field> containerFieldSelector) {
        type.findFields(containerFieldSelector).stream.forEach[ setupContainerField(instance, it) ]
    }

    private def <T> void teardownContainerFields(Class<T> type, Optional<T> instance,
            Predicate<Field> containerFieldSelector) {
        type.findFields(containerFieldSelector).stream.forEach[ teardownContainerField(instance, it) ]
    }

    private def setupContainerField(Optional<?> instance, Field field) {
        if (field.readOnly)
            throw new IllegalArgumentException(
                    "Cannot inject container in final field '%s'" <<< field.name)
        val containerInstance = manager.create(field.type)
        field.update(instance.value, containerInstance)
    }

    private def teardownContainerField(Optional<?> instance, Field field) {
        val containerInstance = field.read(instance.value)
        manager.destroy(containerInstance)
        field.update(instance.value, null)
    }
}
