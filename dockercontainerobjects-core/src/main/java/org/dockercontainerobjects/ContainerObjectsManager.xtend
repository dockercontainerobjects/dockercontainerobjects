package org.dockercontainerobjects

import java.net.InetAddress
import com.github.dockerjava.api.model.NetworkSettings

interface ContainerObjectsManager extends AutoCloseable {

    static enum ContainerStatus {
        CREATED, STARTED, STOPPED, UNKNOWN
    }

    def <T> T create(Class<T> containerType)
    def <T> void destroy(T containerInstance)

    def String getContainerId(Object containerInstance)
    def ContainerStatus getContainerStatus(Object containerInstance)
    def boolean isContainerRunning(Object containerInstance) {
        getContainerStatus(containerInstance) == ContainerStatus.STARTED
    }
    def NetworkSettings getContainerNetworkSettings(Object containerInstance)
    def <ADDR extends InetAddress> ADDR getContainerAddress(Object containerInstance, Class<ADDR> addrType)
    def InetAddress getContainerAddress(Object containerInstance) {
        getContainerAddress(containerInstance, InetAddress)
    }
}
