package org.dockercontainerobjects

import java.lang.reflect.Field
import java.util.function.Predicate

interface ContainerObjectsExtension {

    def void setupEnvironment(ContainerObjectsEnvironment env) {}
    def void teardownEnvironment(ContainerObjectsEnvironment env) {}

    def <T> Predicate<Field> getFieldSelector(ContainerObjectContext<T> ctx)
    def <T> Object getFieldValue(ContainerObjectContext<T> ctx, Field field)
}
