package org.dockercontainerobjects

interface ContainerObjectsClassEnhancer extends AutoCloseable {

    def void setupClass(Class<?> type)
    def void setupInstance(Object instance)

    def void teardownClass(Class<?> type)
    def void teardownInstance(Object instance)
}
