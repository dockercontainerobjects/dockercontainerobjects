package org.dockercontainerobjects

import java.net.InetAddress
import com.github.dockerjava.api.model.NetworkSettings

interface ContainerObjectsManager extends AutoCloseable {

    public static val IMAGE_TAG_DYNAMIC_PLACEHOLDER = "*"

    public static val SCHEME_CLASSPATH = "classpath"
    public static val SCHEME_FILE = "file"
    public static val SCHEME_HTTP = "http"
    public static val SCHEME_HTTPS = "https"

    public static val DOCKERFILE_DEFAULT_NAME = "Dockerfile"

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
